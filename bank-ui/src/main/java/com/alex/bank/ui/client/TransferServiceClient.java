package com.alex.bank.ui.client;

//import com.alex.bank.ui.dto.ApiResult;
//import com.alex.bank.ui.dto.transfer.TransferRequest;
//import com.alex.bank.ui.dto.transfer.TransferResponse;
import com.alex.bank.common.dto.transfer.*;
import com.alex.bank.common.dto.ui.ApiResult;
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

            String userMessage = getUserFriendlyMessage(status, errorBody);
            return ApiResult.error(userMessage);

        } catch (ResourceAccessException e) {
            log.error("Transfer-service: сетевая ошибка: {}", e.getMessage());
            return ApiResult.error("Сервис переводов временно недоступен. Проверьте соединение и повторите позже.");
        } catch (RestClientException e) {
            log.error("Transfer-service: неожиданная ошибка RestClient: {}", e.getMessage());
            return ApiResult.error("Внутренняя ошибка сервиса переводов. Пожалуйста, обратитесь в поддержку.");
        }
    }

    private String getUserFriendlyMessage(HttpStatusCode status, String errorBody) {
        HttpStatus httpStatus = (HttpStatus) status;

        if (httpStatus == HttpStatus.CONFLICT) {
            if (errorBody != null && (errorBody.contains("Insufficient funds") || errorBody.contains("недостаточно средств"))) {
                return "Недостаточно средств на счёте отправителя.";
            } else {
                return "Перевод не выполнен, но средства возвращены. Попробуйте позже или проверьте счёт получателя.";
            }
        }

        return switch (httpStatus) {
            case NOT_FOUND -> "Счёт получателя не найден. Проверьте номер счёта.";
            case BAD_REQUEST -> "Некорректный запрос. Проверьте введённые данные.";
            case SERVICE_UNAVAILABLE -> "Сервис переводов временно недоступен. Попробуйте позже.";
            case INTERNAL_SERVER_ERROR -> "Ошибка сервера при переводе. Пожалуйста, обратитесь в поддержку.";
            default -> "Ошибка при выполнении перевода. Попробуйте ещё раз.";
        };
    }
}