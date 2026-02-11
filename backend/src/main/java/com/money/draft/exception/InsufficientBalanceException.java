
package com.money.draft.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(Long accountId, BigDecimal balance, BigDecimal requested) {
        super("INSUFFICIENT_FUNDS",
                "Insufficient balance in account %d: balance=%s, requested=%s"
                        .formatted(accountId, balance, requested));
    }


}
