package com.alex.bank.transfer.service;


import com.alex.bank.transfer.dto.TransferRequest;
import com.alex.bank.transfer.dto.TransferResponse;

public interface TransferService {

    TransferResponse transfer(TransferRequest transferRequest);
}
