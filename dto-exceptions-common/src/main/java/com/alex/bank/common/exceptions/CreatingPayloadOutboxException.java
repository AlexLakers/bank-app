package com.alex.bank.common.exceptions;

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
