package com.money.draft.service;

import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.dto.RewardLogResponse;

import java.util.List;

public interface RewardService {
    int awardForEligibleTransfer(TransactionLog tx);

    List<RewardLogResponse> getRewardsForAccount(Long accountId);

    List<RewardLogResponse> getAllRewards();
}
