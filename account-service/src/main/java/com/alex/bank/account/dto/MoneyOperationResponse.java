package com.alex.bank.account.dto;

import java.math.BigDecimal;

public record MoneyOperationResponse(boolean success,BigDecimal newBalance) {
}
