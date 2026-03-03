package com.alex.bank.cash.dto;

import java.math.BigDecimal;

public record CashResponse(/*boolean success, String message,*/ String transactionId, BigDecimal newBalance) {
}
