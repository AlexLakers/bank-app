package com.alex.bank.common.exceptions;

public class AccountValidationException extends RuntimeException {
    public AccountValidationException(String message) {
        super(message);
    }
    public AccountValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
