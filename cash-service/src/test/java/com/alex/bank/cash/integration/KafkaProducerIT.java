package com.alex.bank.cash.integration;

import com.alex.bank.cash.client.account.AccountServiceClient;
import com.alex.bank.cash.config.PostgresTestconteinerConfig;
import com.alex.bank.cash.service.CashService;
import com.alex.bank.common.dto.notification.EventType;
import com.alex.bank.common.dto.notification.NotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"cash-events"},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
public class KafkaProducerIT {

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;


    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @MockitoBean
    private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;

    @Test
    void shouldSendCashEventWithTransactionIdAndNewBalance() throws Exception {
        var consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        JsonDeserializer<NotificationRequest> jsonDeserializer = new JsonDeserializer<>(NotificationRequest.class);
        jsonDeserializer.addTrustedPackages("*");

        try (var consumer = new DefaultKafkaConsumerFactory<String, NotificationRequest>(
                consumerProps,
                new StringDeserializer(),
                jsonDeserializer
        ).createConsumer()) {

            consumer.subscribe(List.of("cash-events"));

            UUID transactionId = UUID.randomUUID();
            Map<String, Object> payload = Map.of(
                    "transactionId", transactionId.toString(),
                    "newBalance", 10000.00,
                    "username","testuser"
            );

            NotificationRequest notification = new NotificationRequest(
                    "test-cash-event-id",
                    "cash-service",
                    EventType.CASH_WITHDRAWAL,
                    "Снятие выполнено",
                    payload
            );

            kafkaTemplate.send("cash-events", notification.eventId(), notification).get(5, TimeUnit.SECONDS);

            ConsumerRecord<String, NotificationRequest> record = KafkaTestUtils.getSingleRecord(consumer, "cash-events", Duration.ofSeconds(5));

            assertThat(record.key()).isEqualTo("test-cash-event-id");
            NotificationRequest received = record.value();
            assertThat(received.eventId()).isEqualTo("test-cash-event-id");
            assertThat(received.source()).isEqualTo("cash-service");
            assertThat(received.eventType()).isEqualTo(EventType.CASH_WITHDRAWAL);
            assertThat(received.payload()).containsEntry("transactionId", transactionId.toString());
            assertThat(received.payload()).containsEntry("newBalance", 10000.00);
            assertThat(received.payload()).containsEntry("username", "testuser");
        }
    }

}
