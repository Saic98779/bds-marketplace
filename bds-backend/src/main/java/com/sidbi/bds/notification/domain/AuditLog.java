package com.sidbi.bds.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "ix_audit_created", columnList = "created_at"),
        @Index(name = "ix_audit_actor", columnList = "actor_id"),
        @Index(name = "ix_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", length = 40)
    private String serviceName;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 20)
    private String actorRole;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "action_code", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
