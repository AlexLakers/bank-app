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

    private TransferTransaction copyOf(TransferTransaction original) {
        return TransferTransaction.builder()
                .transactionId(original.getTransactionId())
                .fromAccount(original.getFromAccount())
                .toAccount(original.getToAccount())
                .amount(original.getAmount())
                .status(original.getStatus())
                .message(original.getMessage())
                .build();
    }

    @Test
    void transfer_shouldWithdrowalAndDeposit_success() throws JsonProcessingException {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        TransferTransaction pendingTransaction = TransferTransaction.builder()
                .transactionId(transactionId)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransferTransactionStatus.WITHDRAW_PENDING)
                .build();
        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenReturn(pendingTransaction)
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));

        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount))).thenReturn(receiverNewBalance);
        when(objectMapper.writeValueAsString(any(TransferResponse.class)))
                .thenReturn("{\"transactionId\":\"" + transactionId + "\",\"newBalanceSender\":900.00,\"newBalanceReceiver\":1100.00}");

        TransferResponse response = transferService.transfer(request);

        assertThat(response.transactionId()).isEqualTo(transactionId.toString());
        assertThat(response).isNotNull();
        verify(accountServiceClient).deposit(eq(toAccount), eq(amount));
    }

    @Test
    void transfer_shouldThrowsAccountNotFoundException_whenWithdrawFails_failed() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
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
    void transfer_shouldThrowsInsufficientFundsException_whenWithdrawFails_failes() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowsAccountValidationException_whenWithdrawFails_failed() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new AccountValidationException("Invalid account"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountValidationException.class);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.FAILED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowsExternalServiceException_whenWithdrawFailsAndStatusIsWithdrawPending_failed() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount)))
                .thenThrow(new ExternalServiceException("Service unavailable", null));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Service unavailable"); // исправлено сообщение

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(1).getStatus()).isEqualTo(TransferTransactionStatus.WITHDRAW_PENDING);
        assertThat(transactionCaptor.getAllValues().get(1).getMessage()).isEqualTo("Service unavailable");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowsTransferCompensatedException_whenDepositFailsAndCompensationSucceed_failed() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount)))
                .thenThrow(new RuntimeException("Deposit failed"));
        when(accountServiceClient.deposit(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance); // компенсация

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(TransferCompensatedException.class)
                .hasMessageContaining("Перевод не выполнен, средства возвращены");

        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues().get(2).getStatus()).isEqualTo(TransferTransactionStatus.COMPENSATED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowsCompensationFailedException_whenDepositFailsAndCompensationFails_failed() {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));
        when(accountServiceClient.withdraw(eq(fromAccount), eq(amount))).thenReturn(senderNewBalance);
        when(accountServiceClient.deposit(eq(toAccount), eq(amount)))
                .thenThrow(new RuntimeException("Deposit failed"));
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
    void transfer_shouldWithdrowalAndDeposit_success_WhenOutboxSerializationFails() throws JsonProcessingException {
        TransferRequest request = new TransferRequest(toAccount, fromAccount, amount);

        TransferTransaction pendingTransaction = TransferTransaction.builder()
                .transactionId(transactionId)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransferTransactionStatus.WITHDRAW_PENDING)
                .build();

        when(transactionRepository.save(any(TransferTransaction.class)))
                .thenReturn(pendingTransaction)
                .thenAnswer(invocation -> copyOf(invocation.getArgument(0)));

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