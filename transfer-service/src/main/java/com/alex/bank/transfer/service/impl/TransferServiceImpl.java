package com.alex.bank.transfer.service.impl;

import com.alex.bank.transfer.client.account.AccountServiceClient;
import com.alex.bank.transfer.dto.TransferRequest;
import com.alex.bank.transfer.dto.TransferResponse;
import com.alex.bank.transfer.exception.*;
import com.alex.bank.transfer.model.TransferTransaction;
import com.alex.bank.transfer.model.TransferTransactionStatus;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import com.alex.bank.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferTransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

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
            return deposit(request, transaction, newBalanceSender);
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

    private BigDecimal withdraw(TransferRequest request, TransferTransaction transaction) {
        BigDecimal newBalance = accountServiceClient.withdrawCash(request.fromAccount(), request.amount());
        updateStatus(transaction, TransferTransactionStatus.DEPOSIT_PENDING, null);
        return newBalance;
    }

    private TransferResponse deposit(TransferRequest request, TransferTransaction transaction, BigDecimal senderBalance) {
        try {
            BigDecimal receiverBalance = accountServiceClient.depositCash(request.toAccount(), request.amount());
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
            accountServiceClient.depositCash(transaction.getFromAccount(), transaction.getAmount());
            return true;
        } catch (Exception compensationError) {
            log.error("Ошибка при компенсации: {}", getMessage(compensationError));
            return false;
        }
    }

    private void updateStatus(TransferTransaction transaction, TransferTransactionStatus status, String message) {
        transaction.setStatus(status);
        transaction.setMessage(message);
        transactionRepository.save(transaction);
    }

    private String getMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private TransferTransaction createTransaction(TransferRequest request) {
        TransferTransaction transaction = TransferTransaction.builder()
                .fromAccount(request.fromAccount())
                .toAccount(request.toAccount())
                .amount(request.amount())
                .status(TransferTransactionStatus.WITHDRAW_PENDING)
                .build();
        return transactionRepository.save(transaction);
    }
}
