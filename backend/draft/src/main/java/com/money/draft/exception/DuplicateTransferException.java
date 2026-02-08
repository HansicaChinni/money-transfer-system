
package com.money.draft.exception;

public class DuplicateTransferException extends BusinessException {
    public DuplicateTransferException(String idempotencyKey) {
        super("Duplicate transfer detected for idempotencyKey=" + idempotencyKey);
    }
}
