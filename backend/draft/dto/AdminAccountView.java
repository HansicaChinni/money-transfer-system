package com.money.draft.dto;


import java.math.BigDecimal;
import java.time.Instant;
public record AdminAccountView(Long id, BigDecimal balance, String status, Instant lastUpdated) {}

