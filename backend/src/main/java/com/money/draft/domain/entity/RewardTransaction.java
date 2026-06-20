
package com.money.draft.domain.entity;

import com.money.draft.domain.enums.RewardTransactionType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reward_transactions", indexes = {
        @Index(name = "idx_rw_account_id", columnList = "accountId"),
        @Index(name = "idx_rw_reference_tx", columnList = "referenceTransactionId"),
        @Index(name = "idx_rw_expires_on", columnList = "expiresOn")
})
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RewardTransactionType type;

    @Column(nullable = false)
    private int points;

    @Column
    private Long referenceTransactionId;

    @Column(nullable = false)
    private Instant createdOn;

    @Column
    private Instant expiresOn;

    protected RewardTransaction() {}

    private RewardTransaction(Long accountId, RewardTransactionType type, int points, Long referenceTransactionId) {
        this.accountId = accountId;
        this.type = type;
        this.points = points;
        this.referenceTransactionId = referenceTransactionId;
    }

    public static RewardTransaction earned(Long accountId, int points, Long referenceTransactionId) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        if (points <= 0) throw new IllegalArgumentException("points must be positive");
        if (referenceTransactionId == null) throw new IllegalArgumentException("referenceTransactionId is required");
        RewardTransaction t = new RewardTransaction(accountId, RewardTransactionType.EARNED, points, referenceTransactionId);
        t.expiresOn = Instant.now().plusSeconds(2 * 24 * 60 * 60);
        return t;
    }

    public static RewardTransaction redeemed(Long accountId, int points, Long referenceTransactionId) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        if (points <= 0) throw new IllegalArgumentException("points must be positive");
        return new RewardTransaction(accountId, RewardTransactionType.REDEEMED, points, referenceTransactionId);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdOn == null) this.createdOn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public RewardTransactionType getType() { return type; }
    public int getPoints() { return points; }
    public Long getReferenceTransactionId() { return referenceTransactionId; }
    public Instant getCreatedOn() { return createdOn; }
    public Instant getExpiresOn() { return expiresOn; }
}
