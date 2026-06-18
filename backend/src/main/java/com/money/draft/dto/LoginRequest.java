
package com.money.draft.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String captchaToken,
        String captchaAnswer
) {
    public LoginRequest(String username, String password) {
        this(username, password, null, null);
    }
}
