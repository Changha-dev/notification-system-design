package com.changha.notification.sender;

public abstract class NotificationDispatchException extends RuntimeException {

    private final String failureCode;

    protected NotificationDispatchException(String failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
