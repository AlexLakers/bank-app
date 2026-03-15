package com.alex.bank.cash.client.notification;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.alex.bank.common.dto.notification.*;

@Component
public class NotificationServiceClient {


    private final RestClient notificationRestClient;

    public NotificationServiceClient(@Qualifier("notificationRestClient") RestClient notificationRestClient) {
        this.notificationRestClient = notificationRestClient;
    }


    public NotificationResponse sendNotification(NotificationRequest notificationRequest) {

        return notificationRestClient.post()
                .uri("/api/v1/notifications")
                .body(notificationRequest)
                .retrieve()
                .toEntity(NotificationResponse.class).getBody();
    }
}
