package com.alex.bank.transfer.scheduler;



import com.alex.bank.common.dto.notification.*;
import com.alex.bank.transfer.model.Outbox;
import com.alex.bank.transfer.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.alex.bank.common.dto.notification.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxNotificationScheduler {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final String IDEMPOTENCY_KEY_HEADER = "idempotency-key";

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<Outbox> outboxList = outboxRepository.findNotProcessed();
        List<CompletableFuture<SendResult<String, NotificationRequest>>> futures = new ArrayList<>();
        outboxList.forEach(outbox -> {
            try {
                Map<String, Object> payloadMap = objectMapper.readValue(outbox.getPayload(),
                        new TypeReference<Map<String, Object>>() {
                        });

                ProducerRecord<String, NotificationRequest> record = new ProducerRecord<>(
                        "transfer-events",
                        outbox.getEventId().toString(),
                        buildNotification(outbox, payloadMap));

                record.headers().add(IDEMPOTENCY_KEY_HEADER, outbox.getEventId().toString().getBytes());

                CompletableFuture<SendResult<String, NotificationRequest>> future = kafkaTemplate.send(record);

                future.whenComplete((result, e) -> {
                    if (e != null) {
                        log.error("Error sending event {}: {}", outbox.getEventId(), e.getMessage());
                        return;
                    }
                    RecordMetadata meta = result.getRecordMetadata();
                    log.info("Event {} sent to topic:{}, partition:{}, offset:{}",
                            outbox.getEventId(), meta.topic(), meta.partition(), meta.offset());
                    outbox.setProcessedAt(LocalDateTime.now());
                    outboxRepository.markAsProcessed(outbox.getProcessedAt(), outbox.getEventId());
                });
                futures.add(future);

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception e) {
                log.error("Error processing outbox event {}", outbox.getEventId(), e);
            }
        });
    }

    private NotificationRequest buildNotification(Outbox outbox, Map<String, Object> payloadMap) {
        return new NotificationRequest(
                outbox.getEventId().toString(),
                outbox.getSource(),
                outbox.getEventType(),
                outbox.getMessage(),
                payloadMap);
    }

}
