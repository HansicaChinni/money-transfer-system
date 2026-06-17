package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record RedemptionResponse(
    Long id,
    Long rewardItemId,
    String itemName,
    String brand,
    int pointsSpent,
    BigDecimal couponValue,
    String status,
    String couponCode,
    String notes,
    Instant createdOn,
    LocalDateTime fulfilledOn
) {}
