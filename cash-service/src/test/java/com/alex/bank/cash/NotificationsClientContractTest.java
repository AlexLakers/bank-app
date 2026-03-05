package com.alex.bank.cash;


import com.alex.bank.cash.client.notification.NotificationServiceClient;
import com.alex.bank.cash.dto.EventStatus;
import com.alex.bank.cash.dto.NotificationRequest;
import com.alex.bank.cash.dto.NotificationResponse;
import com.alex.bank.cash.model.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("contract-test")
@AutoConfigureStubRunner(
        ids = "com.alex:notification-service:+:stubs:8086",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(NotificationsClientContractTest.TestRestClientConfig.class)
public class NotificationsClientContractTest {


    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setupOAuth2Mock() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
    }

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Test
    void shouldMatchContractWhenCheckingOwner() {
        String eventId = "3504103f-750d-4622-b6e9-dd9136a23b43";
        NotificationRequest request = new NotificationRequest(
                eventId,
                "account-service",
                EventType.ACCOUNT_UPDATED,
                "Данные пользователя alexeev были обновлены",
                Map.of(
                        "username", "alexeev",
                        "name", "Alexeev Alexeyy",
                        "balance", 10000.00,
                        "birthdate", "1993-01-21"
                ));

        NotificationResponse response = notificationServiceClient.sendNotification(request);
        assertNotNull(response);
        assertEquals(eventId, response.notificationId());
        assertEquals(EventStatus.PROCESSED, response.status());

        assertNotNull(response.processedAt());
    }

    @TestConfiguration
    static class TestRestClientConfig {
        @Bean
        @Primary
        public RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }
}
