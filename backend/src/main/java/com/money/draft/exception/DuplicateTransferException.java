
package com.money.draft.exception;

public class DuplicateTransferException extends BusinessException {
    public DuplicateTransferException(String idempotencyKey) {
        super("DUPLICATE_TRANSFER", "Duplicate transfer detected for idempotencyKey=" + idempotencyKey);
    }
}

