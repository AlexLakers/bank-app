package com.alex.bank.common.dto.transfer;

import java.math.BigDecimal;

public record TransferResponse(String transactionId, BigDecimal newBalanceSender,BigDecimal newBalanceReceiver) {
}
