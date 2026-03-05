package com.alex.bank.cash.dto;

import java.math.BigDecimal;

public record CashResponse( String transactionId, BigDecimal newBalance) {
}
