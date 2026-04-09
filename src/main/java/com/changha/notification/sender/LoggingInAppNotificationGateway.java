package com.changha.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

@Component
@Slf4j
public class LoggingInAppNotificationGateway implements NotificationGateway {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification, String deliveryKey) {
        log.info("IN_APP dispatch deliveryKey={} notificationId={} title={}", deliveryKey, notification.getId(), notification.getTitle());
    }
}
