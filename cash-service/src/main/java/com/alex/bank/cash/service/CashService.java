package com.alex.bank.cash.service;
import com.alex.bank.common.dto.cash.*;

public interface CashService {
   CashResponse processCash(String username, CashRequest request);


}
