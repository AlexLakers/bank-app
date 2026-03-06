package com.alex.bank.ui.client;

import com.alex.bank.ui.dto.ApiResult;
import com.alex.bank.ui.dto.transfer.TransferRequest;
import com.alex.bank.ui.dto.transfer.TransferResponse;
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
public class TransferServiceClient {

    private final RestClient transferRestClient;

    public ApiResult<TransferResponse> processTransferOperation(TransferRequest request) {
        try {
            ResponseEntity<TransferResponse> response = transferRestClient.post()
                    .uri("/transfer")
                    .body(request)
                    .retrieve()
                    .toEntity(TransferResponse.class);

            return ApiResult.success(response.getBody(), null);

        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String errorBody = e.getResponseBodyAsString();
            log.warn("Transfer-service: HTTP ошибка {}: {}", status, errorBody);

            String userMessage = !errorBody.isBlank()
                    ? errorBody
                    : switch ((HttpStatus) status) {
                case NOT_FOUND -> "Получатель не найден";
                case BAD_REQUEST -> "Некорректный запрос";
                case CONFLICT -> "На счете отправителя недостаточно средств";
                default -> "Ошибка сервиса переводов: " + e.getStatusText();
            };
            return ApiResult.error(userMessage);

        } catch (ResourceAccessException e) {
            log.error("Transfer-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error(" Сервис переводов временно недоступен");
        } catch (RestClientException e) {
            log.error("Transfer-service: неожиданная ошибка RestClient: {}", e.getMessage());
            return ApiResult.error("Внутренняя ошибка сервиса переводов");
        }
    }
}