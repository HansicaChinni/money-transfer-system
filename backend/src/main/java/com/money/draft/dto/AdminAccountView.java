
package com.money.draft.dto;

import java.math.BigDecimal;

/** Admin-facing account view (no names for privacy). */
public record AdminAccountView(
        Long id,
        String accountNumber,
        BigDecimal balance,
        String status,
        java.time.LocalDateTime lastUpdated
) {}

