package com.alex.bank.account.service;

import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.dto.MoneyOperationRequest;
import com.alex.bank.account.dto.MoneyOperationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto getAccountByUsername(String username);

    AccountDto updateAccount(AccountEditDto accountEditDto, String username);

    List<AccountDto> getAccountsExcludeOwner(String username);

    BigDecimal increaseBalance(String username, BigDecimal amount);

    BigDecimal decreaseBalance(String username, BigDecimal amount);


}
