package com.sidbi.bds.auth.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import com.sidbi.bds.common.crypto.EncryptedStringConverter;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "ix_users_role_status", columnList = "role, status"),
        @Index(name = "ix_users_reference", columnList = "reference_no")
})
@Getter
@Setter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, length = 512)
    private String email;

    @Column(name = "email_hash", nullable = false, unique = true, length = 64)
    private String emailHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "mobile", length = 512)
    private String mobile;

    @Column(name = "mobile_hash", unique = true, length = 64)
    private String mobileHash;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Enums.UserStatus status;

    @Column(name = "reference_no", length = 30, unique = true)
    private String referenceNo;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "mobile_verified", nullable = false)
    private boolean mobileVerified;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "email_alerts", nullable = false)
    private boolean emailAlerts = true;

    @Column(name = "catalog_sync_pending", nullable = false)
    private boolean catalogSyncPending;

    @Column(name = "created_by_admin")
    private Long createdByAdmin;

    @Column(name = "status_remarks", length = 1000)
    private String statusRemarks;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_code", length = 40)
    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}
