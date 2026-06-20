package com.money.draft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminAccountDetailResponse(
        Long id,
        String accountNumber,
        String holderName,
        BigDecimal balance,
        String status,
        Long version,
        LocalDateTime lastUpdated,
        int rewardPoints,
        int totalRewardsRedeemed
) {}
