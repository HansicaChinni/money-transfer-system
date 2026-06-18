package com.money.draft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for Admin to create a full account and user profile simultaneously.
 */
public record AdminCreateAccountRequest(
        @NotBlank(message = "Username is required for the login")
        String username,

        @NotBlank(message = "Password is required for the login")
        String password,

        @NotBlank(message = "Holder name is required for the account")
        String holderName,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "1000.00", message = "Initial deposit must be at least ₹1000")
        BigDecimal initialBalance,

        String captchaToken,

        String captchaAnswer
) {
    public AdminCreateAccountRequest(String username, String password, String holderName, BigDecimal initialBalance) {
        this(username, password, holderName, initialBalance, null, null);
    }
}