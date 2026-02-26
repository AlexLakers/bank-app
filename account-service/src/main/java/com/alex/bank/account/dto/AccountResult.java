package com.alex.bank.account.dto;

/*public record AccountResult(AccountDto payload, String error, String info) {

    public static AccountResult success(AccountDto payload, String info) {
        return new AccountResult(payload, null, info);
    }

    public static AccountResult error(String error) {
        return new AccountResult(null, error, null);
    }
}*/
public record AccountResult<T>(T payload, String error, String info) {
    public static <T> AccountResult<T> success(T payload, String info) {
        return new AccountResult<>(payload, null, info);
    }

    public static <T> AccountResult<T> error(String error) {
        return new AccountResult<>(null, error, null);
    }
    public boolean isSuccess() { return error == null; }
}
