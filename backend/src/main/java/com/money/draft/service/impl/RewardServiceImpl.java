
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.RewardTransaction;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.RewardTransactionType;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.RewardTransactionRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.RewardSummaryResponse;
import com.money.draft.dto.RewardTransactionResponse;
import com.money.draft.dto.TransactionDetailResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.service.RewardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class RewardServiceImpl implements RewardService {

    private final AccountRepository accountRepo;
    private final RewardTransactionRepository rewardRepo;
    private final TransactionLogRepository txRepo;

    public RewardServiceImpl(AccountRepository accountRepo,
                             RewardTransactionRepository rewardRepo,
                             TransactionLogRepository txRepo) {
        this.accountRepo = accountRepo;
        this.rewardRepo = rewardRepo;
        this.txRepo = txRepo;
    }

    @Override
    public int redeemDuringTransfer(Account from, BigDecimal amount) {
        int available = from.getRewardPoints();
        if (available <= 0) return 0;
        int amountInt = amount.setScale(0, RoundingMode.DOWN).intValue();
        int used = Math.min(available, amountInt);
        from.debitRewardPoints(used);
        return used;
    }

    @Override
    @Transactional
    public void grantPoints(Long accountId, Long transactionId, BigDecimal actualDebit) {
        if (actualDebit == null || actualDebit.compareTo(new BigDecimal("100")) <= 0) return;
        int points = actualDebit.divide(new BigDecimal("100"), RoundingMode.DOWN).intValue();
        if (points <= 0) return;

        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.creditRewardPoints(points);
        accountRepo.save(account);
        rewardRepo.save(RewardTransaction.earned(accountId, points, transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummary(Long accountId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        int earned = rewardRepo.sumPointsByAccountIdAndType(accountId, RewardTransactionType.EARNED);
        int redeemed = rewardRepo.sumPointsByAccountIdAndType(accountId, RewardTransactionType.REDEEMED);
        return new RewardSummaryResponse(account.getRewardPoints(), earned, redeemed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardTransactionResponse> getRewardTransactions(Long accountId) {
        return rewardRepo.findByAccountIdOrderByCreatedOnDesc(accountId).stream()
                .map(this::toRewardTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetail(Long accountId, Long transactionId) {
        TransactionLog tx = txRepo.findById(transactionId)
                .orElseThrow(() -> new AccountNotFoundException(transactionId));

        Optional<RewardTransaction> earned = rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(accountId, transactionId, RewardTransactionType.EARNED);
        Optional<RewardTransaction> redeemed = rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(accountId, transactionId, RewardTransactionType.REDEEMED);

        return new TransactionDetailResponse(
                tx.getId(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getFailureReason(),
                tx.getIdempotencyKey(),
                toLocalDateTime(tx.getCreatedOn()),
                earned.map(RewardTransaction::getPoints).orElse(null),
                redeemed.map(RewardTransaction::getPoints).orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardTransactionResponse> getAllRewardTransactions() {
        return rewardRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedOn().compareTo(a.getCreatedOn()))
                .map(this::toRewardTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummaryForAdmin(Long accountId) {
        return getRewardSummary(accountId);
    }

    private RewardTransactionResponse toRewardTransactionResponse(RewardTransaction r) {
        return new RewardTransactionResponse(
                r.getId(),
                r.getAccountId(),
                r.getType().name(),
                r.getPoints(),
                r.getReferenceTransactionId(),
                toLocalDateTime(r.getCreatedOn())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}
