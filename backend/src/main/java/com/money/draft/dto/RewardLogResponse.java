package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardLogResponse(
        Long id,
        Long transactionId,
        Long accountId,
        BigDecimal transactionAmount,
        Integer points,
        String eligibilityReason,
        LocalDateTime createdOn
) {}
