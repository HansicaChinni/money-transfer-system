package com.money.draft.dto;

public record LoginResponse(String token, String role, Long accountId) {}