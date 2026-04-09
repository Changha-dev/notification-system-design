package com.changha.notification.event;

import java.time.LocalDateTime;

public record NotificationScheduleCreatedEvent(
        Long scheduleId,
        LocalDateTime scheduleAt
) {
}
