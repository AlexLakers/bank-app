package com.alex.bank.cash.service.impl;

import com.alex.bank.cash.client.account.AccountServiceClient;
import com.alex.bank.cash.model.*;
import com.alex.bank.cash.repository.CashTransactionRepository;
import com.alex.bank.cash.repository.OutboxRepository;
import com.alex.bank.cash.service.CashService;
import com.alex.bank.common.dto.cash.CashRequest;
import com.alex.bank.common.dto.cash.CashResponse;
import com.alex.bank.common.dto.cash.CashAction;
import com.alex.bank.common.dto.notification.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alex.bank.common.exceptions.ExternalServiceException;
import com.alex.bank.common.exceptions.AccountValidationException;
import com.alex.bank.common.exceptions.AccountNotFoundException;
import com.alex.bank.common.exceptions.InsufficientFundsException;
import com.alex.bank.common.exceptions.CreatingPayloadOutboxException;


import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CashServiceImpl implements CashService {

    private final AccountServiceClient accountServiceClient;
    private final CashTransactionRepository cashTransactionRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    private CashTransaction createAndSavePendingTransaction(CashAction action, String accountHolder, BigDecimal amount) {
        var span = tracer.nextSpan().name("savePendingTransactionInDatabase").start();
        CashTransaction pendingTransaction;
        try {
            CashTransaction transaction = CashTransaction.builder()
                    .action(action)
                    .accountHolder(accountHolder)
                    .status(CashTransactionStatus.PENDING)
                    .amount(amount)
                    .build();
            pendingTransaction = cashTransactionRepository.save(transaction);
        } finally {
            span.end();
        }

        return pendingTransaction;
    }

    @Transactional(noRollbackFor = {
            AccountNotFoundException.class,
            InsufficientFundsException.class,
            AccountValidationException.class,
            ExternalServiceException.class
    })
    public CashResponse processCash(String username, CashRequest request) {
        CashAction action = request.action();
        CashTransaction transaction = createAndSavePendingTransaction(action, username, request.amount());
        try {
            BigDecimal newBalance = performCashOperation(action, username, request.amount());
            CashResponse cashResponse = handleSuccess(transaction, newBalance, action, request.amount());
            saveOutbox(cashResponse, transaction);
            return cashResponse;
        } catch (Exception e) {
            throw handleFailure(transaction, e);
        }

    }

    private void saveOutbox(CashResponse payload, CashTransaction cashTransaction) {
        var span = tracer.nextSpan().name("saveOutboxInDatabase").start();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            EventType eventType = cashTransaction.getAction() == CashAction.GET
                    ? EventType.CASH_WITHDRAWAL
                    : EventType.CASH_DEPOSITED;

            Outbox outbox = Outbox.builder()
                    .source("cash-service")
                    .eventType(eventType)
                    .payload(payloadJson)
                    .message("Пользователь: %1$s выполнил: %2$s на сумму: %3$s, новый баланс: %4$s"
                            .formatted(cashTransaction.getAccountHolder(), cashTransaction.getAction().name()
                                    , cashTransaction.getAmount(), payload.newBalance()))
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать payload для outbox, транзакция ID: {}",
                    cashTransaction.getTransactionId(), e);
        } finally {
            span.end();
        }
    }

    private BigDecimal performCashOperation(CashAction action, String username, BigDecimal amount) {
        return (action == CashAction.GET)
                ? accountServiceClient.withdrawCash(username, amount)
                : accountServiceClient.depositCash(username, amount);
    }

    private CashResponse handleSuccess(CashTransaction transaction, BigDecimal newBalance,
                                       CashAction action, BigDecimal amount) {

        transaction.setStatus(CashTransactionStatus.SUCCESS);
        cashTransactionRepository.save(transaction);
        log.info("Пользователь {} выполнил {} на сумму {}, новый баланс {}",
                transaction.getAccountHolder(), action, amount, newBalance);

        return new CashResponse(transaction.getTransactionId().toString(), newBalance);

    }

    private RuntimeException handleFailure(CashTransaction transaction, Exception e) {
        transaction.setStatus(CashTransactionStatus.FAILED);

        if (isExpectedException(e)) {
            transaction.setMessage(e.getMessage());
        } else {
            transaction.setMessage("Внутренняя ошибка сервиса");
        }

        cashTransactionRepository.save(transaction);

        log.info("Incrementing metric for user: {}", transaction.getAccountHolder());

        // business-metric
        meterRegistry.counter("cash_operation_failures",
                "username", transaction.getAccountHolder(),
                "action", transaction.getAction().name()
        ).increment();

        if (isExpectedException(e)) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException("Внутренняя ошибка", e);
        }
    }

    private boolean isExpectedException(Exception e) {
        return e instanceof AccountNotFoundException ||
               e instanceof InsufficientFundsException ||
               e instanceof AccountValidationException ||
               e instanceof ExternalServiceException;
    }
}




