package com.alex.bank.transfer.service.impl;

import com.alex.bank.transfer.client.account.AccountServiceClient;
import com.alex.bank.common.dto.transfer.*;
import com.alex.bank.transfer.exception.*;
import com.alex.bank.common.dto.notification.EventType;
import com.alex.bank.transfer.model.Outbox;
import com.alex.bank.transfer.model.TransferTransaction;
import com.alex.bank.transfer.model.TransferTransactionStatus;
import com.alex.bank.transfer.repository.OutboxRepository;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import com.alex.bank.transfer.service.TransferService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alex.bank.common.exceptions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferTransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    @Transactional(noRollbackFor = {
            AccountNotFoundException.class,
            InsufficientFundsException.class,
            AccountValidationException.class,
            ExternalServiceException.class,
            TransferCompensatedException.class,
            CompensationFailedException.class
    })
    public TransferResponse transfer(TransferRequest request) {
        TransferTransaction transaction = createTransaction(request);

        try {
            BigDecimal newBalanceSender = withdraw(request, transaction);
            TransferResponse response = deposit(request, transaction, newBalanceSender);
            saveOutbox(response, transaction);
            return response;
        } catch (AccountNotFoundException | InsufficientFundsException | AccountValidationException e) {
            updateStatus(transaction, TransferTransactionStatus.FAILED, e.getMessage());
            throw e;
        } catch (ExternalServiceException e) {
            updateStatus(transaction, TransferTransactionStatus.WITHDRAW_PENDING, e.getMessage());
            throw e;
        } catch (TransferCompensatedException e) {
            updateStatus(transaction, TransferTransactionStatus.COMPENSATED, e.getMessage());
            throw e;
        } catch (CompensationFailedException e) {
            updateStatus(transaction, TransferTransactionStatus.COMPENSATED_FAILED, e.getMessage());
            throw e;
        }
    }

    private void saveOutbox(TransferResponse payload, TransferTransaction transaction) {
        var span = tracer.nextSpan().name("saveOutboxInDatabase").start();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .source("transfer-service")
                    .eventType(EventType.TRANSFER_PERFORMED)
                    .payload(payloadJson)
                    .message("%1$s выполнил перевод %2$s на сумму %3$s, новый баланс отправителя %4$s, новый баланс получателя %5$s"
                            .formatted(transaction.getFromAccount(), transaction.getToAccount(),
                                    transaction.getAmount(), payload.newBalanceSender(), payload.newBalanceReceiver()))
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать payload для outbox, транзакция ID: {}",
                    transaction.getTransactionId(), e);
        } finally {
            span.end();
        }
    }

    private BigDecimal withdraw(TransferRequest request, TransferTransaction transaction) {
        BigDecimal newBalance = accountServiceClient.withdraw(request.fromAccount(), request.amount());
        updateStatus(transaction, TransferTransactionStatus.DEPOSIT_PENDING, null);
        return newBalance;
    }

    private TransferResponse deposit(TransferRequest request, TransferTransaction transaction, BigDecimal senderBalance) {
        try {
            BigDecimal receiverBalance = accountServiceClient.deposit(request.toAccount(), request.amount());
            updateStatus(transaction, TransferTransactionStatus.SUCCESS, null);
            log.info("Перевод успешен. Отправитель: {}, сумма: {}", request.fromAccount(), request.amount());
            return new TransferResponse(transaction.getTransactionId().toString(), senderBalance, receiverBalance);
        } catch (Exception e) {
            boolean compensated = compensate(transaction);
            if (compensated) {
                throw new TransferCompensatedException(
                        "Перевод не выполнен, средства возвращены. Причина: " + getMessage(e));
            } else {
                throw new CompensationFailedException(
                        "КРИТИЧЕСКАЯ ОШИБКА: деньги списаны, но не удалось вернуть. Причина: " + getMessage(e));
            }
        }
    }

    private boolean compensate(TransferTransaction transaction) {
        try {
            accountServiceClient.deposit(transaction.getFromAccount(), transaction.getAmount());
            return true;
        } catch (Exception compensationError) {
            log.error("Ошибка при компенсации: {}", getMessage(compensationError));
            return false;
        }
    }

    private void updateStatus(TransferTransaction transaction, TransferTransactionStatus status, String message) {
        var span = tracer.nextSpan().name("updateTransactionInDatabase").start();
        try {
            transaction.setStatus(status);
            transaction.setMessage(message);
            transactionRepository.save(transaction);
        } finally {
            span.end();
        }
    }

    private String getMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private TransferTransaction createTransaction(TransferRequest request) {
        TransferTransaction pendingTransaction = null;
        var span = tracer.nextSpan().name("saveTransactionInDatabase").start();
        try {
            TransferTransaction transaction = TransferTransaction.builder()
                    .fromAccount(request.fromAccount())
                    .toAccount(request.toAccount())
                    .amount(request.amount())
                    .status(TransferTransactionStatus.WITHDRAW_PENDING)
                    .build();
            pendingTransaction = transactionRepository.save(transaction);
        } finally {
            span.end();
        }
        return pendingTransaction;
    }
}
