
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
    private String failureReason;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdOn;

    @Column
    private Integer rewardPointsEarned;

    @Column
    private Integer rewardPointsUsed;

    protected TransactionLog() { }

    private TransactionLog(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            TransactionStatus status,
            String failureReason,
            String idempotencyKey,
            Integer rewardPointsEarned,
            Integer rewardPointsUsed
    ) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
        this.failureReason = truncate(failureReason, 255);
        this.idempotencyKey = idempotencyKey;
        this.rewardPointsEarned = rewardPointsEarned;
        this.rewardPointsUsed = rewardPointsUsed;
    }

    public static TransactionLog success(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey,
                                          Integer rewardPointsEarned, Integer rewardPointsUsed) {
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
                idempotencyKey,
                rewardPointsEarned != null && rewardPointsEarned > 0 ? rewardPointsEarned : null,
                rewardPointsUsed != null && rewardPointsUsed > 0 ? rewardPointsUsed : null
        );
    }

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
                idempotencyKey,
                null,
                null
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

    public Long getId() { return id; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedOn() { return createdOn; }
    public Integer getRewardPointsEarned() { return rewardPointsEarned; }
    public Integer getRewardPointsUsed() { return rewardPointsUsed; }
}
