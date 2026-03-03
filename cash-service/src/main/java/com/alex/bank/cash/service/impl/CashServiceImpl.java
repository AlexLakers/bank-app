package com.alex.bank.cash.service.impl;

import com.alex.bank.cash.client.account.AccountServiceClient;
import com.alex.bank.cash.dto.CashRequest;
import com.alex.bank.cash.dto.CashResponse;
import com.alex.bank.cash.exception.AccountNotFoundException;
import com.alex.bank.cash.exception.AccountValidationException;
import com.alex.bank.cash.exception.ExternalServiceException;
import com.alex.bank.cash.exception.InsufficientFundsException;
import com.alex.bank.cash.model.CashAction;
import com.alex.bank.cash.model.CashTransaction;
import com.alex.bank.cash.model.CashTransactionStatus;
import com.alex.bank.cash.repository.CashTransactionRepository;
import com.alex.bank.cash.service.CashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
@Slf4j
public class CashServiceImpl implements CashService {

    private final AccountServiceClient accountServiceClient;
    private final CashTransactionRepository cashTransactionRepository;

    private CashTransaction createAndSavePendingTransaction(CashAction action, String accountHolder, BigDecimal amount) {
        CashTransaction transaction = CashTransaction.builder()
                .action(action)
                .accountHolder(accountHolder)
                .status(CashTransactionStatus.PENDING)
                .amount(amount)
                .build();
        return cashTransactionRepository.save(transaction);
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
            return handleSuccess(transaction, newBalance, action, request.amount());
        } catch (AccountNotFoundException | InsufficientFundsException | AccountValidationException | ExternalServiceException e) {
            throw handleFailure(transaction, e);
        } catch (Exception e) {
            throw handleUnexpectedFailure(transaction, e);
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

    private RuntimeException handleFailure(CashTransaction transaction, RuntimeException e) {
        transaction.setStatus(CashTransactionStatus.FAILED);
        transaction.setMessage(e.getMessage());
        cashTransactionRepository.save(transaction);
        return e;
    }

    private RuntimeException handleUnexpectedFailure(CashTransaction transaction, Exception e) {
        transaction.setStatus(CashTransactionStatus.FAILED);
        transaction.setMessage("Внутренняя ошибка сервиса");
        cashTransactionRepository.save(transaction);
        return new RuntimeException("Внутренняя ошибка", e);
    }
}




