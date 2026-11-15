package com.sidbi.bds.notification.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
public class NotificationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "message_text", nullable = false, length = 2000)
    private String message;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 500)
    private String description;
}
