package com.alex.bank.notification;

import com.alex.bank.common.dto.notification.EventType;
import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.config.PostgresTestconteinerConfig;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@EmbeddedKafka(topics = {"account-events"}, partitions = 1)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cloud.consul.enabled=false")
public class ConsumerKafkaIT {

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Autowired
    private EventIdempotenceRepository eventIdempotenceRepository;

    @Test
    void shouldConsumeAndStoreEvent() throws Exception {
        NotificationRequest request = new NotificationRequest(
                "unique-event-id",
                "account-service",
                EventType.ACCOUNT_UPDATED,
                "User data updated",
                Map.of("username", "alexeev", "name", "Alexey")
        );

        ProducerRecord<String, NotificationRequest> record = new ProducerRecord<>("account-events", request.eventId(), request);
        record.headers().add("idempotency-key", request.eventId().getBytes());

        kafkaTemplate.send(record).get();

        // waiting record in table 'events_idempotance'
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    boolean exists = eventIdempotenceRepository.existsByEventId("unique-event-id");
                    assertThat(exists).isTrue();
                });
    }
}
