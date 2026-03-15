package com.alex.bank.transfer.model;

public enum TransferTransactionStatus {
    WITHDRAW_PENDING,
    DEPOSIT_PENDING,
    FAILED,
    COMPENSATED_FAILED,
    COMPENSATED,
    SUCCESS
}
