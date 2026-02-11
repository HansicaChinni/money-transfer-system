
package com.money.draft.exception;

public class IncorrectPasswordException extends BusinessException {
    public IncorrectPasswordException() {
        super("INCORRECT_PASSWORD", "Current password is incorrect");
    }
}
