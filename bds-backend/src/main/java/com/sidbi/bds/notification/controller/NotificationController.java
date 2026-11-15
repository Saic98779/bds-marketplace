package com.sidbi.bds.notification.controller;

import com.sidbi.bds.common.api.ApiResponse;
import com.sidbi.bds.common.api.PageResponse;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.notification.dto.NotificationDtos;
import com.sidbi.bds.notification.service.NotificationService;
import com.sidbi.bds.notification.service.PlatformAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-platform notifications for the signed-in user")
public class NotificationController {

    private final NotificationService notificationService;
    private final PlatformAdminService platformService;

    @GetMapping("/notifications")
    @Operation(summary = "List notifications")
    public ApiResponse<PageResponse<NotificationDtos.NotificationResponse>> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                                 @RequestParam(required = false) String category,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.list(CurrentUser.id(), unreadOnly, category, page, size));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Unread notification count")
    public ApiResponse<NotificationDtos.UnreadCount> unread() {
        return ApiResponse.ok(notificationService.unread(CurrentUser.id()));
    }

    @PostMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<NotificationDtos.NotificationResponse> read(@PathVariable Long notificationId) {
        return ApiResponse.ok(notificationService.markRead(CurrentUser.id(), notificationId));
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<Map<String, Integer>> readAll() {
        return ApiResponse.ok("All notifications marked as read", Map.of("updated", notificationService.markAllRead(CurrentUser.id())));
    }

    @GetMapping("/public/settings")
    @Operation(summary = "Public platform settings and CMS content")
    public ApiResponse<Map<String, String>> publicSettings() {
        return ApiResponse.ok(platformService.publicSettings());
    }
}
