package com.money.draft.service;

import com.money.draft.dto.*;
import java.math.BigDecimal;
import java.util.List;

public interface RewardService {
    void grantRewardIfEligible(Long fromAccountId, Long toAccountId, Long transactionId, BigDecimal amount);
    RewardSummaryResponse getRewardSummary(Long accountId);
    List<RewardTransactionResponse> getRewardHistory(Long accountId);
    List<RewardItemResponse> getAvailableItems();
    RedemptionResponse redeem(Long accountId, Long rewardItemId);
    List<RedemptionResponse> getRedemptions(Long accountId);
    List<RedemptionResponse> getAllRedemptions();
    RedemptionResponse fulfillRedemption(Long redemptionId, String notes);
    RedemptionResponse cancelRedemption(Long redemptionId);
    RewardItemResponse createItem(CreateRewardItemRequest req);
    RewardItemResponse updateItem(Long id, CreateRewardItemRequest req);
    void deleteItem(Long id);

    int getPointsPerUnit();
    void updatePointsPerUnit(int pointsPerUnit, String performedBy);
}
