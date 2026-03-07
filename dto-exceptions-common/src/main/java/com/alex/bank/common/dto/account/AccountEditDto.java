package com.alex.bank.common.dto.account;

import java.time.LocalDate;

public record AccountEditDto(String name, LocalDate birthdate) {
}
