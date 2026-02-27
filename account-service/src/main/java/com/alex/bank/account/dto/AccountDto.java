package com.alex.bank.account.dto;

import java.math.BigDecimal;

public record AccountDto(String username, String name, BigDecimal balance, String birthdate) {

}
