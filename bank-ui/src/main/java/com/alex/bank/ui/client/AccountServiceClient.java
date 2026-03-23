package com.alex.bank.ui.client;


//import com.alex.bank.ui.dto.ApiResult;
/*import com.alex.bank.ui.dto.account.AccountDto;
import com.alex.bank.ui.dto.account.AccountEditDto;*/

import com.alex.bank.common.dto.ui.ApiResult;
import com.alex.bank.common.dto.account.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestClient accountsRestClient;

    private <T> ApiResult<T> executeRequest(Supplier<T> request) {
        try {
            T payload = request.get();
            return ApiResult.success(payload, null);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String body = e.getResponseBodyAsString();
            log.warn("Account-service: HTTP ошибка {}: {}", status, body); // тело в лог

            String userMessage = getUserFriendlyMessage(status);
            return ApiResult.error(userMessage);
        } catch (ResourceAccessException e) {
            log.error("Account-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error("Сервис аккаунтов временно недоступен. Проверьте соединение и повторите позже.");
        } catch (RestClientException e) {
            log.error("Account-service: неожиданная ошибка RestClient", e);
            return ApiResult.error("Внутренняя ошибка сервиса аккаунтов. Пожалуйста, обратитесь в поддержку.");
        }
    }


    private String getUserFriendlyMessage(HttpStatusCode status) {
        HttpStatus httpStatus = (HttpStatus) status;
        return switch (httpStatus) {
            case NOT_FOUND -> "Аккаунт не найден. Проверьте идентификатор.";
            case BAD_REQUEST -> "Некорректный запрос. Проверьте введённые данные.";
            case SERVICE_UNAVAILABLE -> "Сервис аккаунтов временно недоступен. Пожалуйста, попробуйте позже.";
            default -> "Ошибка при обращении к сервису аккаунтов. Попробуйте ещё раз или обратитесь в поддержку.";
        };
    }

    @Retry(name = "retry-account-service", fallbackMethod = "fallbackGetAuthAccount")
    @CircuitBreaker(name = "circuit-account-service", fallbackMethod = "fallbackGetAuthAccount")
    public ApiResult<AccountDto> getAuthAccount() {
        AccountDto result = accountsRestClient.get()
                .uri("/api/v1/accounts/me")
                .retrieve()
                .body(AccountDto.class);
        return ApiResult.success(result, null);
    }

    private ApiResult<AccountDto> fallbackGetAuthAccount(Throwable e) {
        log.error("Fallback для getAuthAccount после исчерпания попыток или размыкания цепи", e);
        return ApiResult.error("Сервис аккаунтов временно недоступен. Пожалуйста, попробуйте позже.");
    }

    @Retry(name = "retry-account-service", fallbackMethod = "fallbackGetAccounts")
    @CircuitBreaker(name = "circuit-account-service", fallbackMethod = "fallbackGetAccounts")
    public ApiResult<List<AccountDto>> getAccountsExcludeAuth() {
        List<AccountDto> result = accountsRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/accounts")
                        .queryParam("excludeCurrent", true)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<AccountDto>>() {
                });
        return ApiResult.success(result, null);
    }

    private ApiResult<List<AccountDto>> fallbackGetAccounts(Throwable e) {
        log.error("Fallback для getAccountsExcludeAuth", e);
        return ApiResult.error("Сервис аккаунтов временно недоступен.");
    }

    @Retry(name = "retry-account-service", fallbackMethod = "fallbackUpdateAccount")
    @CircuitBreaker(name = "circuit-account-service", fallbackMethod = "fallbackUpdateAccount")

    public ApiResult<AccountDto> updateAuthAccount(AccountEditDto accountEditDto) {
        Supplier<AccountDto> requestUpdateAccount = () -> accountsRestClient.put()
                .uri("/api/v1/accounts")
                .body(accountEditDto)
                .retrieve()
                .toEntity(AccountDto.class).getBody();
        return executeRequest(requestUpdateAccount);
    }

    private ApiResult<List<AccountDto>> fallbackUpdateAccount(AccountEditDto accountEditDto, Throwable e) {
        log.error("Fallback для updateAuthAccount", e);
        return ApiResult.error("Сервис аккаунтов временно недоступен.");
    }
}