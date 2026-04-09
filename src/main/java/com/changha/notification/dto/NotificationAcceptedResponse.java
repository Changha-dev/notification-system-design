package com.changha.notification.dto;

public record NotificationAcceptedResponse(
        Long notificationId,
        Long scheduleId,
        String status,
        boolean accepted,
        String message
) {
}
