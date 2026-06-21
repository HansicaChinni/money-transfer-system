
package com.money.draft.config;

import com.money.draft.domain.repository.RewardTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RewardExpiryMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RewardExpiryMigrationRunner.class);

    private final RewardTransactionRepository rewardRepo;

    public RewardExpiryMigrationRunner(RewardTransactionRepository rewardRepo) {
        this.rewardRepo = rewardRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var unset = rewardRepo.findEarnedWithNullExpiresOn();
        if (unset.isEmpty()) {
            log.info("Reward expiry migration: no earned transactions without expiresOn found.");
            return;
        }
        int updated = 0;
        for (var r : unset) {
            Instant expiresOn = r.getCreatedOn().plus(2, ChronoUnit.DAYS);
            r.setExpiresOn(expiresOn);
            rewardRepo.save(r);
            updated++;
            log.debug("Set expiresOn={} for reward transaction id={} (createdOn={})",
                    expiresOn, r.getId(), r.getCreatedOn());
        }
        log.info("Reward expiry migration complete. {} earned transaction(s) updated with expiresOn.", updated);
    }
}
