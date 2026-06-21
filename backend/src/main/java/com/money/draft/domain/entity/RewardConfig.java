package com.money.draft.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_config")
public class RewardConfig {

    @Id
    private Long id;

    @Column(nullable = false)
    private int pointsPerUnit;

    @Column(length = 100)
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    protected RewardConfig() {}

    public RewardConfig(int pointsPerUnit) {
        this.id = 1L;
        this.pointsPerUnit = pointsPerUnit;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public int getPointsPerUnit() { return pointsPerUnit; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setPointsPerUnit(int pointsPerUnit) { this.pointsPerUnit = pointsPerUnit; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
