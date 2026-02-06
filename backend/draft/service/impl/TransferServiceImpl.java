
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.*;
import com.money.draft.service.TransactionLogWriter;
import com.money.draft.service.TransferService;
import jakarta.validation.ValidationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;
    private final TransactionLogWriter logWriter;

    // retry config
    private static final int MAX_RETRIES = 3;

    public TransferServiceImpl(AccountRepository accountRepo,
                               TransactionLogRepository txRepo,
                               TransactionLogWriter logWriter) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.logWriter = logWriter;
    }

    /**
     * Public transfer API used by controllers/services that already have a TransferRequest.
     * NOTE: We now defensively auto-generate an idempotencyKey if it's null/blank to improve DX.
     * If you want STRICT client-provided idempotency for public APIs, remove this auto-generation
     * and throw a validation error instead.
     */
    @Override
    public TransferResponse transfer(TransferRequest req) {
        // Defensive normalization
        validateRequired(req);

        // Auto-generate idempotency if missing/blank (DX-friendly; optional)
        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()) {
            String auto = "sys-" + req.getFromAccountId() + "-" + req.getToAccountId() + "-" + UUID.randomUUID();
            req.setIdempotencyKey(auto);
        }

        // 1) Basic idempotency check up-front
        txRepo.findByIdempotencyKey(req.getIdempotencyKey()).ifPresent(existing -> {
            throw new DuplicateTransferException(req.getIdempotencyKey());
        });

        // 2) Guard self-transfer
        if (Objects.equals(req.getFromAccountId(), req.getToAccountId())) {
            throw new SelfTransferNotAllowedException(req.getFromAccountId());
        }

        // 3) Optimistic-lock retry loop
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return doTransferOnce(req);
            } catch (OptimisticLockingFailureException ex) {
                if (attempts >= MAX_RETRIES) throw ex; // bubble up after max retries
                try {
                    Thread.sleep(25L * attempts); // small back-off
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } catch (BusinessException ex) {
                // Log failure outside main transaction and rethrow
                logFailure(req, ex.getMessage());
                throw ex;
            } catch (RuntimeException ex) {
                // Unknown error -> log failure and rethrow
                logFailure(req, "Unexpected error: " + ex.getMessage());
                throw ex;
            }
        }
    }

    /**
     * Server-side only entry point for user-initiated transfers (from /me/transfer).
     * - Forces fromAccountId from the server
     * - Auto-generates a fresh idempotencyKey per call
     * - Leaves all validations/business rules to the same pipeline
     */
    @Override
    public TransferResponse transferForUser(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId == null) throw new IllegalArgumentException("fromAccountId is required");
        if (toAccountId == null) throw new IllegalArgumentException("toAccountId is required");
        if (amount == null) throw new IllegalArgumentException("amount is required");

        TransferRequest req = new TransferRequest();
        req.setFromAccountId(fromAccountId);
        req.setToAccountId(toAccountId);
        req.setAmount(amount);

        // Always generate a brand-new idempotency key for user-triggered transfers
        req.setIdempotencyKey("me-" + fromAccountId + "-" + UUID.randomUUID());

        return transfer(req);
    }

    @Transactional
    protected TransferResponse doTransferOnce(TransferRequest req) {
        // Load accounts
        Account from = accountRepo.findById(req.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(req.getFromAccountId()));
        Account to = accountRepo.findById(req.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(req.getToAccountId()));

        // Status checks
        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(from.getId(), String.valueOf(from.getStatus()));
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(to.getId(), String.valueOf(to.getStatus()));
        }

        // Positive / sufficient amount
        BigDecimal amount = req.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(from.getId(), from.getBalance(), amount);
        }

        // Domain operations
        from.debit(amount);
        to.credit(amount);

        // Persist account changes (will trigger optimistic locking)
        accountRepo.save(from);
        accountRepo.save(to);

        // Log success in a new transaction
        TransactionLog tx = logWriter.logSuccess(from.getId(), to.getId(), amount, req.getIdempotencyKey());

        // Return response
        return TransferResponse.success(tx.getId(), amount);
    }

    private void logFailure(TransferRequest req, String reason) {
        Long fromId = req.getFromAccountId();
        Long toId = req.getToAccountId();
        BigDecimal amount = req.getAmount();
        String key = req.getIdempotencyKey();
        try {
            logWriter.logFailure(fromId, toId, amount, key, reason);
        } catch (Exception ignored) {
            // If logging fails, we still propagate the original exception
        }
    }

    private void validateRequired(TransferRequest req) {
        if (req == null) throw new IllegalArgumentException("TransferRequest is required");
        if (req.getFromAccountId() == null) throw new ValidationException("fromAccountId is required");
        if (req.getToAccountId() == null) throw new ValidationException("toAccountId is required");
        if (req.getAmount() == null) throw new ValidationException("amount is required");
    }
}
