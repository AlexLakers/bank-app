package com.alex.bank.cash.exception;

public class CreatingPayloadOutboxException extends RuntimeException {
    public CreatingPayloadOutboxException(String message) {
        super(message);
    }

    public CreatingPayloadOutboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreatingPayloadOutboxException() {
        super("Ошибка сохранения данных в outbox");
    }
}
