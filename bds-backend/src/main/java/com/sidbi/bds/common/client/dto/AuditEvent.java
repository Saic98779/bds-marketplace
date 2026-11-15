package com.sidbi.bds.common.client.dto;

public record AuditEvent(
        String service,
        Long actorId,
        String actorRole,
        String actorName,
        String action,
        String entityType,
        String entityId,
        String details,
        String ipAddress
) {
}
