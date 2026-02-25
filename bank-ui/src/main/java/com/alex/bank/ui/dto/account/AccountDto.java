package com.alex.bank.ui.dto.account;

import java.math.BigDecimal;

public record AccountDto( String username, String name, BigDecimal balance, String birthdate) {

}
