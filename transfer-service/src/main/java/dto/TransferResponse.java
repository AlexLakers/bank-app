package dto;

import java.math.BigDecimal;

public record TransferResponse(String transactionId, BigDecimal newBalanceSender,BigDecimal newBalanceReceiver) {
}
