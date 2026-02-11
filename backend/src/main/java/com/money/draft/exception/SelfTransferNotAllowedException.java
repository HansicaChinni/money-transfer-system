
package com.money.draft.exception;

import jakarta.validation.constraints.NotNull;

public class SelfTransferNotAllowedException extends BusinessException {
    public SelfTransferNotAllowedException(@NotNull Long accountId) {
        super("SELF_TRANSFER_NOT_ALLOWED", "Self transfer is not allowed for accountId=" + accountId);
    }
}
