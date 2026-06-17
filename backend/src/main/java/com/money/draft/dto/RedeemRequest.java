package com.money.draft.dto;

import jakarta.validation.constraints.NotNull;

public record RedeemRequest(
    @NotNull Long rewardItemId
) {}
