package com.alex.bank.account.client.notification;


/*import com.alex.bank.account.dto.NotificationRequest;*/
//import com.alex.bank.account.dto.NotificationResponse;
import com.alex.bank.common.dto.notification.NotificationResponse;
import com.alex.bank.common.dto.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationServiceClient {
    private final RestClient notificationRestClient;


    public NotificationServiceClient(@Qualifier("notificationsRestClient") RestClient notificationRestClient) {
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
