package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardTransactionResponse(
    Long id,
    Long transactionId,
    int pointsEarned,
    BigDecimal amount,
    String reason,
    LocalDateTime createdOn
) {}
