package com.alex.bank.cash.dto;

import com.alex.bank.cash.model.CashAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashRequest(
        @NotNull
        CashAction action,

        @NotNull
        @Positive(message = "Сумма должна быть положительная")
        BigDecimal amount) {
}
