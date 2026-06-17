
package com.money.draft.dto;

import java.math.BigDecimal;

/** Response model for transfer outcomes. */
public record TransferResponse(
        String status,        // "SUCCESS" or "FAILED"
        String message,       // optional human-readable message on failure
        Long transactionId,   // present when successful
        BigDecimal amount,    // present when successful
        Integer rewardPoints  // reward points earned by the sender
) {
    public static TransferResponse success(Long txId, BigDecimal amount) {
        return success(txId, amount, 0);
    }

    public static TransferResponse success(Long txId, BigDecimal amount, Integer rewardPoints) {
        return new TransferResponse("SUCCESS", null, txId, amount, rewardPoints);
    }

    public static TransferResponse failure(String message) {
        return new TransferResponse("FAILED", message, null, null, 0);
    }
}
