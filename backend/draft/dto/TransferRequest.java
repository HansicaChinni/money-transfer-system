
package com.money.draft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {

    @NotNull(message = "fromAccountId is required")
    private Long fromAccountId;

    @NotNull(message = "toAccountId is required")
    private Long toAccountId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    // --- Getters/Setters ---

    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }

    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    // --- Cross-field validation helper (we'll also guard in service) ---
    public boolean isSelfTransfer() {
        return fromAccountId != null && fromAccountId.equals(toAccountId);
    }
}
