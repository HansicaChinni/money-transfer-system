package com.money.draft.exception;

import java.math.BigDecimal;

public class AccountClosureException extends BusinessException {
    public AccountClosureException(Long accountId, BigDecimal balance) {
        super("ACCOUNT_NOT_CLOSED",
                "Cannot close account %d with non-zero balance of ₹%s".formatted(accountId, balance));
    }
}
