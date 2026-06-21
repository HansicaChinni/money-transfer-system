
package com.money.draft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MeTransferRequest(
        @NotBlank String toAccountNumber,
        @NotNull @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal amount,
        boolean useRewardPoints
) {}
