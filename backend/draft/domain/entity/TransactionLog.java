
package com.money.draft.domain.entity;

import com.money.draft.domain.enums.TransactionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "transaction_logs",
        indexes = {
                @Index(name = "idx_tx_from_account", columnList = "fromAccountId"),
                @Index(name = "idx_tx_to_account", columnList = "toAccountId"),
                @Index(name = "idx_tx_created_on", columnList = "createdOn")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tx_idempotency", columnNames = "idempotencyKey")
        }
)
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We store only IDs (not @ManyToOne) to keep the log immutable and simple.
    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(length = 255)
    private String failureReason;    // null for SUCCESS

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdOn;

    protected TransactionLog() { }

    private TransactionLog(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            TransactionStatus status,
            String failureReason,
            String idempotencyKey
    ) {
        this.fromAccountId = Objects.requireNonNull(fromAccountId, "fromAccountId is required");
        this.toAccountId = Objects.requireNonNull(toAccountId, "toAccountId is required");
        if (Objects.equals(fromAccountId, toAccountId)) {
            throw new IllegalArgumentException("fromAccountId and toAccountId cannot be the same");
        }
        this.amount = normalizedMoney(Objects.requireNonNull(amount, "amount is required"));
        if (this.amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.status = Objects.requireNonNull(status, "status is required");
        this.failureReason = failureReason;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        this.createdOn = Instant.now();
    }

    public static TransactionLog success(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey) {
        return new TransactionLog(fromAccountId, toAccountId, amount, TransactionStatus.SUCCESS, null, idempotencyKey);
    }

    public static TransactionLog failure(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey, String reason) {
        return new TransactionLog(fromAccountId, toAccountId, amount, TransactionStatus.FAILED, reason, idempotencyKey);
    }

    private static BigDecimal normalizedMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- Getters (no setters for immutability of logs) ----
    public Long getId() { return id; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedOn() { return createdOn; }
}
