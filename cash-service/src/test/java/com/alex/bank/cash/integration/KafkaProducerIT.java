package com.alex.bank.cash.integration;

import com.alex.bank.common.dto.notification.EventType;
import com.alex.bank.common.dto.notification.NotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        topics = {"cash-events"},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
public class KafkaProducerIT {

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

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
                    "newBalance", 10000.00
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
        }
    }
}
