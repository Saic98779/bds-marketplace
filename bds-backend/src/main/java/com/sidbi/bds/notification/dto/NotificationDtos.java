package com.sidbi.bds.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            String category,
            String severity,
            String title,
            String message,
            String linkUrl,
            boolean read,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
    }

    public record UnreadCount(long unread) {
    }

    public record EmailRequest(@NotBlank @Email String to, @NotBlank @Size(max = 250) String subject, @NotBlank @Size(max = 4000) String body) {
    }

    public record TemplateRequest(
            @NotBlank @Size(max = 250) String title,
            @NotBlank @Size(max = 2000) String message,
            @Size(max = 30) String category,
            @Size(max = 20) String severity,
            Boolean emailEnabled,
            Boolean smsEnabled,
            Boolean active,
            @Size(max = 500) String description
    ) {
    }

    public record TemplateResponse(
            Long id,
            String code,
            String category,
            String severity,
            String title,
            String message,
            boolean emailEnabled,
            boolean smsEnabled,
            boolean active,
            String description,
            List<String> placeholders,
            LocalDateTime updatedAt
    ) {
    }

    public record AuditResponse(
            Long id,
            String serviceName,
            Long actorId,
            String actorRole,
            String actorName,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress,
            LocalDateTime createdAt
    ) {
    }

    public record SettingRequest(@Size(max = 2000) String value) {
    }

    public record SettingResponse(Long id, String key, String value, String group, String description, boolean publicSetting,
                                  LocalDateTime updatedAt) {
    }

    public record ServiceHealth(String service, int instances, String status, List<String> urls) {
    }

    public record SystemHealth(String overall, List<ServiceHealth> services, long notificationsSent, long notificationsFailed,
                               long notificationsSkipped, LocalDateTime checkedAt) {
    }
}
