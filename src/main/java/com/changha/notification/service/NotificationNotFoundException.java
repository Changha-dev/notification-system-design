package com.changha.notification.service;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long notificationId, Long recipientId) {
        super("Notification not found. notificationId=%d recipientId=%d".formatted(notificationId, recipientId));
    }
}
