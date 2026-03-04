package com.alex.bank.cash.service;

import com.alex.bank.cash.client.account.AccountServiceClient;
import com.alex.bank.cash.dto.CashRequest;
import com.alex.bank.cash.dto.CashResponse;
import com.alex.bank.cash.exception.*;
import com.alex.bank.cash.model.*;
import com.alex.bank.cash.repository.CashTransactionRepository;
import com.alex.bank.cash.repository.OutboxRepository;
import com.alex.bank.cash.service.impl.CashServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private CashTransactionRepository cashTransactionRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CashServiceImpl cashService;

    @Captor
    private ArgumentCaptor<CashTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Outbox> outboxCaptor;

    private final String username = "testUser";
    private final BigDecimal amount = new BigDecimal("100.00");
    private final BigDecimal newBalance = new BigDecimal("500.00");
    private CashTransaction pendingTransaction;

    @BeforeEach
    void setUp() {
        pendingTransaction = CashTransaction.builder()
                .transactionId(UUID.randomUUID())
                .action(CashAction.GET)
                .accountHolder(username)
                .status(CashTransactionStatus.PENDING)
                .amount(amount)
                .build();
    }

    @Test
    void processCash_Success_Deposit() throws JsonProcessingException {
        CashRequest request = new CashRequest(CashAction.PUT, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class)))
                .thenReturn(pendingTransaction); // первое сохранение (PENDING)
        when(accountServiceClient.depositCash(username, amount)).thenReturn(newBalance);
        when(objectMapper.writeValueAsString(any(CashResponse.class))).thenReturn("{\"balance\":500.00}");


        CashResponse response = cashService.processCash(username, request);


        assertThat(response.newBalance()).isEqualByComparingTo(newBalance);
        assertThat(response.transactionId()).isEqualTo(pendingTransaction.getTransactionId().toString());


        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        CashTransaction savedPending = transactionCaptor.getAllValues().get(0);
        assertThat(savedPending.getStatus()).isEqualTo(CashTransactionStatus.PENDING);
        assertThat(savedPending.getAction()).isEqualTo(CashAction.PUT);
        assertThat(savedPending.getAccountHolder()).isEqualTo(username);
        assertThat(savedPending.getAmount()).isEqualByComparingTo(amount);

        CashTransaction savedSuccess = transactionCaptor.getAllValues().get(1);
        assertThat(savedSuccess.getStatus()).isEqualTo(CashTransactionStatus.SUCCESS);
        assertThat(savedSuccess.getMessage()).isNull();


        verify(outboxRepository).save(outboxCaptor.capture());
        Outbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getSource()).isEqualTo("cash-service");
        assertThat(outbox.getEventType()).isEqualTo(EventType.CASH_DEPOSITED);
        assertThat(outbox.getPayload()).isEqualTo("{\"balance\":500.00}");
        assertThat(outbox.getMessage()).contains("PUT", amount.toString(), newBalance.toString());
    }

    @Test
    void processCash_Success_Withdrawal() throws JsonProcessingException {

        CashRequest request = new CashRequest(CashAction.GET, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class)))
                .thenReturn(pendingTransaction);
        when(accountServiceClient.withdrawCash(username, amount)).thenReturn(newBalance);
        when(objectMapper.writeValueAsString(any(CashResponse.class))).thenReturn("{\"balance\":500.00}");


        CashResponse response = cashService.processCash(username, request);


        assertThat(response.newBalance()).isEqualByComparingTo(newBalance);

        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        CashTransaction savedPending = transactionCaptor.getAllValues().get(0);
        assertThat(savedPending.getAction()).isEqualTo(CashAction.GET);


        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.CASH_WITHDRAWAL);
    }

    @Test
    void processCash_AccountNotFoundException() {

        CashRequest request = new CashRequest(CashAction.GET, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.withdrawCash(username, amount))
                .thenThrow(new AccountNotFoundException("Account not found"));


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found");


        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        CashTransaction failedTransaction = transactionCaptor.getAllValues().get(1);
        assertThat(failedTransaction.getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        assertThat(failedTransaction.getMessage()).isEqualTo("Account not found");


        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_InsufficientFundsException() {

        CashRequest request = new CashRequest(CashAction.GET, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.withdrawCash(username, amount))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_AccountValidationException() {

        CashRequest request = new CashRequest(CashAction.PUT, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.depositCash(username, amount))
                .thenThrow(new AccountValidationException("Invalid amount"));


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(AccountValidationException.class);

        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_ExternalServiceException() {

        CashRequest request = new CashRequest(CashAction.GET, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.withdrawCash(username, amount))
                .thenThrow(new ExternalServiceException("Service unavailable", null));


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(ExternalServiceException.class);

        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_UnexpectedException() {

        CashRequest request = new CashRequest(CashAction.PUT, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.depositCash(username, amount))
                .thenThrow(new NullPointerException("Unexpected NPE"));


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Внутренняя ошибка");


        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        CashTransaction failedTransaction = transactionCaptor.getAllValues().get(1);
        assertThat(failedTransaction.getStatus()).isEqualTo(CashTransactionStatus.FAILED);
        assertThat(failedTransaction.getMessage()).isEqualTo("Внутренняя ошибка сервиса");

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_OutboxJsonProcessingException() throws JsonProcessingException {

        CashRequest request = new CashRequest(CashAction.GET, amount);
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(pendingTransaction);
        when(accountServiceClient.withdrawCash(username, amount)).thenReturn(newBalance);
        when(objectMapper.writeValueAsString(any(CashResponse.class)))
                .thenThrow(new JsonProcessingException("Serialization error") {});


        assertThatThrownBy(() -> cashService.processCash(username, request))
                .isInstanceOf(CreatingPayloadOutboxException.class);


        verify(cashTransactionRepository, times(2)).save(transactionCaptor.capture());
        CashTransaction successTransaction = transactionCaptor.getAllValues().get(1);
        assertThat(successTransaction.getStatus()).isEqualTo(CashTransactionStatus.SUCCESS);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processCash_FirstSaveReturnsTransactionWithId() throws JsonProcessingException {
        CashRequest request = new CashRequest(CashAction.PUT, amount);
        CashTransaction savedWithId = CashTransaction.builder()
                .transactionId(UUID.randomUUID())
                .action(CashAction.PUT)
                .accountHolder(username)
                .status(CashTransactionStatus.PENDING)
                .amount(amount)
                .build();
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenReturn(savedWithId);
        when(accountServiceClient.depositCash(username, amount)).thenReturn(newBalance);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        CashResponse response = cashService.processCash(username, request);

        assertThat(response.transactionId()).isEqualTo(savedWithId.getTransactionId().toString());
    }
}