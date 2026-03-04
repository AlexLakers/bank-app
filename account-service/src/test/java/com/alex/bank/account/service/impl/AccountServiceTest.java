package com.alex.bank.account.service.impl;


import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.exception.AccountNotFoundException;
import com.alex.bank.account.exception.CreatingPayloadOutboxException;
import com.alex.bank.account.exception.InsufficientFundsException;
import com.alex.bank.account.mapper.AccountMapper;
import com.alex.bank.account.model.Account;
import com.alex.bank.account.model.EventType;
import com.alex.bank.account.model.Outbox;
import com.alex.bank.account.repository.AccountRepository;
import com.alex.bank.account.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Captor
    private ArgumentCaptor<Outbox> outboxCaptor;

    private Account account;
    private AccountDto accountDto;
    private AccountEditDto accountEditDto;
    private final String username = "testuser";
    private final BigDecimal initialBalance = new BigDecimal("1000.00");
    private final LocalDate birthdate = LocalDate.of(1990, 1, 1);

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .username(username)
                .name("Test User")
                .birthdate(birthdate)
                .balance(initialBalance)
                .build();

        accountDto = new AccountDto(
                username,
                "Test User",
                initialBalance,
                birthdate.toString()
        );

        accountEditDto = new AccountEditDto("Updated Name", birthdate);
    }

    // === getAccountByUsername (без изменений) ===
    @Test
    void getAccountByUsername_shouldReturnAccountDto_whenAccountExists() {
        when(accountRepository.findAccountByUsername(username)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        AccountDto result = accountService.getAccountByUsername(username);

        assertThat(result).isEqualTo(accountDto);
        verify(accountRepository).findAccountByUsername(username);
        verify(accountMapper).toDto(account);
    }

    @Test
    void getAccountByUsername_shouldThrowAccountNotFoundException_whenAccountNotExists() {
        when(accountRepository.findAccountByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByUsername(username))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);

        verify(accountRepository).findAccountByUsername(username);
        verifyNoInteractions(accountMapper);
    }

    @Test
    void updateAccount_shouldUpdateAndSaveOutbox_whenAccountExists() throws Exception {
        Account updatedAccount = Account.builder()
                .id(1L)
                .username(username)
                .name(accountEditDto.name())
                .birthdate(accountEditDto.birthdate())
                .balance(initialBalance)
                .build();
        AccountDto updatedDto = new AccountDto(
                username,
                accountEditDto.name(),
                initialBalance,
                accountEditDto.birthdate().toString()
        );

        when(accountRepository.findAccountByUsername(username)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toDto(updatedAccount)).thenReturn(updatedDto);
        when(objectMapper.writeValueAsString(updatedDto)).thenReturn("{}");

        AccountDto result = accountService.updateAccount(accountEditDto, username);

        assertThat(result).isEqualTo(updatedDto);
        verify(accountMapper).updateAccount(accountEditDto, account);
        verify(accountRepository).save(account);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.getSource()).isEqualTo("account-service");
        assertThat(savedOutbox.getEventType()).isEqualTo(EventType.ACCOUNT_UPDATED);
        assertThat(savedOutbox.getMessage()).contains(username);
        assertThat(savedOutbox.getPayload()).isEqualTo("{}");
        assertThat(savedOutbox.getCreatedAt()).isNotNull();
    }

    @Test
    void updateAccount_shouldThrowAccountNotFoundException_whenAccountNotExists() {
        when(accountRepository.findAccountByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount(accountEditDto, username))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);

        verify(accountRepository).findAccountByUsername(username);
        verifyNoMoreInteractions(accountMapper, accountRepository, outboxRepository);
    }

    @Test
    void updateAccount_shouldThrowCreatingPayloadOutboxException_whenJsonProcessingFails() throws Exception {
        when(accountRepository.findAccountByUsername(username)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(accountDto);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> accountService.updateAccount(accountEditDto, username))
                .isInstanceOf(CreatingPayloadOutboxException.class);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void getAccountsExcludeOwner_shouldReturnList_whenAccountsExist() {
        Account otherAccount = Account.builder()
                .id(2L)
                .username("other")
                .name("Other User")
                .birthdate(LocalDate.of(1995, 5, 5))
                .balance(new BigDecimal("500.00"))
                .build();
        AccountDto otherDto = new AccountDto(
                "other",
                "Other User",
                new BigDecimal("500.00"),
                "1995-05-05"
        );

        when(accountRepository.findAccountsByUsernameNot(username)).thenReturn(List.of(otherAccount));
        when(accountMapper.toDto(otherAccount)).thenReturn(otherDto);

        List<AccountDto> result = accountService.getAccountsExcludeOwner(username);

        assertThat(result).hasSize(1).containsExactly(otherDto);
        verify(accountRepository).findAccountsByUsernameNot(username);
        verify(accountMapper).toDto(otherAccount);
    }

    @Test
    void getAccountsExcludeOwner_shouldReturnEmptyList_whenNoOtherAccounts() {
        when(accountRepository.findAccountsByUsernameNot(username)).thenReturn(List.of());

        List<AccountDto> result = accountService.getAccountsExcludeOwner(username);

        assertThat(result).isEmpty();
        verify(accountRepository).findAccountsByUsernameNot(username);
        verifyNoInteractions(accountMapper);
    }

    @Test
    void increaseBalance_shouldIncreaseAndReturnNewBalance() {
        BigDecimal amount = new BigDecimal("200.00");
        BigDecimal newBalance = initialBalance.add(amount);
        when(accountRepository.increaseBalanceByUsername(username, amount))
                .thenReturn(Optional.of(newBalance));

        BigDecimal result = accountService.increaseBalance(username, amount);

        assertThat(result).isEqualTo(newBalance);
        verify(accountRepository).increaseBalanceByUsername(username, amount);
        verify(accountRepository, never()).findAccountByUsername(anyString());
    }

    @Test
    void increaseBalance_shouldThrowAccountNotFoundException_whenAccountNotExists() {
        BigDecimal amount = new BigDecimal("200.00");
        when(accountRepository.increaseBalanceByUsername(username, amount))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.increaseBalance(username, amount))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);

        verify(accountRepository).increaseBalanceByUsername(username, amount);
        verify(accountRepository, never()).findAccountByUsername(anyString());
    }

    @Test
    void decreaseBalance_shouldDecreaseAndReturnNewBalance() {
        BigDecimal amount = new BigDecimal("300.00");
        BigDecimal newBalance = initialBalance.subtract(amount);
        when(accountRepository.decreaseBalanceByUsername(username, amount))
                .thenReturn(Optional.of(newBalance));

        BigDecimal result = accountService.decreaseBalance(username, amount);

        assertThat(result).isEqualTo(newBalance);
        verify(accountRepository).decreaseBalanceByUsername(username, amount);
        verify(accountRepository, never()).existsByUsername(anyString());
        verify(accountRepository, never()).findAccountByUsername(anyString());
    }

    @Test
    void decreaseBalance_shouldThrowInsufficientFundsException_whenBalanceTooLow() {
        BigDecimal amount = new BigDecimal("2000.00");
        when(accountRepository.decreaseBalanceByUsername(username, amount))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> accountService.decreaseBalance(username, amount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining(username);

        verify(accountRepository).decreaseBalanceByUsername(username, amount);
        verify(accountRepository).existsByUsername(username);
        verify(accountRepository, never()).findAccountByUsername(anyString());
    }

    @Test
    void decreaseBalance_shouldThrowAccountNotFoundException_whenAccountNotExists() {
        BigDecimal amount = new BigDecimal("100.00");
        when(accountRepository.decreaseBalanceByUsername(username, amount))
                .thenReturn(Optional.empty());
        when(accountRepository.existsByUsername(username)).thenReturn(false);

        assertThatThrownBy(() -> accountService.decreaseBalance(username, amount))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);

        verify(accountRepository).decreaseBalanceByUsername(username, amount);
        verify(accountRepository).existsByUsername(username);
        verify(accountRepository, never()).findAccountByUsername(anyString());
    }
}