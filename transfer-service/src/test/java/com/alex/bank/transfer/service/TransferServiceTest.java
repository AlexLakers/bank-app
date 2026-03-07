package com.alex.bank.transfer.service;

import com.alex.bank.transfer.client.account.AccountServiceClient;
import com.alex.bank.transfer.dto.TransferRequest;
import com.alex.bank.transfer.dto.TransferResponse;
import com.alex.bank.transfer.exception.*;
import com.alex.bank.transfer.model.EventType;
import com.alex.bank.transfer.model.Outbox;
import com.alex.bank.transfer.model.TransferTransaction;
import com.alex.bank.transfer.model.TransferTransactionStatus;
import com.alex.bank.transfer.repository.OutboxRepository;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import com.alex.bank.transfer.service.impl.TransferServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferTransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Captor
    private ArgumentCaptor<TransferTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Outbox> outboxCaptor;

    private final String fromAccount = "alexeev";
    private final String toAccount = "sergeev";
    private final BigDecimal amount = new BigDecimal("100.00");
    private final BigDecimal senderNewBalance = new BigDecimal("900.00");
    private final BigDecimal receiverNewBalance = new BigDecimal("1100.00");
    private final UUID transactionId = UUID.randomUUID();

    @Test
    void transfer_success() throws JsonProcessingException {

        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        TransferTransaction pendingTransaction = TransferTransaction.builder()
                .transactionId(transactionId)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransferTransactionStatus.WITHDRAW_PENDING)
                .build();

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenReturn(pendingTransaction);
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount))).thenReturn(receiverNewBalance);
        when(objectMapper.writeValueAsString(any(TransferResponse.class)))
                .thenReturn("{\"transactionId\":\"" + transactionId + "\",\"newBalanceSender\":900.00,\"newBalanceReceiver\":1100.00}");

        TransferResponse response = transferService.transfer(request);

        assertThat(response.transactionId()).isEqualTo(transactionId.toString());
        assertThat(response.newBalanceSender()).isEqualByComparingTo(senderNewBalance);
        assertThat(response.newBalanceReceiver()).isEqualByComparingTo(receiverNewBalance);

        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(0).getStatus()).isEqualTo(TransferTransactionStatus.WITHDRAW_PENDING);
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.DEPOSIT_PENDING);
        assertThat(transactionCaptor.getAllValues().get(2).getStatus()).isEqualTo(TransferTransactionStatus.SUCCESS);

        verify(outboxRepository).save(outboxCaptor.capture());
        Outbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getSource()).isEqualTo("transfer-service");
        assertThat(outbox.getEventType()).isEqualTo(EventType.TRANSFER_PERFORMED);
        assertThat(outbox.getMessage()).contains("alexeev выполнил перевод sergeev на сумму 100.00, новый баланс отправителя 900.00, новый баланс получателя 1100.00");
    }

    @Test
    void transfer_throwsAccountNotFoundException_whenWithdrawFails() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new AccountNotFoundException("Account not found"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.FAILED);
        assertThat(transactionCaptor.getAllValues().get(1).getMessage()).isEqualTo("Account not found");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_throwsInsufficientFundsException_whenWithdrawFails() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_throwsAccountValidationException_whenWithdrawFails() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new AccountValidationException("Invalid account"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountValidationException.class);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_throwsExternalServiceException_whenWithdrawFailsAndStatusIsWithdrawPending() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new ExternalServiceException("Service unavailable", null));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Сервис аккаунтов временно недоступен");

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.WITHDRAW_PENDING);
        assertThat(transactionCaptor.getAllValues().get(1).getMessage()).isEqualTo("Service unavailable");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_throwsTransferCompensatedException_whenDepositFailsAndCompensationSucceeds() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount)))
                .thenThrow(new RuntimeException("Deposit failed"));
        // Компенсация: возврат средств отправителю
        when(accountServiceClient.deposit(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(TransferCompensatedException.class)
                .hasMessageContaining("Перевод не выполнен, средства возвращены");

        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(2).getStatus()).isEqualTo(TransferTransactionStatus.COMPENSATED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_throwsCompensationFailedException_whenDepositFailsAndCompensationFails() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> {
                    TransferTransaction tx = invocation.getArgument(0);
                    tx.setTransactionId(transactionId);
                    return tx;
                });
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount)))
                .thenThrow(new RuntimeException("Deposit failed"));
        // Компенсация тоже падает
        when(accountServiceClient.deposit(eq(fromAccount), eq(amount)))
                .thenThrow(new RuntimeException("Compensation failed"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CompensationFailedException.class)
                .hasMessageContaining("КРИТИЧЕСКАЯ ОШИБКА: деньги списаны, но не удалось вернуть");

        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(2).getStatus()).isEqualTo(TransferTransactionStatus.COMPENSATED_FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_successEvenWhenOutboxSerializationFails() throws JsonProcessingException {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        TransferTransaction pendingTransaction = TransferTransaction.builder()
                .transactionId(transactionId)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransferTransactionStatus.WITHDRAW_PENDING)
                .build();

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenReturn(pendingTransaction);
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount))).thenReturn(receiverNewBalance);
        when(objectMapper.writeValueAsString(any(TransferResponse.class)))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        TransferResponse response = transferService.transfer(request);

        assertThat(response.transactionId()).isEqualTo(transactionId.toString());
        assertThat(response.newBalanceSender()).isEqualByComparingTo(senderNewBalance);
        assertThat(response.newBalanceReceiver()).isEqualByComparingTo(receiverNewBalance);

        verify(transactionRepository, times(3)).save(any());
        verify(outboxRepository, never()).save(any());
    }
}