package com.changha.notification.sender;

import org.springframework.stereotype.Component;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

@Component
public class EmailNotificationSender implements NotificationSender {

    private final NotificationGateway gateway;

    public EmailNotificationSender(LoggingEmailNotificationGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, String deliveryKey) {
        gateway.send(notification, deliveryKey);
    }
}
