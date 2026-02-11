
package com.money.draft.exception;

import jakarta.validation.constraints.NotNull;

public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException(@NotNull Long id) {
        super("ACCOUNT_NOT_FOUND", "Account not found: " + id);
    }
}
