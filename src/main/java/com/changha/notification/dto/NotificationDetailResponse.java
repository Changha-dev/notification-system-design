package com.changha.notification.dto;

import java.time.LocalDateTime;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.domain.NotificationType;

public record NotificationDetailResponse(
        Long notificationId,
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        Long referenceId,
        NotificationStatus status,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime sentAt,
        String lastFailureCode,
        String lastFailureMessage,
        LocalDateTime createdAt
) {
}
