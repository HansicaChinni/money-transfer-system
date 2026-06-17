package com.money.draft.dto;

public record RewardSummaryResponse(
    int totalPoints,
    Integer lastEarned,
    String lastEarnedOn
) {}
