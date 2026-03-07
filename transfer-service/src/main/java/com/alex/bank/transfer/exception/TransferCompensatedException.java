package com.alex.bank.transfer.exception;

public class TransferCompensatedException extends RuntimeException{
    public TransferCompensatedException(String message) {
        super(message);
    }
    public TransferCompensatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
