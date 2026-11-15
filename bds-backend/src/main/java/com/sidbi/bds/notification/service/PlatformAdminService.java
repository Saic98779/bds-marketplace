package com.sidbi.bds.notification.service;

import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.dto.AuditEvent;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.report.ReportExporter;
import com.sidbi.bds.common.report.ReportFormat;
import com.sidbi.bds.common.report.ReportTable;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.util.Search;
import com.sidbi.bds.notification.domain.AuditLog;
import com.sidbi.bds.notification.domain.Notification;
import com.sidbi.bds.notification.domain.NotificationTemplate;
import com.sidbi.bds.notification.domain.PlatformSetting;
import com.sidbi.bds.notification.dto.NotificationDtos;
import com.sidbi.bds.notification.repository.AuditLogRepository;
import com.sidbi.bds.notification.repository.NotificationRepository;
import com.sidbi.bds.notification.repository.NotificationTemplateRepository;
import com.sidbi.bds.notification.repository.PlatformSettingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final NotificationTemplateRepository templateRepository;
    private final AuditLogRepository auditRepository;
    private final PlatformSettingRepository settingRepository;
    private final NotificationRepository notificationRepository;
    private final HealthEndpoint healthEndpoint;
    private final ReportExporter exporter;

    public record AuditFilter(String service, Long actorId, String action, String entityType, String entityId, String q,
                              LocalDate from, LocalDate to) {
    }

    @Transactional
    public void record(AuditEvent event) {
        AuditLog log = new AuditLog();
        log.setServiceName(event.service());
        log.setActorId(event.actorId());
        log.setActorRole(event.actorRole());
        log.setActorName(event.actorName());
        log.setAction(event.action() == null ? "UNKNOWN" : event.action());
        log.setEntityType(event.entityType());
        log.setEntityId(event.entityId());
        log.setDetails(event.details() != null && event.details().length() > 2000 ? event.details().substring(0, 2000) : event.details());
        log.setIpAddress(event.ipAddress());
        auditRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDtos.AuditResponse> audits(AuditFilter filter, int page, int size) {
        CurrentUser.require(Permission.AUDIT_VIEW);
        Page<AuditLog> result = auditRepository.findAll(spec(filter), PageRequestFactory.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, this::map);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportAudits(AuditFilter filter, ReportFormat format) {
        CurrentUser.require(Permission.AUDIT_VIEW);
        List<AuditLog> logs = auditRepository.findAll(spec(filter), PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        ReportTable table = new ReportTable("Audit Log", "Timestamp", "Service", "Actor", "Role", "Action", "Entity", "Entity ID", "Details", "IP Address");
        for (AuditLog l : logs) {
            table.row(l.getCreatedAt().format(STAMP), l.getServiceName(), l.getActorName(), l.getActorRole(), l.getAction(),
                    l.getEntityType(), l.getEntityId(), l.getDetails(), l.getIpAddress());
        }
        return exporter.export(table, format, "audit-log");
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.TemplateResponse> templates() {
        CurrentUser.require(Permission.SETTINGS_MANAGE);
        return templateRepository.findAllByOrderByCategoryAscCodeAsc().stream().map(this::map).toList();
    }

    @Transactional
    public NotificationDtos.TemplateResponse updateTemplate(Long id, NotificationDtos.TemplateRequest request) {
        CurrentUser.require(Permission.SETTINGS_MANAGE);
        NotificationTemplate template = templateRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Template not found"));
        template.setTitle(request.title());
        template.setMessage(request.message());
        if (request.category() != null) {
            template.setCategory(request.category());
        }
        if (request.severity() != null) {
            template.setSeverity(request.severity());
        }
        template.setEmailEnabled(Boolean.TRUE.equals(request.emailEnabled()));
        template.setSmsEnabled(Boolean.TRUE.equals(request.smsEnabled()));
        template.setActive(request.active() == null || request.active());
        template.setDescription(request.description());
        audit("TEMPLATE_UPDATED", "NOTIFICATION_TEMPLATE", template.getCode());
        return map(template);
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.SettingResponse> settings() {
        CurrentUser.require(Permission.SETTINGS_MANAGE);
        return settingRepository.findAllByOrderByGroupAscKeyAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> publicSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        settingRepository.findByPublicSettingTrue().forEach(s -> result.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
        return result;
    }

    @Transactional
    public NotificationDtos.SettingResponse updateSetting(String key, NotificationDtos.SettingRequest request) {
        CurrentUser.require(Permission.SETTINGS_MANAGE);
        PlatformSetting setting = settingRepository.findByKey(key).orElseThrow(() -> BusinessException.notFound("Setting not found"));
        setting.setValue(request.value());
        audit("SETTING_UPDATED", "PLATFORM_SETTING", key);
        return map(setting);
    }

    public NotificationDtos.SystemHealth health() {
        CurrentUser.require(Permission.SETTINGS_MANAGE);
        Status status = healthEndpoint.health().getStatus();
        boolean allUp = Status.UP.equals(status);
        List<NotificationDtos.ServiceHealth> services = List.of(
                new NotificationDtos.ServiceHealth("bds-backend", 1, allUp ? "UP" : "DEGRADED", List.of()));
        long sent = 0;
        long failed = 0;
        long skipped = 0;
        for (Object[] row : notificationRepository.countByEmailStatus()) {
            Notification.DeliveryStatus s = (Notification.DeliveryStatus) row[0];
            long c = ((Number) row[1]).longValue();
            switch (s) {
                case SENT -> sent = c;
                case FAILED -> failed = c;
                case SKIPPED -> skipped = c;
                default -> {
                }
            }
        }
        return new NotificationDtos.SystemHealth(allUp ? "UP" : "DEGRADED", services, sent, failed, skipped, LocalDateTime.now());
    }

    private void audit(String action, String type, String id) {
        var user = CurrentUser.get();
        record(new AuditEvent("notification", user.userId(), user.role().name(), user.name(), action, type, id, null, null));
    }

    private NotificationDtos.AuditResponse map(AuditLog l) {
        return new NotificationDtos.AuditResponse(l.getId(), l.getServiceName(), l.getActorId(), l.getActorRole(), l.getActorName(),
                l.getAction(), l.getEntityType(), l.getEntityId(), l.getDetails(), l.getIpAddress(), l.getCreatedAt());
    }

    private NotificationDtos.TemplateResponse map(NotificationTemplate t) {
        return new NotificationDtos.TemplateResponse(t.getId(), t.getCode(), t.getCategory(), t.getSeverity(), t.getTitle(), t.getMessage(),
                t.isEmailEnabled(), t.isSmsEnabled(), t.isActive(), t.getDescription(),
                List.copyOf(NotificationService.placeholders(t.getTitle(), t.getMessage())), t.getUpdatedAt());
    }

    private NotificationDtos.SettingResponse map(PlatformSetting s) {
        return new NotificationDtos.SettingResponse(s.getId(), s.getKey(), s.getValue(), s.getGroup(), s.getDescription(),
                s.isPublicSetting(), s.getUpdatedAt());
    }

    private static Specification<AuditLog> spec(AuditFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.service() != null && !filter.service().isBlank()) {
                predicates.add(cb.equal(root.get("serviceName"), filter.service()));
            }
            if (filter.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), filter.actorId()));
            }
            if (filter.action() != null && !filter.action().isBlank()) {
                predicates.add(cb.like(root.get("action"), Search.containsExact(filter.action().trim().toUpperCase(Locale.ROOT)), Search.ESCAPE));
            }
            if (filter.entityType() != null && !filter.entityType().isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), filter.entityType()));
            }
            if (filter.entityId() != null && !filter.entityId().isBlank()) {
                predicates.add(cb.equal(root.get("entityId"), filter.entityId()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = Search.contains(filter.q());
                predicates.add(cb.or(cb.like(cb.lower(root.get("actorName")), like, Search.ESCAPE), cb.like(cb.lower(root.get("details")), like, Search.ESCAPE)));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), filter.to().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
