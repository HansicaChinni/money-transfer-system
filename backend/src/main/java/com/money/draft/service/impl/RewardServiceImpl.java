package com.money.draft.service.impl;

import com.money.draft.domain.entity.RewardLog;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.domain.repository.RewardLogRepository;
import com.money.draft.dto.RewardLogResponse;
import com.money.draft.service.RewardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RewardServiceImpl implements RewardService {

    private static final BigDecimal MIN_ELIGIBLE_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal POINT_UNIT = new BigDecimal("100.00");

    private final RewardLogRepository rewardRepo;

    public RewardServiceImpl(RewardLogRepository rewardRepo) {
        this.rewardRepo = rewardRepo;
    }

    @Override
    @Transactional
    public int awardForEligibleTransfer(TransactionLog tx) {
        if (tx == null || !isEligible(tx)) {
            return 0;
        }

        return rewardRepo.findByTransactionId(tx.getId())
                .map(RewardLog::getPoints)
                .orElseGet(() -> {
                    int points = calculatePoints(tx.getAmount());
                    RewardLog reward = new RewardLog(
                            tx.getId(),
                            tx.getFromAccountId(),
                            tx.getAmount(),
                            points,
                            "SUCCESS transfer over 100 between different users"
                    );
                    rewardRepo.save(reward);
                    return points;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardLogResponse> getRewardsForAccount(Long accountId) {
        return rewardRepo.findByAccountIdOrderByCreatedOnDesc(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardLogResponse> getAllRewards() {
        return rewardRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isEligible(TransactionLog tx) {
        return tx.getStatus() == TransactionStatus.SUCCESS
                && tx.getAmount().compareTo(MIN_ELIGIBLE_AMOUNT) > 0
                && !tx.getFromAccountId().equals(tx.getToAccountId());
    }

    private int calculatePoints(BigDecimal amount) {
        return amount.divide(POINT_UNIT, 0, RoundingMode.DOWN).intValue();
    }

    private RewardLogResponse toResponse(RewardLog reward) {
        return new RewardLogResponse(
                reward.getId(),
                reward.getTransactionId(),
                reward.getAccountId(),
                reward.getTransactionAmount(),
                reward.getPoints(),
                reward.getEligibilityReason(),
                toLocalDateTime(reward.getCreatedOn())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}
