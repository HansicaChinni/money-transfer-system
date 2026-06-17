package com.money.draft.domain.entity;

import com.money.draft.domain.enums.RedemptionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_requests", indexes = {
    @Index(name = "idx_redemption_account", columnList = "accountId"),
    @Index(name = "idx_redemption_status", columnList = "status")
})
public class RedemptionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Long rewardItemId;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RedemptionStatus status;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Column(length = 255)
    private String notes;

    @Column(nullable = false)
    private Instant createdOn;

    @Column
    private LocalDateTime fulfilledOn;

    public RedemptionRequest() {}

    public RedemptionRequest(Long accountId, Long rewardItemId, int pointsSpent) {
        this.accountId = accountId;
        this.rewardItemId = rewardItemId;
        this.pointsSpent = pointsSpent;
        this.status = RedemptionStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) createdOn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Long getRewardItemId() { return rewardItemId; }
    public int getPointsSpent() { return pointsSpent; }
    public RedemptionStatus getStatus() { return status; }
    public void setStatus(RedemptionStatus status) { this.status = status; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedOn() { return createdOn; }
    public LocalDateTime getFulfilledOn() { return fulfilledOn; }
    public void setFulfilledOn(LocalDateTime fulfilledOn) { this.fulfilledOn = fulfilledOn; }
}
