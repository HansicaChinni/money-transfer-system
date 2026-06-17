package com.money.draft.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "reward_logs",
        indexes = {
                @Index(name = "idx_reward_account", columnList = "accountId"),
                @Index(name = "idx_reward_created_on", columnList = "createdOn")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reward_transaction", columnNames = "transactionId")
        }
)
public class RewardLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false, length = 255)
    private String eligibilityReason;

    @Column(nullable = false)
    private Instant createdOn;

    protected RewardLog() {
    }

    public RewardLog(Long transactionId,
                     Long accountId,
                     BigDecimal transactionAmount,
                     Integer points,
                     String eligibilityReason) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionAmount = transactionAmount;
        this.points = points;
        this.eligibilityReason = eligibilityReason;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdOn == null) {
            this.createdOn = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Long getTransactionId() { return transactionId; }
    public Long getAccountId() { return accountId; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public Integer getPoints() { return points; }
    public String getEligibilityReason() { return eligibilityReason; }
    public Instant getCreatedOn() { return createdOn; }
}
