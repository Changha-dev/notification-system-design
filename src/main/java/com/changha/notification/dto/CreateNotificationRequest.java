package com.changha.notification.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;

public record CreateNotificationRequest(
        @NotNull Long recipientId,
        @NotNull NotificationType notificationType,
        @NotNull NotificationChannel channel,
        @NotNull Long referenceId,
        LocalDateTime scheduleAt
) {
}
