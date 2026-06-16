package com.money.draft.exception;

import java.math.BigDecimal;

public class DailyLimitExceededException extends BusinessException {
    public DailyLimitExceededException(Long accountId, BigDecimal limit, BigDecimal attempted) {
        super("DAILY_LIMIT_EXCEEDED",
                "Daily transfer limit of ₹%s exceeded for account %d (attempted: ₹%s)"
                        .formatted(limit, accountId, attempted));
    }
}
