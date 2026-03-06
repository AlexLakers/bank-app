package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(@NotBlank
                              String toAccount,

                              @NotBlank
                              String fromAccount,

                              @NotNull
                              @Positive(message = "Сумма должна быть положительная")
                              BigDecimal amount) {
}

