package com.alex.bank.account.scheduler;

import com.alex.bank.account.client.notification.NotificationServiceClient;
//import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.common.dto.account.AccountEditDto;
import com.alex.bank.common.dto.account.AccountDto;
import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.common.dto.notification.NotificationResponse;
//import com.alex.bank.account.dto.NotificationRequest;
//import com.alex.bank.account.dto.NotificationResponse;
import com.alex.bank.account.model.Outbox;
import com.alex.bank.account.repository.OutboxRepository;
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

import java.time.Instant;
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
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;


    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<Outbox> outboxList = outboxRepository.findNotProcessed();
        List<CompletableFuture<SendResult<String, NotificationRequest>>> futures = new ArrayList<>();
        outboxList.forEach(outbox -> {
            try {
                Map<String, Object> payloadMap = objectMapper.readValue(outbox.getPayload(),
                        new TypeReference<Map<String, Object>>() {
                        });

                NotificationRequest event = new NotificationRequest(
                        outbox.getEventId().toString(),
                        outbox.getSource(),
                        outbox.getEventType(),
                        outbox.getMessage(),
                        payloadMap);

                ProducerRecord<String, NotificationRequest> record = new ProducerRecord<>(
                        "account-events",
                        outbox.getEventId().toString(),
                        event);
                record.headers().add("idempotencyKey", outbox.getEventId().toString().getBytes());

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

}
