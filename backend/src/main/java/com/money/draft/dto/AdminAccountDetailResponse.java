package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Specialized detail view for Admin search.
 * Includes audit fields like version and lastUpdated.
 */
public record AdminAccountDetailResponse(
        Long id,
        String holderName,
        BigDecimal balance,
        String status,
        Long version,
        LocalDateTime lastUpdated
) {}