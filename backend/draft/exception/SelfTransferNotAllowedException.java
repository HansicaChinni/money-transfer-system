
package com.money.draft.exception;

public class SelfTransferNotAllowedException extends BusinessException {
    public SelfTransferNotAllowedException(Long accountId) {
        super("Self transfer is not allowed for accountId=" + accountId);
    }
}
