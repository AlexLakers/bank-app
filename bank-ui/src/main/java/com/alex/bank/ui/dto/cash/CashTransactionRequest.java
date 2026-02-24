package com.alex.bank.ui.dto.cash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashTransactionRequest(
        @NotNull
        CashAction action,

        @NotNull
        @Positive(message = "Сумма должна быть положительная")
        BigDecimal amount) {
}
