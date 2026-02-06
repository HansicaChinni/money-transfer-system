
package com.money.draft.exception;

public class AccountNotActiveException extends BusinessException {
    public AccountNotActiveException(Long id, String status) {
        super("Account " + id + " is not active (" + status + ")");
    }
}
