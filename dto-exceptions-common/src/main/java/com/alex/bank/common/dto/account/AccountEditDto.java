package com.alex.bank.common.dto.account;

import com.alex.bank.common.validation.ValidAge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AccountEditDto(@NotBlank String name,
                             @NotNull @ValidAge LocalDate birthdate) {
}
