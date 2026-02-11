
package com.money.draft.dto;

public record ChangePasswordRequest(
        @jakarta.validation.constraints.NotBlank String currentPassword,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 8) String newPassword
) {}
