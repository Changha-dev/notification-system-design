package com.changha.notification.sender;

public class RetryableNotificationDispatchException extends NotificationDispatchException {

    public RetryableNotificationDispatchException(String failureCode, String message) {
        super(failureCode, message);
    }
}
