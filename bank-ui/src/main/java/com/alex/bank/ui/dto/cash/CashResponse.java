package com.alex.bank.ui.dto.cash;

import java.math.BigDecimal;

public record CashResponse( String transactionId, BigDecimal newBalance) {
}
