package com.sidbi.bds.notification.service;

import com.sidbi.bds.common.api.PageRequestFactory;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.dto.NotificationRequest;
import com.sidbi.bds.common.client.dto.UserContact;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.notification.domain.Notification;
import com.sidbi.bds.notification.domain.NotificationTemplate;
import com.sidbi.bds.notification.dto.NotificationDtos;
import com.sidbi.bds.notification.repository.NotificationRepository;
import com.sidbi.bds.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final IdentityClient identityClient;
    private final EmailSender emailSender;

    @Transactional
    public int dispatch(NotificationRequest request) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (request.recipientIds() != null) {
            request.recipientIds().stream().filter(id -> id != null).forEach(recipients::add);
        }
        if (request.recipientPermission() != null && !request.recipientPermission().isBlank()) {
            try {
                recipients.addAll(identityClient.usersWithPermission(request.recipientPermission()));
            } catch (RuntimeException ignored) {
            }
        }
        if (recipients.isEmpty()) {
            return 0;
        }
        NotificationTemplate template = request.templateCode() == null ? null
                : templateRepository.findByCode(request.templateCode()).orElse(null);
        if (template != null && !template.isActive()) {
            return 0;
        }
        Map<String, String> params = request.params() == null ? Map.of() : request.params();
        String title = render(template != null ? template.getTitle() : params.getOrDefault("title", "BDS Marketplace update"), params);
        String message = render(template != null ? template.getMessage() : params.getOrDefault("message", ""), params);
        for (Long recipientId : recipients) {
            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setTemplateCode(request.templateCode());
            notification.setCategory(request.category() != null ? request.category() : template != null ? template.getCategory() : "SYSTEM");
            notification.setSeverity(template != null ? template.getSeverity() : "INFO");
            notification.setTitle(truncate(title, 250));
            notification.setMessage(truncate(message, 2000));
            notification.setLinkUrl(request.linkUrl());
            if (template != null && template.isEmailEnabled()) {
                notification.setEmailStatus(Notification.DeliveryStatus.PENDING);
            }
            notificationRepository.save(notification);
            if (notification.getEmailStatus() == Notification.DeliveryStatus.PENDING) {
                deliverEmail(notification);
            }
        }
        return recipients.size();
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDtos.NotificationResponse> list(Long userId, boolean unreadOnly, String category, int page, int size) {
        Page<Notification> result;
        if (unreadOnly) {
            result = notificationRepository.findByRecipientIdAndReadAtIsNull(userId, PageRequestFactory.newestFirst(page, size));
        } else if (category != null && !category.isBlank()) {
            result = notificationRepository.findByRecipientIdAndCategory(userId, category, PageRequestFactory.newestFirst(page, size));
        } else {
            result = notificationRepository.findByRecipientId(userId, PageRequestFactory.newestFirst(page, size));
        }
        return PageResponse.of(result, this::map);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCount unread(Long userId) {
        return new NotificationDtos.UnreadCount(notificationRepository.countByRecipientIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(Long userId, Long id) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return map(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    public EmailSender.Result sendDirect(NotificationDtos.EmailRequest request) {
        return emailSender.send(request.to(), request.subject(), request.body());
    }

    static String render(String text, Map<String, String> params) {
        if (text == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String value = params.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(builder, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(builder);
        return builder.toString().replaceAll("\\s{2,}", " ").trim();
    }

    static Set<String> placeholders(String... texts) {
        Set<String> result = new LinkedHashSet<>();
        for (String text : texts) {
            if (text != null) {
                Matcher matcher = PLACEHOLDER.matcher(text);
                while (matcher.find()) {
                    result.add(matcher.group(1));
                }
            }
        }
        return result;
    }

    private void deliverEmail(Notification notification) {
        try {
            UserContact contact = identityClient.contact(notification.getRecipientId());
            if (contact.email() == null) {
                notification.setEmailStatus(Notification.DeliveryStatus.SKIPPED);
                notification.setEmailError("Recipient has disabled email alerts");
                return;
            }
            EmailSender.Result result = emailSender.send(contact.email(), notification.getTitle(),
                    "Dear " + contact.name() + ",\n\n" + notification.getMessage());
            notification.setEmailStatus(switch (result) {
                case SENT -> Notification.DeliveryStatus.SENT;
                case FAILED -> Notification.DeliveryStatus.FAILED;
                case SKIPPED -> Notification.DeliveryStatus.SKIPPED;
            });
            if (result == EmailSender.Result.SKIPPED) {
                notification.setEmailError("Email channel not configured");
            }
        } catch (RuntimeException ex) {
            notification.setEmailStatus(Notification.DeliveryStatus.FAILED);
            notification.setEmailError("Recipient contact unavailable");
        }
    }

    private NotificationDtos.NotificationResponse map(Notification n) {
        return new NotificationDtos.NotificationResponse(n.getId(), n.getCategory(), n.getSeverity(), n.getTitle(), n.getMessage(),
                n.getLinkUrl(), n.getReadAt() != null, n.getReadAt(), n.getCreatedAt());
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
