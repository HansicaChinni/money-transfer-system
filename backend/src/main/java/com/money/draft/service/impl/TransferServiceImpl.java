package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.*;
import com.money.draft.service.RewardService;
import com.money.draft.service.TransactionLogWriter;
import com.money.draft.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransferServiceImpl implements TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);
    private static final int MAX_RETRIES = 3;

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;
    private final TransactionLogWriter logWriter;
    private final RewardService rewardService;

    public TransferServiceImpl(AccountRepository accountRepo,
                               TransactionLogRepository txRepo,
                               TransactionLogWriter logWriter,
                               RewardService rewardService) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.logWriter = logWriter;
        this.rewardService = rewardService;
    }

    @Override
    public TransferResponse transfer(TransferRequest req) {
        if (req == null) throw new ValidationException("TransferRequest is required");

        TransferRequest normalized = (req.idempotencyKey() == null || req.idempotencyKey().isBlank())
                ? new TransferRequest(
                req.fromAccountId(),
                req.toAccountId(),
                req.amount(),
                "sys-%d-%d-%s".formatted(req.fromAccountId(), req.toAccountId(), UUID.randomUUID()))
                : req;

        txRepo.findByIdempotencyKey(normalized.idempotencyKey()).ifPresent(existing -> {
            throw new DuplicateTransferException(normalized.idempotencyKey());
        });

        if (normalized.isSelfTransfer()) {
            throw new SelfTransferNotAllowedException(normalized.fromAccountId());
        }

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return doTransferOnce(normalized);
            } catch (OptimisticLockingFailureException ex) {
                if (attempts >= MAX_RETRIES) throw ex;
                long backoff = 25L * attempts;
                log.warn("Optimistic lock conflict on transfer (attempt {}), retrying in {}ms", attempts, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (BusinessException ex) {
                logFailure(normalized, ex.getMessage());
                throw ex;
            } catch (RuntimeException ex) {
                logFailure(normalized, "Unexpected error: " + ex.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public TransferResponse transferForUser(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        TransferRequest req = new TransferRequest(
                fromAccountId,
                toAccountId,
                amount,
                "me-" + fromAccountId + "-" + UUID.randomUUID()
        );
        return transfer(req);
    }

    @Transactional
    protected TransferResponse doTransferOnce(TransferRequest req) {
        Account from = accountRepo.findById(req.fromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(req.fromAccountId()));
        Account to = accountRepo.findById(req.toAccountId())
                .orElseThrow(() -> new AccountNotFoundException(req.toAccountId()));

        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(from.getId(), from.getStatus().name());
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(to.getId(), to.getStatus().name());
        }

        BigDecimal amount = req.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than zero");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(from.getId(), from.getBalance(), amount);
        }

        from.recordTransfer(amount);

        from.debit(amount);
        to.credit(amount);

        accountRepo.save(from);
        accountRepo.save(to);

        TransactionLog tx = logWriter.logSuccess(from.getId(), to.getId(), amount, req.idempotencyKey());

        rewardService.grantRewardIfEligible(from.getId(), to.getId(), tx.getId(), amount);

        return TransferResponse.success(tx.getId(), amount);
    }

    private void logFailure(TransferRequest req, String reason) {
        try {
            logWriter.logFailure(req.fromAccountId(), req.toAccountId(), req.amount(), req.idempotencyKey(), reason);
        } catch (Exception ignored) {
        }
    }
}
