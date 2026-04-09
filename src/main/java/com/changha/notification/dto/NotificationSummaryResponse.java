package com.changha.notification.dto;

import java.time.LocalDateTime;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.domain.NotificationType;

public record NotificationSummaryResponse(
        Long notificationId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
