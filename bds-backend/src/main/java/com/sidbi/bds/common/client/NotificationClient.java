package com.sidbi.bds.common.client;

import com.sidbi.bds.common.client.dto.AuditEvent;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.notification.dto.NotificationDtos;
import com.sidbi.bds.notification.service.NotificationService;
import com.sidbi.bds.notification.service.PlatformAdminService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class NotificationClient {

    private static final String BASE_PACKAGE = "com.sidbi.bds.";

    private final ObjectProvider<NotificationService> notificationService;
    private final ObjectProvider<PlatformAdminService> platformService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public NotificationClient(ObjectProvider<NotificationService> notificationService, ObjectProvider<PlatformAdminService> platformService) {
        this.notificationService = notificationService;
        this.platformService = platformService;
    }

    public void notify(NotificationRequest request) {
        executor.submit(() -> {
            try {
                notificationService.getObject().dispatch(request);
            } catch (RuntimeException ignored) {
            }
        });
    }

    public void notify(Long recipientId, String templateCode, String category, String linkUrl, Map<String, String> params) {
        if (recipientId != null) {
            notify(NotificationRequest.to(recipientId, templateCode, category, linkUrl, params));
        }
    }

    public void email(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        executor.submit(() -> {
            try {
                notificationService.getObject().sendDirect(new NotificationDtos.EmailRequest(to, subject, body));
            } catch (RuntimeException ignored) {
            }
        });
    }

    public void audit(String action, String entityType, Object entityId, String details) {
        Optional<AuthUser> actor = CurrentUser.find();
        audit(new AuditEvent(
                callerModule(),
                actor.map(AuthUser::userId).orElse(null),
                actor.map(a -> a.role().name()).orElse("SYSTEM"),
                actor.map(AuthUser::name).orElse("System"),
                action,
                entityType,
                entityId == null ? null : String.valueOf(entityId),
                details,
                clientIp()
        ));
    }

    public void audit(AuditEvent event) {
        executor.submit(() -> {
            try {
                platformService.getObject().record(event);
            } catch (RuntimeException ignored) {
            }
        });
    }

    /** Audit entries are labelled with the business module (auth, catalog, procurement, notification) that raised them. */
    private static String callerModule() {
        return StackWalker.getInstance().walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .filter(name -> name.startsWith(BASE_PACKAGE) && name.indexOf('.', BASE_PACKAGE.length()) > 0
                        && !name.startsWith(BASE_PACKAGE + "common."))
                .map(name -> name.substring(BASE_PACKAGE.length(), name.indexOf('.', BASE_PACKAGE.length())))
                .findFirst()
                .orElse("system"));
    }

    private static String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
