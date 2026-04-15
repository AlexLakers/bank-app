package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.model.DeadLetterQueue;
import com.alex.bank.notification.repository.DeadLetterQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeadLetterQueueRecordRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterQueueRepository repository;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueRecordRecoverer(DeadLetterQueueRepository repository,
                                          ObjectMapper objectMapper,
                                          MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception e) {
        String username = extractUsername(record);

        //business-metric
        meterRegistry.counter("notification_failures",
                        "username", username)
                .increment();

        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        deadLetterQueue.setMsgTopic(record.topic());
        deadLetterQueue.setMsgPartition(record.partition());
        deadLetterQueue.setMsgOffset(record.offset());
        if (record.key() instanceof String key) {
            deadLetterQueue.setMsgKey(key);
        }

        deadLetterQueue.setErrorMessage(e.getMessage());

        repository.save(deadLetterQueue);
        log.info("Message {} saved in dlq table", record.key());
        meterRegistry.counter("dead-letter-queue-record-received").increment();
    }

    private String extractUsername(ConsumerRecord<?, ?> record) {
        try {
            Object value = record.value();
            if (value instanceof NotificationRequest notification) {
                return (String) notification.payload().get("username");
            } else if (value instanceof byte[] bytes) {
                NotificationRequest notification = objectMapper.readValue(bytes, NotificationRequest.class);
                return (String) notification.payload().get("username");
            }
        } catch (Exception ex) {
            log.warn("Cannot extract username from record", ex);
        }
        return null;
    }
}
