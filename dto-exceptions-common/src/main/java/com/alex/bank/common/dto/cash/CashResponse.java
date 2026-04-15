package com.alex.bank.common.dto.cash;

import java.math.BigDecimal;

public record CashResponse( String transactionId, BigDecimal newBalance, String username) {
}
