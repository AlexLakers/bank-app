package com.alex.bank.cash.service;

import com.alex.bank.cash.dto.CashRequest;
import com.alex.bank.cash.dto.CashResponse;

public interface CashService {
   CashResponse processCash(String username, CashRequest request);


}
