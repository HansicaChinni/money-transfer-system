
package com.money.draft.dto;

import java.math.BigDecimal;

/** Admin-facing account view. */
public record AdminAccountView(
        Long id,
        String accountNumber,
        String holderName,
        BigDecimal balance,
        String status,
        java.time.LocalDateTime lastUpdated
) {}

