package com.alex.bank.account.integration;

import com.alex.bank.account.config.PostgresTestconteinerConfig;
import com.alex.bank.common.dto.notification.EventType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import com.alex.bank.common.dto.notification.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"account-events"},
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
    void shouldSendNotificationWhenAccountUpdated() throws Exception {
        var consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        JsonDeserializer<NotificationRequest> jsonDeserializer = new JsonDeserializer<>(NotificationRequest.class);
        jsonDeserializer.addTrustedPackages("*");
        try (var consumer = new DefaultKafkaConsumerFactory<String, NotificationRequest>(
                consumerProps,
                new StringDeserializer(),
                jsonDeserializer
        ).createConsumer()) {
            consumer.subscribe(List.of("account-events"));

            NotificationRequest request = new NotificationRequest(
                    "test-event-id",
                    "account-service",
                    EventType.ACCOUNT_UPDATED,
                    "User data updated",
                    Map.of("username", "alexeev", "name", "Alexey")
            );
            kafkaTemplate.send("account-events", request.eventId(), request).get(5, TimeUnit.SECONDS);

            ConsumerRecord<String, NotificationRequest> record = KafkaTestUtils.getSingleRecord(consumer, "account-events", Duration.ofSeconds(5));
            assertThat(record.key()).isEqualTo("test-event-id");
            NotificationRequest received = record.value();
            assertThat(received.eventId()).isEqualTo("test-event-id");
            assertThat(received.source()).isEqualTo("account-service");
        }
    }
}


