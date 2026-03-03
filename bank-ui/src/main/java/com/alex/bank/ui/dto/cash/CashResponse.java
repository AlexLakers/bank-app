package com.alex.bank.ui.dto.cash;

public record CashResponse(boolean success, String message, String transactionId) {
}
