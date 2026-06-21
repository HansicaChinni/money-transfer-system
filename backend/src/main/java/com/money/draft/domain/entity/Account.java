
package com.money.draft.domain.entity;

import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.exception.AccountNotActiveException;
import com.money.draft.exception.InsufficientBalanceException;
import com.money.draft.exception.InsufficientRewardPointsException;
import com.money.draft.exception.ValidationException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

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
    private int rewardPoints;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    public Account() {
        // JPA only
    }

    public static String generateAccountNumber(Long id) {
        return "ACC-2026-" + String.format("%06d", id);
    }

    // ---- Domain methods ----
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void credit(BigDecimal amount) {
        requireActive();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than zero");
        }
        BigDecimal value = normalizedMoney(amount);
        this.balance = this.balance.add(value);
        touch();
    }

    public void debit(BigDecimal amount) {
        requireActive();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than zero");
        }
        BigDecimal value = normalizedMoney(amount);
        if (this.balance.compareTo(value) < 0) {
            throw new InsufficientBalanceException(this.id, this.balance, value);
        }
        this.balance = this.balance.subtract(value);
        touch();
    }

    private void requireActive() {
        if (!isActive()) {
            throw new AccountNotActiveException(this.id, this.status != null ? this.status.name() : "UNKNOWN");
        }
    }

    private static BigDecimal normalizedMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void touch() {
        this.lastUpdated = LocalDateTime.now(); // server local date-time
    }

    @PrePersist
    protected void onCreate() {
        if (this.lastUpdated == null) {
            this.lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public void creditRewardPoints(int points) {
        requireActive();
        if (points <= 0) throw new ValidationException("reward points must be positive");
        this.rewardPoints += points;
        touch();
    }

    public void debitRewardPoints(int points) {
        requireActive();
        if (points <= 0) throw new ValidationException("reward points must be positive");
        if (this.rewardPoints < points) {
            throw new InsufficientRewardPointsException(this.id, this.rewardPoints, points);
        }
        this.rewardPoints -= points;
        touch();
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }


}
