
package com.money.draft.exception;

public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException(Long id) {
        super("Account not found: " + id);
    }
}
