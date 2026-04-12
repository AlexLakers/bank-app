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
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    @Transactional(noRollbackFor = {
            AccountNotFoundException.class,
            InsufficientFundsException.class,
            AccountValidationException.class,
            ExternalServiceException.class,
            TransferCompensatedException.class,
            CompensationFailedException.class
    })
    public TransferResponse transfer(TransferRequest request) {
        log.info("Transfer requested: from={}, to={}, amount={}", request.fromAccount(), request.toAccount(), request.amount());
        TransferTransaction transaction = createTransaction(request);

        try {
            BigDecimal newBalanceSender = withdraw(request, transaction);
            TransferResponse response = deposit(request, transaction, newBalanceSender);
            saveOutbox(response, transaction);
            log.info("Transfer completed successfully: from={}, to={}, amount={}, transactionId={}",
                    request.fromAccount(), request.toAccount(), request.amount(), transaction.getTransactionId());
            return response;
        } catch (AccountNotFoundException | InsufficientFundsException | AccountValidationException e) {
            updateStatus(transaction, TransferTransactionStatus.FAILED, e.getMessage());
            recordTransferFailure(request.fromAccount(), request.toAccount(), e.getMessage(), TransferTransactionStatus.FAILED);
            throw e;
        } catch (ExternalServiceException e) {
            updateStatus(transaction, TransferTransactionStatus.WITHDRAW_PENDING, e.getMessage());
            recordTransferFailure(request.fromAccount(), request.toAccount(), e.getMessage(), TransferTransactionStatus.WITHDRAW_PENDING);
            throw e;
        } catch (TransferCompensatedException e) {
            updateStatus(transaction, TransferTransactionStatus.COMPENSATED, e.getMessage());
            recordTransferFailure(request.fromAccount(), request.toAccount(), e.getMessage(), TransferTransactionStatus.COMPENSATED);
            throw e;
        } catch (CompensationFailedException e) {
            updateStatus(transaction, TransferTransactionStatus.COMPENSATED_FAILED, e.getMessage());
            recordTransferFailure(request.fromAccount(), request.toAccount(), e.getMessage(), TransferTransactionStatus.COMPENSATED_FAILED);
            throw e;
        }
    }

    private void saveOutbox(TransferResponse payload, TransferTransaction transaction) {
        log.debug("Saving outbox for transferId={}", transaction.getTransactionId());
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
            log.info("Outbox saved for transferId={}", transaction.getTransactionId());
        } catch (JsonProcessingException e) {
            log.error("Serialize error payload for outbox, transaction ID: {}",
                    transaction.getTransactionId(), e);
        } finally {
            span.end();
        }
    }

    private BigDecimal withdraw(TransferRequest request, TransferTransaction transaction) {
        log.debug("Withdrawing {} from account {} for transferId={}",
                request.amount(), request.fromAccount(), transaction.getTransactionId());
        BigDecimal newBalance = accountServiceClient.withdraw(request.fromAccount(), request.amount());
        updateStatus(transaction, TransferTransactionStatus.DEPOSIT_PENDING, null);
        log.info("Withdrawal successful: from={}, amount={}, newBalance={}, transferId={}",
                request.fromAccount(), request.amount(), newBalance, transaction.getTransactionId());
        return newBalance;
    }

    private TransferResponse deposit(TransferRequest request, TransferTransaction transaction, BigDecimal senderBalance) {
        log.debug("Depositing {} to account {} for transferId={}",
                request.amount(), request.toAccount(), transaction.getTransactionId());
        try {
            BigDecimal receiverBalance = accountServiceClient.deposit(request.toAccount(), request.amount());
            updateStatus(transaction, TransferTransactionStatus.SUCCESS, null);
            log.info("Deposit successful: to={}, amount={}, newBalance={}, transferId={}",
                    request.toAccount(), request.amount(), receiverBalance, transaction.getTransactionId());
            log.info("Transfer successful: from={}, to={}, amount={}, senderBalance={}, receiverBalance={}",
                    request.fromAccount(), request.toAccount(), request.amount(), senderBalance, receiverBalance);
            return new TransferResponse(transaction.getTransactionId().toString(), senderBalance, receiverBalance,transaction.getFromAccount());
        } catch (Exception e) {
            boolean compensated = compensate(transaction);
            if (compensated) {
                throw new TransferCompensatedException(
                        "Transfer is failed, amount returned. Reason: " + getMessage(e));
            } else {
                throw new CompensationFailedException(
                        "Critical error: money is increased, but not returned. Reason: " + getMessage(e));
            }
        }
    }

    private boolean compensate(TransferTransaction transaction) {
        log.warn("Compensating transferId={}: returning {} to account {}",
                transaction.getTransactionId(), transaction.getAmount(), transaction.getFromAccount());
        try {
            accountServiceClient.deposit(transaction.getFromAccount(), transaction.getAmount());
            return true;
        } catch (Exception compensationError) {
            log.error("Error during compensation: {}", getMessage(compensationError));
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
        log.debug("Creating pending transfer transaction: from={}, to={}, amount={}",
                request.fromAccount(), request.toAccount(), request.amount());
       // TransferTransaction pendingTransaction = null;
        var span = tracer.nextSpan().name("saveTransactionInDatabase").start();
        try {
            TransferTransaction transaction = TransferTransaction.builder()
                    .fromAccount(request.fromAccount())
                    .toAccount(request.toAccount())
                    .amount(request.amount())
                    .status(TransferTransactionStatus.WITHDRAW_PENDING)
                    .build();
            TransferTransaction savedTransaction= transactionRepository.save(transaction);
            log.debug("Transfer transaction created with id: {}", savedTransaction.getTransactionId());
            return savedTransaction;
        } finally {
            span.end();
        }
    }

    private void recordTransferFailure(String sender, String receiver, String reason, TransferTransactionStatus status) {
        String fullMessage = reason + " [%s] ".formatted(status.name());
        log.warn("Transfer failure recorded: sender={}, receiver={}, reason={}, status={}",
                sender, receiver, reason, status);
        //business-metric
        meterRegistry.counter("transfer_failures",
                "sender", sender,
                "receiver", receiver,
                "reason", fullMessage
        ).increment();
        log.debug("Transfer failure recorded: sender={}, receiver={}, reason={}", sender, receiver, reason);
    }
}
