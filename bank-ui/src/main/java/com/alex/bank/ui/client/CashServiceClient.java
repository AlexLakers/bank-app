package com.alex.bank.ui.client;


import com.alex.bank.ui.dto.ApiResult;
import com.alex.bank.ui.dto.cash.CashRequest;
import com.alex.bank.ui.dto.cash.CashResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashServiceClient {

    private final RestClient cashRestClient;

    public ApiResult<CashResponse> processCashOperation(CashRequest request) {
        try {
            ResponseEntity<CashResponse> response = cashRestClient.post()
                    .uri("/cash/owner/operations")
                    .body(request)
                    .retrieve()
                    .toEntity(CashResponse.class);

            return ApiResult.success(response.getBody(), null);

        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String errorBody = e.getResponseBodyAsString();
            log.warn("Cash-service: HTTP ошибка {}: {}", status, errorBody);

            String userMessage = !errorBody.isBlank()
                    ? errorBody
                    : switch ((HttpStatus) status) {
                case NOT_FOUND -> "Аккаунт не найден";
                case BAD_REQUEST -> "Некорректный запрос";
                case CONFLICT -> "На счете недостаточно средств";
                default -> "Ошибка сервиса пополнения/снятия: " + e.getStatusText();
            };
            return ApiResult.error(userMessage);

        } catch (ResourceAccessException e) {
            log.error("Cash-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error(" Сервис переводов временно недоступен");
        } catch (RestClientException e) {
            log.error("Cash-service: неожиданная ошибка RestClient: {}", e.getMessage());
            return ApiResult.error("Внутренняя ошибка сервиса переводов");
        }
    }
}
