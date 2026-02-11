
package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** Read model for transaction logs for both user and admin views. */
public record TransactionLogResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String status,
        String failureReason,
        String idempotencyKey,
        LocalDateTime createdOn


) {}

