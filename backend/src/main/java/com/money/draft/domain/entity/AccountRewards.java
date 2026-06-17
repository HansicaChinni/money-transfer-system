package com.money.draft.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_rewards")
public class AccountRewards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long accountId;

    @Column(nullable = false)
    private int totalPoints;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    public AccountRewards() {}

    public AccountRewards(Long accountId) {
        this.accountId = accountId;
        this.totalPoints = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public void addPoints(int points) {
        this.totalPoints += points;
        this.lastUpdated = LocalDateTime.now();
    }

    public void deductPoints(int points) {
        this.totalPoints -= points;
        this.lastUpdated = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (lastUpdated == null) lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public int getTotalPoints() { return totalPoints; }
    public Long getVersion() { return version; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}
