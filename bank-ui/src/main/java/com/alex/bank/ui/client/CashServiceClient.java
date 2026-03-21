package com.alex.bank.ui.client;


//import com.alex.bank.ui.dto.ApiResult;
//import com.alex.bank.ui.dto.cash.CashRequest;
//import com.alex.bank.ui.dto.cash.CashResponse;
import com.alex.bank.common.dto.cash.*;
import com.alex.bank.common.dto.ui.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                    .uri("/api/v1/cash/owner/operations")
                    .body(request)
                    .retrieve()
                    .toEntity(CashResponse.class);

            return ApiResult.success(response.getBody(), null);

        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String errorBody = e.getResponseBodyAsString();
            log.warn("Cash-service: HTTP ошибка {}: {}", status, errorBody);

            String userMessage = getUserFriendlyMessage(status, errorBody);
            return ApiResult.error(userMessage);

        } catch (ResourceAccessException e) {
            log.error("Cash-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error("Сервис пополнения/снятия временно недоступен. Проверьте соединение и повторите позже.");
        } catch (RestClientException e) {
            log.error("Cash-service: неожиданная ошибка RestClient: {}", e.getMessage());
            return ApiResult.error("Внутренняя ошибка сервиса пополнения/снятия. Пожалуйста, обратитесь в поддержку.");
        }
    }

    private String getUserFriendlyMessage(HttpStatusCode status, String errorBody) {
        HttpStatus httpStatus = (HttpStatus) status;

        return switch (httpStatus) {
            case NOT_FOUND -> "Аккаунт не найден. Проверьте номер счёта.";
            case BAD_REQUEST -> "Некорректный запрос. Проверьте введённые данные.";
            case CONFLICT -> "На счёте недостаточно средств для выполнения операции.";
            case SERVICE_UNAVAILABLE -> "Сервис пополнения/снятия временно недоступен. Пожалуйста, попробуйте позже.";
            default -> "Ошибка при обработке запроса. Попробуйте ещё раз или обратитесь в поддержку.";
        };
    }
}
