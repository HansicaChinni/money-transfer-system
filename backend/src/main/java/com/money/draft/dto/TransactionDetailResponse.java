
package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDetailResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String status,
        String failureReason,
        String idempotencyKey,
        LocalDateTime createdOn,
        Integer rewardPointsEarned,
        Integer rewardPointsUsed
) {}
