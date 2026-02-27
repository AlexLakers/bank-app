package com.alex.bank.account.service.impl;

import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.dto.MoneyOperationRequest;
import com.alex.bank.account.dto.MoneyOperationResponse;
import com.alex.bank.account.exception.AccountNotFoundException;
import com.alex.bank.account.exception.CreatingPayloadOutboxException;
import com.alex.bank.account.exception.InsufficientFundsException;
import com.alex.bank.account.mapper.AccountMapper;
import com.alex.bank.account.model.Account;
import com.alex.bank.account.model.EventType;
import com.alex.bank.account.model.Outbox;
import com.alex.bank.account.repository.AccountRepository;
import com.alex.bank.account.repository.OutboxRepository;
import com.alex.bank.account.service.AccountService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public AccountDto getAccountByUsername(String username) {
        return accountRepository.findAccountByUsername(username)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }

    @Override
    @Transactional
    public AccountDto updateAccount(AccountEditDto accountEditDto, String username) {
        AccountDto dto = accountRepository.findAccountByUsername(username)
                .map(account -> {
                    accountMapper.updateAccount(accountEditDto, account);
                    return account;
                })
                .map(accountRepository::save)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new AccountNotFoundException(username));

        saveOutbox(dto);

        return dto;
    }

    private void saveOutbox(AccountDto payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            Outbox outbox = Outbox.builder()
                    .source("account-service")
                    .eventType(EventType.ACCOUNT_UPDATED)
                    .payload(payloadJson)
                    .message("Данные пользователя %s были обновлены".formatted(payload.username()))
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new CreatingPayloadOutboxException();
        }
    }

    @Override
    public List<AccountDto> getAccountsExcludeOwner(String username) {
        return accountRepository.findAccountsByUsernameNot(username)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BigDecimal increaseBalance(String username, BigDecimal amount) {
        if (accountRepository.increaseBalanceByUsername(username, amount) > 0)
            return getCurrentBalance(username);
        throw new AccountNotFoundException(username);

    }

    @Override
    @Transactional
    public BigDecimal decreaseBalance(String username, BigDecimal amount) {
        if (accountRepository.decreaseBalanceByUsername(username, amount) > 0)
            return getCurrentBalance(username);
        if (accountRepository.existsByUsername(username)) throw new InsufficientFundsException(amount, username);
        throw new AccountNotFoundException(username);
    }

    private BigDecimal getCurrentBalance(String username) {
        return accountRepository.findAccountByUsername(username)
                .map(Account::getBalance)
                .orElseThrow(() -> new AccountNotFoundException(username));

    }
}
