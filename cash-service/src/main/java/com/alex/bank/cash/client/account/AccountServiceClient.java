package com.alex.bank.cash.client.account;

//import com.alex.bank.cash.dto.MoneyOperationRequest;
//import com.alex.bank.cash.exception.AccountNotFoundException;
//import com.alex.bank.cash.exception.AccountValidationException;
//import com.alex.bank.cash.exception.ExternalServiceException;
//import com.alex.bank.cash.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import com.alex.bank.common.exceptions.*;
import com.alex.bank.common.dto.account.MoneyOperationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Component
public class AccountServiceClient {

    private final RestClient accountRestClient;

    public AccountServiceClient(@Qualifier("accountsRestClient") RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }

    public BigDecimal withdrawCash(String username, BigDecimal amount) {
        return executeWithErrorHandling(() -> accountRestClient.patch()
                .uri("/api/v1/accounts/{owner}/balance/decrease", username)
                .body(new MoneyOperationRequest(amount))
                .retrieve()
                .body(BigDecimal.class));
    }

    public BigDecimal depositCash(String username, BigDecimal amount) {
        return executeWithErrorHandling(() -> accountRestClient.patch()
                .uri("/api/v1/accounts/{owner}/balance/increase", username)
                .body(new MoneyOperationRequest(amount))
                .retrieve()
                .body(BigDecimal.class));
    }

    private BigDecimal executeWithErrorHandling(Supplier<BigDecimal> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            handleError(e);
            return null;
        } catch (ResourceAccessException e) {
            throw new ExternalServiceException("Сервис аккаунтов временно недоступен: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Внутренняя ошибка при вызове сервиса аккаунтов: " + e.getMessage(), e);
        }
    }

    private void handleError(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        String errorBody = e.getResponseBodyAsString();
        switch ((HttpStatus) status) {
            case NOT_FOUND:
                throw new AccountNotFoundException(buildMessage(errorBody, "Аккаунт не найден"));
            case BAD_REQUEST:
                throw new AccountValidationException(buildMessage(errorBody, "Некорректные данные для снятия/пополения"));
            case CONFLICT:
                throw new InsufficientFundsException(buildMessage(errorBody, "На счете недостаточно средств"));
            default:
                throw new ExternalServiceException(buildMessage(errorBody, "Ошибка сервиса аккаунтов: " + e.getStatusText()));
        }
    }

    private String buildMessage(String body, String defaultMessage) {
        return (body != null && !body.isBlank()) ? body : defaultMessage;
    }
}
