
package com.money.draft.domain.enums;

public enum AccountStatus {
    ACTIVE,
    LOCKED,
    CLOSED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
