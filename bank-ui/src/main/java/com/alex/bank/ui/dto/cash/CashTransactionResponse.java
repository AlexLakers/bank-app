package com.alex.bank.ui.dto.cash;

import com.alex.bank.ui.dto.CashTransactionStatus;

public record CashTransactionResponse(boolean success, String message,String transactionId) {
}
