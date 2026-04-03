package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.exception.NotificationDeserializationException;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EventIdempotenceRepository eventIdempotenceRepository;
    private final Tracer tracer;

    @KafkaListener(topics = {"account-events", "cash-events", "transfer-events"},
            containerFactory = "kafkaListenerContainerFactory", groupId = "notification-handlers")
    public void listen(ConsumerRecord<String, NotificationRequest> record, Acknowledgment acknowledgment) {

        NotificationRequest notification = record.value();
        log.info("Event [{} type:{} source:{} message:{} data:{}]",
                notification.eventId(), notification.eventType(), notification.source(), notification.message(), notification.payload());
        Span span = tracer.nextSpan().name("saveOutboxInDatabase").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            eventIdempotenceRepository.saveEvent(notification.eventId());
        } finally {
            span.end();
        }
        acknowledgment.acknowledge();
    }
}