package com.money.draft.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRewardItemRequest(
    @NotBlank String name,
    String description,
    @NotBlank String brand,
    @NotNull @Min(1) Integer pointsRequired,
    @NotNull BigDecimal couponValue,
    String imageUrl
) {}
