
package com.money.draft.config;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.RewardTransaction;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.RewardTransactionType;
import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.RewardTransactionRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
public class RewardDataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RewardDataMigrationRunner.class);

    private final TransactionLogRepository txRepo;
    private final RewardTransactionRepository rewardRepo;
    private final AccountRepository accountRepo;

    public RewardDataMigrationRunner(TransactionLogRepository txRepo,
                                     RewardTransactionRepository rewardRepo,
                                     AccountRepository accountRepo) {
        this.txRepo = txRepo;
        this.rewardRepo = rewardRepo;
        this.accountRepo = accountRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int backfilled = 0;
        int earnedCreated = 0;
        int redeemedCreated = 0;

        List<TransactionLog> txs = txRepo.findByStatus(TransactionStatus.SUCCESS);
        for (TransactionLog tx : txs) {
            boolean changed = false;
            Long fromId = tx.getFromAccountId();
            Long txId = tx.getId();
            BigDecimal amount = tx.getAmount();

            if (backfillEarned(tx, fromId, txId)) {
                changed = true;
            }
            if (backfillRedeemed(tx, fromId, txId)) {
                changed = true;
            }
            if (changed) {
                backfilled++;
            }

            if (!hasEarnedEntry(fromId, txId) && amount.compareTo(new BigDecimal("100")) > 0) {
                int points = amount.divide(new BigDecimal("100"), RoundingMode.DOWN).intValue();
                if (points > 0) {
                    createEarnedEntry(fromId, txId, points);
                    tx.setRewardPointsEarned(points);
                    txRepo.save(tx);
                    earnedCreated++;
                }
            }

            if (tx.getRewardPointsUsed() != null && tx.getRewardPointsUsed() > 0
                    && !hasRedeemedEntry(fromId, txId)) {
                rewardRepo.save(RewardTransaction.redeemed(fromId, tx.getRewardPointsUsed(), txId));
                redeemedCreated++;
            }
        }

        int fixedRefs = fixNullReferenceTransactionIds();

        log.info("Reward data migration: {} TransactionLog backfilled, {} EARNED entries created, "
                + "{} REDEEMED entries created, {} REDEEMED references fixed",
                backfilled, earnedCreated, redeemedCreated, fixedRefs);
    }

    private boolean backfillEarned(TransactionLog tx, Long fromId, Long txId) {
        List<RewardTransaction> existing = rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(fromId, txId, RewardTransactionType.EARNED);
        if (!existing.isEmpty() && tx.getRewardPointsEarned() == null) {
            tx.setRewardPointsEarned(existing.get(0).getPoints());
            txRepo.save(tx);
            return true;
        }
        return false;
    }

    private boolean backfillRedeemed(TransactionLog tx, Long fromId, Long txId) {
        List<RewardTransaction> existing = rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(fromId, txId, RewardTransactionType.REDEEMED);
        if (!existing.isEmpty() && tx.getRewardPointsUsed() == null) {
            tx.setRewardPointsUsed(existing.get(0).getPoints());
            txRepo.save(tx);
            return true;
        }
        return false;
    }

    private boolean hasEarnedEntry(Long accountId, Long txId) {
        return !rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(accountId, txId, RewardTransactionType.EARNED)
                .isEmpty();
    }

    private boolean hasRedeemedEntry(Long accountId, Long txId) {
        return !rewardRepo
                .findByAccountIdAndReferenceTransactionIdAndType(accountId, txId, RewardTransactionType.REDEEMED)
                .isEmpty();
    }

    private void createEarnedEntry(Long accountId, Long txId, int points) {
        Optional<Account> accountOpt = accountRepo.findById(accountId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.creditRewardPoints(points);
            accountRepo.save(account);
        }
        rewardRepo.save(RewardTransaction.earned(accountId, points, txId));
        log.info("Created EARNED entry for transaction {} account {} points {}", txId, accountId, points);
    }

    private int fixNullReferenceTransactionIds() {
        List<RewardTransaction> broken = rewardRepo.findRedeemedWithNullReferenceTransactionId();
        int fixed = 0;
        for (RewardTransaction r : broken) {
            List<TransactionLog> matches = txRepo.findByFromAccountIdAndRewardPointsUsed(
                    r.getAccountId(), r.getPoints());
            if (matches.size() == 1) {
                r.setReferenceTransactionId(matches.get(0).getId());
                rewardRepo.save(r);
                fixed++;
                log.info("Fixed REDEEMED entry id={}: set referenceTransactionId={}",
                        r.getId(), matches.get(0).getId());
            } else if (matches.isEmpty()) {
                log.warn("No matching TransactionLog for REDEEMED entry id={} accountId={} points={}",
                        r.getId(), r.getAccountId(), r.getPoints());
            } else {
                log.warn("Multiple TransactionLog matches for REDEEMED entry id={} accountId={} points={} (count={})",
                        r.getId(), r.getAccountId(), r.getPoints(), matches.size());
            }
        }
        return fixed;
    }
}
