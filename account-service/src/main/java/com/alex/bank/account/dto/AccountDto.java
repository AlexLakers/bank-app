package com.alex.bank.account.dto;

import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@FieldNameConstants
public record AccountDto(String username, String name, BigDecimal balance, String birthdate) {

}
