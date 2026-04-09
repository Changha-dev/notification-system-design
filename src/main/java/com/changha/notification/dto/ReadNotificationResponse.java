package com.changha.notification.dto;

import java.time.LocalDateTime;

public record ReadNotificationResponse(
        Long notificationId,
        boolean isRead,
        LocalDateTime readAt
) {
}
