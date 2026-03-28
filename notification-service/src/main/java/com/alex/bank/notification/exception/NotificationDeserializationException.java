package com.alex.bank.notification.exception;

public class NotificationDeserializationException extends RuntimeException {
    public NotificationDeserializationException(String message) {
        super(message);
    }
    public NotificationDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
