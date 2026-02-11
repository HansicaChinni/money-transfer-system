
package com.money.draft.exception;

public class AccountNotActiveException extends BusinessException {
    public AccountNotActiveException(Long id, String status) {
        super("ACCOUNT_NOT_ACTIVE", "Account " + id + " is not active (" + status + ")");
    }
}
