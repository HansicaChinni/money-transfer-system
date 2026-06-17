package com.money.draft.exception;

public class InsufficientRewardPointsException extends BusinessException {
    public InsufficientRewardPointsException(int available, int required) {
        super("INSUFFICIENT_REWARD_POINTS",
                "You need %d points but only have %d".formatted(required, available));
    }
}
