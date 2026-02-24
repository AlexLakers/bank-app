package com.alex.bank.ui.client;


import com.alex.bank.ui.dto.ApiResult;
import com.alex.bank.ui.dto.account.AccountDto;
import com.alex.bank.ui.dto.account.AccountEditDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

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
            log.warn("Account-service: HTTP ошибка {}: {}", status, e.getMessage());

            String body = e.getResponseBodyAsString();
            String userMessage = !body.isBlank()
                    ? body
                    : switch ((HttpStatus) status) {
                case NOT_FOUND -> "Аккаунт не найден";
                case BAD_REQUEST -> "Некорректный запрос";
                default -> "Ошибка при обращении к сервису аккаунтов";
            };
            return ApiResult.error(userMessage);
        } catch (ResourceAccessException e) {
            log.error("Account-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error("Сервис аккаунтов временно недоступен");
        } catch (RestClientException e) {
            log.error("Account-service: неожиданная ошибка RestClient", e);
            return ApiResult.error("Внутренняя ошибка сервиса аккаунтов");
        }
    }

    public ApiResult<AccountDto> getAuthAccount() {
        Supplier<AccountDto> requestAccount = () -> accountsRestClient.get()
                .uri("/accounts/me")
                .retrieve()
                .toEntity(AccountDto.class).getBody();

        return executeRequest(requestAccount);

    }

    public ApiResult<List<AccountDto>> getAccountsExcludeAuth() {
        Supplier<List<AccountDto>> requestAccounts = () -> accountsRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/accounts")
                        .queryParam("excludeCurrent", true)
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<AccountDto>>() {
                }).getBody();
        return executeRequest(requestAccounts);

    }

    public ApiResult<AccountDto> updateAuthAccount(AccountEditDto accountEditDto) {
        Supplier<AccountDto> requestUpdateAccount = () -> accountsRestClient.put()
                .uri("/accounts")
                .body(accountEditDto)
                .retrieve()
                .toEntity(AccountDto.class).getBody();

        return executeRequest(requestUpdateAccount);
    }

}
