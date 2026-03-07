package com.alex.bank.transfer.dto;

import java.math.BigDecimal;

public record TransferResponse(String transactionId, BigDecimal newBalanceSender,BigDecimal newBalanceReceiver) {
}
