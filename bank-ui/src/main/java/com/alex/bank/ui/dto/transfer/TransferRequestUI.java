package com.alex.bank.ui.dto.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequestUI(@NotBlank
                                String toAccount,

                                @NotNull
                                @Positive(message = "Сумма должна быть положительная")
                                BigDecimal amount) {
}
