package com.sidbi.bds.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "engagement_updates", indexes = @Index(name = "ix_eng_update_engagement", columnList = "engagement_id"))
@Getter
@Setter
public class EngagementUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engagement_id", nullable = false)
    private Long engagementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false, length = 20)
    private ProcEnums.UpdateType updateType;

    @Column(name = "message_text", length = 2000)
    private String message;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ProcEnums.EngagementStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private ProcEnums.EngagementStatus toStatus;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_role", length = 20)
    private String authorRole;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
