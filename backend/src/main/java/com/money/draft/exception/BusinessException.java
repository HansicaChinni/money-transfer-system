
package com.money.draft.exception;

/**
 * Base business exception with a machine-readable code and human-readable message.
 */
public abstract class BusinessException extends RuntimeException {
    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
