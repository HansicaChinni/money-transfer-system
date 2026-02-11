
package com.money.draft.dto;

import java.math.BigDecimal;

/** Response model for transfer outcomes. */
public record TransferResponse(
        String status,        // "SUCCESS" or "FAILED"
        String message,       // optional human-readable message on failure
        Long transactionId,   // present when successful
        BigDecimal amount     // present when successful
) {
    public static TransferResponse success(Long txId, BigDecimal amount) {
        return new TransferResponse("SUCCESS", null, txId, amount);
    }
    public static TransferResponse failure(String message) {
        return new TransferResponse("FAILED", message, null, null);
    }
}
