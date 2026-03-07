package com.alex.bank.common.dto.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MoneyOperationRequest(@NotNull
                                    @Positive(message = "Сумма должна быть положительная")
                                    BigDecimal amount) {
}
