package com.money.draft.domain.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditLog() {}

    public AuditLog(String action, String entityType, Long entityId, String performedBy, String oldValue, String newValue) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.performedBy = performedBy;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getPerformedBy() { return performedBy; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getTimestamp() { return timestamp; }
}
