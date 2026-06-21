package com.money.draft.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reward_transactions", indexes = {
    @Index(name = "idx_reward_account", columnList = "accountId"),
    @Index(name = "idx_reward_tx", columnList = "transactionId")
})
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column
    private Long transactionId;

    @Column(nullable = false)
    private int pointsEarned;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdOn;

    protected RewardTransaction() {}

    public RewardTransaction(Long accountId, Long transactionId, int pointsEarned, BigDecimal amount, String reason) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.pointsEarned = pointsEarned;
        this.amount = amount;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) createdOn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Long getTransactionId() { return transactionId; }
    public int getPointsEarned() { return pointsEarned; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public Instant getCreatedOn() { return createdOn; }
}
