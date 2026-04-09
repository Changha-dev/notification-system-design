package com.changha.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.dto.NotificationAcceptedResponse;
import com.changha.notification.dto.NotificationDetailResponse;
import com.changha.notification.dto.NotificationListResponse;
import com.changha.notification.dto.ReadFilter;
import com.changha.notification.dto.ReadNotificationResponse;
import com.changha.notification.service.NotificationApplicationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @PostMapping
    public ResponseEntity<NotificationAcceptedResponse> createNotification(@Validated @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.accepted().body(notificationApplicationService.accept(request));
    }

    @GetMapping("/{notificationId}")
    public NotificationDetailResponse getNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return notificationApplicationService.getDetail(notificationId, userId);
    }

    @GetMapping
    public NotificationListResponse getNotifications(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "ALL") ReadFilter read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationApplicationService.getList(userId, read, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    public ReadNotificationResponse markRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return notificationApplicationService.markRead(notificationId, userId);
    }
}
