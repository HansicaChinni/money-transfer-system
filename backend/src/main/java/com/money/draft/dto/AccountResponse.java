
package com.money.draft.dto;

import java.math.BigDecimal;

/**
 * User-facing account view (e.g., /me/balance).
 * holderName is included for the owner; admin uses AdminAccountView (no names).
 */
public record AccountResponse(
        Long id,
        String accountNumber,
        String holderName,
        BigDecimal balance,
        String status
) {}
