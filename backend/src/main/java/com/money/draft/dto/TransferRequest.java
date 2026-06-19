
package com.money.draft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Internal service DTO for executing transfers with idempotency.
 * Controllers can build this from MeTransferRequest or admin/internal flows.
 */
public record TransferRequest(
        @NotNull(message = "fromAccountId is required") Long fromAccountId,
        @NotNull(message = "toAccountId is required") Long toAccountId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be at least 0.01") BigDecimal amount,
        @NotBlank(message = "idempotencyKey is required") String idempotencyKey,
        boolean useRewardPoints
) {
    /** Helper for service-level cross-field guard (in addition to validations). */
    public boolean isSelfTransfer() {
        return fromAccountId != null && fromAccountId.equals(toAccountId);
    }
}
