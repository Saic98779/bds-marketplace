package com.sidbi.bds.common.client.dto;

import java.util.List;
import java.util.Map;

public record NotificationRequest(
        List<Long> recipientIds,
        String recipientPermission,
        String templateCode,
        Map<String, String> params,
        String category,
        String linkUrl
) {
    public static NotificationRequest to(Long recipientId, String templateCode, String category, String linkUrl, Map<String, String> params) {
        return new NotificationRequest(List.of(recipientId), null, templateCode, params, category, linkUrl);
    }

    public static NotificationRequest to(List<Long> recipientIds, String templateCode, String category, String linkUrl, Map<String, String> params) {
        return new NotificationRequest(recipientIds, null, templateCode, params, category, linkUrl);
    }

    public static NotificationRequest toAdmins(String permission, String templateCode, String category, String linkUrl, Map<String, String> params) {
        return new NotificationRequest(List.of(), permission, templateCode, params, category, linkUrl);
    }
}
