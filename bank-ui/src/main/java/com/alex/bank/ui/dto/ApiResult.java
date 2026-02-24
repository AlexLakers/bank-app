package com.alex.bank.ui.dto;

/*public record AccountResult(AccountDto payload, String error, String info) {
    public static AccountResult success(AccountDto payload, String info) {
        return new AccountResult(payload, null, info);
    }

    public static AccountResult error(String error) {
        return new AccountResult(null, error, null);
    }
}*/
public record ApiResult<T>(T payload, String error, String info) {
    public static <T> ApiResult<T> success(T payload, String info) {
        return new ApiResult<>(payload, null, info);
    }

    public static <T> ApiResult<T> error(String error) {
        return new ApiResult<>(null, error, null);
    }
    public boolean isSuccess() { return error == null; }
}
