package com.alex.bank.transfer.service;



import com.alex.bank.common.dto.transfer.*;
import com.alex.bank.transfer.*;

public interface TransferService {

    TransferResponse transfer(TransferRequest transferRequest);
}
