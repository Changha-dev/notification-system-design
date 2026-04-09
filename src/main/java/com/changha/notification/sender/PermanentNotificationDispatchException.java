package com.changha.notification.sender;

public class PermanentNotificationDispatchException extends NotificationDispatchException {

    public PermanentNotificationDispatchException(String failureCode, String message) {
        super(failureCode, message);
    }
}
