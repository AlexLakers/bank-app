package com.alex.bank.account.dto;

import java.time.LocalDate;

public record AccountEditDto(String name, LocalDate birthdate) {
}
