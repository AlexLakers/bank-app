package com.alex.bank.account.service.impl;

import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.dto.MoneyOperationRequest;
import com.alex.bank.account.dto.MoneyOperationResponse;
import com.alex.bank.account.exception.AccountNotFoundException;
import com.alex.bank.account.exception.InsufficientFundsException;
import com.alex.bank.account.mapper.AccountMapper;
import com.alex.bank.account.model.Account;
import com.alex.bank.account.repository.AccountRepository;
import com.alex.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountDto getAccountByUsername(String username) {
        return accountRepository.findAccountByUsername(username)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }

    @Override
    public AccountDto updateAccount(AccountEditDto accountEditDto, String username) {
        return accountRepository.findAccountByUsername(username)
                .map(account -> {
                    accountMapper.updateAccount(accountEditDto, account);
                    return account;
                })
                .map(accountRepository::save)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }

    @Override
    public List<AccountDto> getAccountsExcludeOwner(String username) {
        return accountRepository.findAccountsByUsernameNot(username)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

}
