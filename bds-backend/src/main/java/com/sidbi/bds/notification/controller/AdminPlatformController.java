package com.sidbi.bds.notification.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.notification.dto.NotificationDtos;
import com.sidbi.bds.notification.service.PlatformAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Platform", description = "Audit logs, notification templates, settings and system health")
public class AdminPlatformController {

    private final PlatformAdminService service;

    @GetMapping("/audit-logs")
    @Operation(summary = "Search the audit log")
    public ApiResponse<PageResponse<NotificationDtos.AuditResponse>> audits(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(this.service.audits(new PlatformAdminService.AuditFilter(service, actorId, action, entityType, entityId, q, from, to), page, size));
    }

    @GetMapping("/audit-logs/export")
    @Operation(summary = "Export the audit log")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String service,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return this.service.exportAudits(new PlatformAdminService.AuditFilter(service, null, action, null, null, null, from, to), format);
    }

    @GetMapping("/notification-templates")
    @Operation(summary = "List notification templates")
    public ApiResponse<List<NotificationDtos.TemplateResponse>> templates() {
        return ApiResponse.ok(service.templates());
    }

    @PutMapping("/notification-templates/{templateId}")
    @Operation(summary = "Update a notification template")
    public ApiResponse<NotificationDtos.TemplateResponse> updateTemplate(@PathVariable Long templateId,
                                                                         @Valid @RequestBody NotificationDtos.TemplateRequest request) {
        return ApiResponse.ok("Template updated", service.updateTemplate(templateId, request));
    }

    @GetMapping("/settings")
    @Operation(summary = "List platform settings")
    public ApiResponse<List<NotificationDtos.SettingResponse>> settings() {
        return ApiResponse.ok(service.settings());
    }

    @PutMapping("/settings/{key}")
    @Operation(summary = "Update a platform setting")
    public ApiResponse<NotificationDtos.SettingResponse> updateSetting(@PathVariable String key, @Valid @RequestBody NotificationDtos.SettingRequest request) {
        return ApiResponse.ok("Setting updated", service.updateSetting(key, request));
    }

    @GetMapping("/system/health")
    @Operation(summary = "Backend health and notification delivery status")
    public ApiResponse<NotificationDtos.SystemHealth> health() {
        return ApiResponse.ok(service.health());
    }
}
