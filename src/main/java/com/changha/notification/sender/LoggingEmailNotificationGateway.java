package com.changha.notification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

@Component
public class LoggingEmailNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationGateway.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, String deliveryKey) {
        log.info("EMAIL dispatch deliveryKey={} notificationId={} title={}", deliveryKey, notification.getId(), notification.getTitle());
    }
}
