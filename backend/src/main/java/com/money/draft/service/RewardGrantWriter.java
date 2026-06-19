
package com.money.draft.service;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.RewardTransaction;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.RewardTransactionRepository;
import com.money.draft.exception.AccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RewardGrantWriter {

    private static final Logger log = LoggerFactory.getLogger(RewardGrantWriter.class);

    private final AccountRepository accountRepo;
    private final RewardTransactionRepository rewardRepo;

    public RewardGrantWriter(AccountRepository accountRepo, RewardTransactionRepository rewardRepo) {
        this.accountRepo = accountRepo;
        this.rewardRepo = rewardRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void grantPoints(Long accountId, Long transactionId, BigDecimal actualDebit) {
        try {
            if (actualDebit == null || actualDebit.compareTo(new BigDecimal("100")) <= 0) return;
            int points = actualDebit.divide(new BigDecimal("100"), RoundingMode.DOWN).intValue();
            if (points <= 0) return;

            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            account.creditRewardPoints(points);
            accountRepo.save(account);
            rewardRepo.save(RewardTransaction.earned(accountId, points, transactionId));
            log.info("Granted {} reward points to account {} for transaction {}", points, accountId, transactionId);
        } catch (Exception e) {
            log.error("Failed to grant reward points for account {} transaction {}: {}",
                    accountId, transactionId, e.getMessage());
        }
    }
}
