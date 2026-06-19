
package com.money.draft.dto;

import java.math.BigDecimal;

/** Response model for transfer outcomes. */
public record TransferResponse(
        String status,              // "SUCCESS" or "FAILED"
        String message,             // optional human-readable message on failure
        Long transactionId,         // present when successful
        BigDecimal amount,          // present when successful
        Integer rewardPointsUsed,   // present when useRewardPoints was true
        Integer rewardPointsEarned  // present when eligible and successful
) {
    public static TransferResponse success(Long txId, BigDecimal amount, int rewardPointsUsed, int rewardPointsEarned) {
        return new TransferResponse("SUCCESS", null, txId, amount,
                rewardPointsUsed > 0 ? rewardPointsUsed : null,
                rewardPointsEarned > 0 ? rewardPointsEarned : null);
    }
    public static TransferResponse failure(String message) {
        return new TransferResponse("FAILED", message, null, null, null, null);
    }
}
