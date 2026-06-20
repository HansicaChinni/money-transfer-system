
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.RewardTransaction;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.RewardTransactionType;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.RewardTransactionRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminRewardDashboardResponse;
import com.money.draft.dto.RewardSummaryResponse;
import com.money.draft.dto.RewardTransactionResponse;
import com.money.draft.dto.TransactionDetailResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.service.RewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RewardServiceImpl implements RewardService {

    private static final Logger log = LoggerFactory.getLogger(RewardServiceImpl.class);

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

    private int calculateExpiringPoints(Long accountId) {
        Instant now = Instant.now();
        Instant threshold = now.plus(2, ChronoUnit.DAYS);
        int raw = rewardRepo.sumPointsExpiringBetween(accountId, now, threshold);
        Account account = accountRepo.findById(accountId).orElse(null);
        if (account == null) return 0;
        return Math.min(raw, account.getRewardPoints());
    }

    @Override
    @Transactional
    public RewardSummaryResponse getRewardSummary(Long accountId) {
        processExpiredPoints(accountId);
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        int earned = rewardRepo.sumPointsByAccountIdAndType(accountId, RewardTransactionType.EARNED);
        int redeemed = rewardRepo.sumPointsByAccountIdAndType(accountId, RewardTransactionType.REDEEMED);
        int expiring = calculateExpiringPoints(accountId);
        return new RewardSummaryResponse(account.getRewardPoints(), earned, redeemed, expiring);
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

        java.util.Map<Long, Account> accountCache = new java.util.HashMap<>();
        accountRepo.findById(tx.getFromAccountId()).ifPresent(a -> accountCache.put(a.getId(), a));
        accountRepo.findById(tx.getToAccountId()).ifPresent(a -> accountCache.put(a.getId(), a));

        return new TransactionDetailResponse(
                tx.getId(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                resolveAccountNumber(tx.getFromAccountId(), accountCache),
                resolveAccountNumber(tx.getToAccountId(), accountCache),
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
        List<RewardTransaction> all = rewardRepo.findAll();
        all.sort((a, b) -> b.getCreatedOn().compareTo(a.getCreatedOn()));
        java.util.Set<Long> accountIds = new java.util.HashSet<>();
        for (RewardTransaction r : all) accountIds.add(r.getAccountId());
        java.util.Map<Long, Account> accountCache = accountRepo.findAllById(accountIds).stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, a -> a));
        return all.stream()
                .map(r -> toRewardTransactionResponse(r, accountCache))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummaryForAdmin(Long accountId) {
        return getRewardSummary(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRewardDashboardResponse getAdminRewardDashboard() {
        int totalEarned = rewardRepo.sumPointsByType(RewardTransactionType.EARNED);
        int totalRedeemed = rewardRepo.sumPointsByType(RewardTransactionType.REDEEMED);
        return new AdminRewardDashboardResponse(totalEarned, totalRedeemed);
    }

    private RewardTransactionResponse toRewardTransactionResponse(RewardTransaction r) {
        java.util.Map<Long, Account> cache = new java.util.HashMap<>();
        accountRepo.findById(r.getAccountId()).ifPresent(a -> cache.put(a.getId(), a));
        return toRewardTransactionResponse(r, cache);
    }

    private RewardTransactionResponse toRewardTransactionResponse(RewardTransaction r, java.util.Map<Long, Account> accountCache) {
        Account a = accountCache.get(r.getAccountId());
        return new RewardTransactionResponse(
                r.getId(),
                r.getAccountId(),
                a != null ? a.getAccountNumber() : null,
                a != null ? a.getHolderName() : null,
                r.getType().name(),
                r.getPoints(),
                r.getReferenceTransactionId(),
                toLocalDateTime(r.getCreatedOn()),
                toLocalDateTime(r.getExpiresOn())
        );
    }

    private static String resolveAccountNumber(Long accountId, java.util.Map<Long, Account> cache) {
        Account a = cache.get(accountId);
        return a != null ? a.getAccountNumber() : "#" + accountId;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    @Transactional
    public void processExpiredPoints(Long accountId) {
        List<RewardTransaction> earnedList = rewardRepo.findEarnedByAccountIdOrderByCreatedOnAsc(accountId);
        if (earnedList.isEmpty()) return;

        List<RewardTransaction> redeemedList = rewardRepo.findRedeemedByAccountIdOrderByCreatedOnAsc(accountId);

        int totalUnredeemedExpired = 0;
        int redeemedSoFar = 0;

        for (RewardTransaction earned : earnedList) {
            int earnedPoints = earned.getPoints();
            int consumed = 0;

            while (consumed < earnedPoints && redeemedSoFar < redeemedList.size()) {
                RewardTransaction redeemed = redeemedList.get(redeemedSoFar);
                int availableInThisRedeemed = redeemed.getPoints();
                int canConsume = Math.min(earnedPoints - consumed, availableInThisRedeemed);

                consumed += canConsume;
                if (canConsume >= availableInThisRedeemed) {
                    redeemedSoFar++;
                } else {
                    redeemedList.set(redeemedSoFar,
                            RewardTransaction.redeemed(redeemed.getAccountId(),
                                    availableInThisRedeemed - canConsume, redeemed.getReferenceTransactionId()));
                }
            }

            if (earned.getExpiresOn() != null && earned.getExpiresOn().compareTo(Instant.now()) <= 0) {
                int unconsumed = earnedPoints - consumed;
                if (unconsumed > 0) {
                    totalUnredeemedExpired += unconsumed;
                }
            }
        }

        if (totalUnredeemedExpired > 0) {
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            int toDeduct = Math.min(totalUnredeemedExpired, account.getRewardPoints());
            if (toDeduct > 0) {
                account.setRewardPoints(account.getRewardPoints() - toDeduct);
                accountRepo.save(account);
                log.info("Deducted {} expired reward points from account {}", toDeduct, accountId);
            }
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expirePointsScheduled() {
        Instant now = Instant.now();
        List<Long> accountIds = rewardRepo.findAccountIdsWithExpiredEarnings(now);
        for (Long accountId : accountIds) {
            try {
                processExpiredPoints(accountId);
            } catch (Exception e) {
                log.error("Failed to process expired points for account {}: {}", accountId, e.getMessage());
            }
        }
    }
}
