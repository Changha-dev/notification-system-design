package com.changha.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final LoggingEmailNotificationGateway gateway;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, String deliveryKey) {
        gateway.send(notification, deliveryKey);
    }
}
