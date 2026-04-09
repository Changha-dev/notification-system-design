package com.changha.notification.sender;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;

public interface NotificationGateway {

    NotificationChannel channel();

    void send(Notification notification, String deliveryKey);
}
