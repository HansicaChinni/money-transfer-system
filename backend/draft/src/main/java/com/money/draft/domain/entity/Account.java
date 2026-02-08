
package com.money.draft.domain.entity;

import com.money.draft.domain.enums.AccountStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant lastUpdated;

    public Account() { } // JPA

    public Account(String holderName, BigDecimal openingBalance) {
        this.holderName = Objects.requireNonNull(holderName, "holderName is required");
        this.balance = normalizedMoney(Objects.requireNonNull(openingBalance, "openingBalance is required"));
        if (this.balance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        this.status = AccountStatus.ACTIVE;
        this.lastUpdated = Instant.now();
    }

    // ---- Domain methods ----
    public boolean isActive() {
        return status != null && status == AccountStatus.ACTIVE;
    }

    public void credit(BigDecimal amount) {
        requireActive();
        BigDecimal value = normalizedMoney(Objects.requireNonNull(amount));
        if (value.signum() <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        this.balance = this.balance.add(value);
        touch();
    }

    public void debit(BigDecimal amount) {
        requireActive();
        BigDecimal value = normalizedMoney(Objects.requireNonNull(amount));
        if (value.signum() <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (this.balance.compareTo(value) < 0) throw new IllegalStateException("Insufficient balance");
        this.balance = this.balance.subtract(value);
        touch();
    }

    private void requireActive() {
        if (!isActive()) throw new IllegalStateException("Account is not active (" + status + ")");
    }

    private static BigDecimal normalizedMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void touch() {
        this.lastUpdated = Instant.now();
    }

    // ---- Getters & Setters (needed by your services) ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
