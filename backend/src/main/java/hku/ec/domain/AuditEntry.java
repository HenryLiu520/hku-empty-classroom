package hku.ec.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity", nullable = false)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditEntry() { }

    public AuditEntry(String action, String entity, Long entityId, String actor, String detail) {
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.actor = actor;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getEntity() { return entity; }
    public Long getEntityId() { return entityId; }
    public String getActor() { return actor; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
