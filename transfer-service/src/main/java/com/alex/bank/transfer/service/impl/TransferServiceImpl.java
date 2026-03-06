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
import java.util.UUID;

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
            ExternalServiceException.class
    })
    public TransferResponse transfer(TransferRequest request) {
        TransferTransaction transaction = createTransaction(request);

        try {
            BigDecimal newBalanceSender = withdraw(request, transaction);
            return depositOrCompensate(request, transaction, newBalanceSender);
        } catch (AccountNotFoundException | InsufficientFundsException | AccountValidationException e) {
            failTransaction(transaction, e);
            throw e;
        } catch (ExternalServiceException e) {
            pendingTransaction(transaction, e);
            throw e;
        } catch (Exception e) {
            failTransaction(transaction, "Внутренняя ошибка сервиса");
            throw new RuntimeException("Внутренняя ошибка", e);
        }
    }

    private BigDecimal withdraw(TransferRequest request, TransferTransaction transaction) {
        BigDecimal newBalance = accountServiceClient.withdrawCash(request.fromAccount(), request.amount());
        transaction.setStatus(TransferTransactionStatus.DEPOSIT_PENDING);
        transactionRepository.save(transaction);
        return newBalance;
    }

    private TransferResponse depositOrCompensate(TransferRequest request, TransferTransaction transaction, BigDecimal senderBalance) {
        try {
            BigDecimal newBalanceReceiver = accountServiceClient.depositCash(request.toAccount(), request.amount());
            return completeSuccess(transaction, newBalanceReceiver, senderBalance);
        } catch (Exception e) {
            boolean compensated = compensate(request, transaction, e);
            if (!compensated) {
                throw new CompensationFailedException(
                        "Перевод не выполнен, средства возвращены. Причина: " + extractMessage(e)
                );
            }
            throw e;
        }
    }

    private boolean compensate(TransferRequest request, TransferTransaction transaction, Exception originalError) {
        try {
            accountServiceClient.depositCash(request.fromAccount(), request.amount());
            transaction.setStatus(TransferTransactionStatus.COMPENSATED);
            transaction.setMessage("Перевод не выполнен, деньги возвращены. Причина: " + extractMessage(originalError));
            transactionRepository.save(transaction);
            return true;
        } catch (Exception compensationError) {
            transaction.setStatus(TransferTransactionStatus.COMPENSATED_FAILED);
            transaction.setMessage("КРИТИЧЕСКАЯ ОШИБКА: деньги списаны, но не удалось вернуть. " +
                                   "Оригинал: " + extractMessage(originalError) +
                                   ", ошибка возврата: " + extractMessage(compensationError));
            transactionRepository.save(transaction);
            return false;
        }
    }

    private TransferResponse completeSuccess(TransferTransaction transaction, BigDecimal receiverBalance, BigDecimal senderBalance) {
        transaction.setStatus(TransferTransactionStatus.SUCCESS);
        TransferTransaction successTransaction = transactionRepository.save(transaction);
        log.info("Перевод успешен. Отправитель: {}, сумма: {}, новый баланс: {}",
                transaction.getFromAccount(), transaction.getAmount(), senderBalance);
        return new TransferResponse(successTransaction.getTransactionId().toString(), senderBalance, receiverBalance);
    }


    private void failTransaction(TransferTransaction transaction, Exception e) {
        transaction.setStatus(TransferTransactionStatus.FAILED);
        transaction.setMessage(e.getMessage());
        transactionRepository.save(transaction);
    }

    private void failTransaction(TransferTransaction transaction, String message) {
        transaction.setStatus(TransferTransactionStatus.FAILED);
        transaction.setMessage(message);
        transactionRepository.save(transaction);
    }

    private void pendingTransaction(TransferTransaction transaction, ExternalServiceException e) {
        transaction.setStatus(TransferTransactionStatus.WITHDRAW_PENDING);
        transaction.setMessage(e.getMessage());
        transactionRepository.save(transaction);
    }

    private String extractMessage(Exception e) {
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
