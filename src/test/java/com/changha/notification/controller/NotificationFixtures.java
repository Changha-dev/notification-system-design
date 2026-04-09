package com.changha.notification.controller;

import java.time.LocalDateTime;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.dto.CreateNotificationRequest;

public final class NotificationFixtures {

    private NotificationFixtures() {
    }

    public static CreateNotificationRequest createImmediateEmailRequest() {
        return new CreateNotificationRequest(
                1001L,
                NotificationType.PAYMENT_CONFIRMED,
                NotificationChannel.EMAIL,
                5001L,
                null
        );
    }

    public static CreateNotificationRequest createScheduledEmailRequest(LocalDateTime scheduleAt) {
        return new CreateNotificationRequest(
                1001L,
                NotificationType.COURSE_START_D_MINUS_1,
                NotificationChannel.EMAIL,
                7001L,
                scheduleAt
        );
    }
}
