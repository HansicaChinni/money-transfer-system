
package com.money.draft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** User-facing transfer request (server derives fromAccountId from JWT). */
public record MeTransferRequest(
        @NotNull Long toAccountId,
        @NotNull @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal amount
) {}
