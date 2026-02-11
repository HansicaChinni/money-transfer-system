
package com.money.draft.domain.entity;

import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.exception.ValidationException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

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
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
        this.failureReason = truncate(failureReason, 255);
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * Success logs enforce strict domain constraints.
     */
    public static TransactionLog success(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey) {
        if (fromAccountId == null) throw new ValidationException("fromAccountId is required");
        if (toAccountId == null) throw new ValidationException("toAccountId is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new ValidationException("idempotencyKey is required");
        if (fromAccountId.equals(toAccountId)) {
            throw new ValidationException("fromAccountId and toAccountId cannot be the same for SUCCESS");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount must be greater than zero for SUCCESS");
        }
        return new TransactionLog(
                fromAccountId,
                toAccountId,
                normalizedMoney(amount),
                TransactionStatus.SUCCESS,
                null,
                idempotencyKey
        );
    }

    /**
     * Failure logs are more permissive to ensure we can always record failed attempts.
     * We still require non-null amount and idempotencyKey due to NOT NULL columns.
     */
    public static TransactionLog failure(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey, String reason) {
        if (fromAccountId == null) throw new ValidationException("fromAccountId is required");
        if (toAccountId == null) throw new ValidationException("toAccountId is required");
        if (amount == null) throw new ValidationException("amount is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new ValidationException("idempotencyKey is required");

        return new TransactionLog(
                fromAccountId,
                toAccountId,
                normalizedMoney(amount),
                TransactionStatus.FAILED,
                reason,
                idempotencyKey
        );
    }

    private static BigDecimal normalizedMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdOn == null) this.createdOn = Instant.now();
    }

    // ---- Getters (no setters to keep logs immutable) ----
    public Long getId() { return id; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedOn() { return createdOn; }
}
