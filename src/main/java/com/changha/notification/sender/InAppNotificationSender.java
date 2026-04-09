package com.changha.notification.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final LoggingInAppNotificationGateway gateway;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification, String deliveryKey) {
        gateway.send(notification, deliveryKey);
    }
}
