
package com.money.draft.dto;

public record RewardSummaryResponse(
        int currentPoints,
        int totalEarned,
        int totalRedeemed
) {}
