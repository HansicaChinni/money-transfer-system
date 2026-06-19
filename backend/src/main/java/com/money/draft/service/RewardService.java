
package com.money.draft.service;

import com.money.draft.domain.entity.Account;
import com.money.draft.dto.RewardSummaryResponse;
import com.money.draft.dto.RewardTransactionResponse;
import com.money.draft.dto.TransactionDetailResponse;

import java.math.BigDecimal;
import java.util.List;

public interface RewardService {
    int redeemDuringTransfer(Account from, BigDecimal amount);
    void grantPoints(Long accountId, Long transactionId, BigDecimal actualDebit);
    RewardSummaryResponse getRewardSummary(Long accountId);
    List<RewardTransactionResponse> getRewardTransactions(Long accountId);
    TransactionDetailResponse getTransactionDetail(Long accountId, Long transactionId);
    List<RewardTransactionResponse> getAllRewardTransactions();
    RewardSummaryResponse getRewardSummaryForAdmin(Long accountId);
}
