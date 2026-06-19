
package com.money.draft.exception;

public class InsufficientRewardPointsException extends BusinessException {
    public InsufficientRewardPointsException(Long accountId, int available, int requested) {
        super("INSUFFICIENT_REWARD_POINTS",
                "Account " + accountId + " has " + available + " reward points but " + requested + " were requested");
    }
}
