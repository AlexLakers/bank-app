package com.alex.bank.common.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal amount, String username) {
        super("На счету: %1s не хватает средств для снятия суммы: %2s".formatted(username, amount));
    }

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(BigDecimal amount, Throwable cause) {
        super("На счету не хватает средств для снятия суммы: %s".formatted(amount), cause);
    }
}
