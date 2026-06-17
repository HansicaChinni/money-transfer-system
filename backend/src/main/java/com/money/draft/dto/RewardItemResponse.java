package com.money.draft.dto;

import java.math.BigDecimal;

public record RewardItemResponse(
    Long id,
    String name,
    String description,
    String brand,
    int pointsRequired,
    BigDecimal couponValue,
    boolean isActive,
    String imageUrl
) {}
