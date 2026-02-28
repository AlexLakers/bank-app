package com.alex.bank.account.client.notification;

import com.alex.bank.account.dto.NotificationRequest;
import com.alex.bank.account.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NotificationServiceClient {
    private final RestClient notificationRestClient;


    public NotificationResponse sendNotification(NotificationRequest notificationRequest) {

       return notificationRestClient.post()
                .uri("/api/v1/notifications")
                .body(notificationRequest)
                .retrieve()
                .toEntity(NotificationResponse.class).getBody();
    }
}
