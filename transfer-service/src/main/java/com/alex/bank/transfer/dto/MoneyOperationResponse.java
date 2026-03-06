package com.alex.bank.transfer.dto;

import java.math.BigDecimal;

public record MoneyOperationResponse(BigDecimal newBalance) {
}
