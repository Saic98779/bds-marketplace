package com.sidbi.bds.notification.domain;

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
@Table(name = "notifications", indexes = {
        @Index(name = "ix_notification_recipient", columnList = "recipient_id, read_at"),
        @Index(name = "ix_notification_created", columnList = "created_at")
})
@Getter
@Setter
public class Notification {

    public enum DeliveryStatus {
        NOT_REQUIRED,
        PENDING,
        SENT,
        FAILED,
        SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "template_code", length = 60)
    private String templateCode;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "message_text", nullable = false, length = 2000)
    private String message;

    @Column(name = "link_url", length = 300)
    private String linkUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private DeliveryStatus emailStatus = DeliveryStatus.NOT_REQUIRED;

    @Column(name = "email_error", length = 500)
    private String emailError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
