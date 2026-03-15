package com.alex.bank.common.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String login) {
        super("Аккаунт с именем:  %s не найден".formatted(login));
    }

    public AccountNotFoundException(String login, Throwable cause) {
        super("Аккаунт с именем:  %s не найден".formatted(login), cause);
    }

}
