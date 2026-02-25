package com.alex.bank.ui.dto.account;

import com.alex.bank.ui.validation.ValidAge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record AccountEditDto(@NotBlank(message = "Имя не может быть пустым")
                             String name,

                             @Past
                             @ValidAge(message = "Возраст должен быть более 18 лет")
                             LocalDate birthdate) {
}
