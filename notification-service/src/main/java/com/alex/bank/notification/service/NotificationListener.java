package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.exception.NotificationDeserializationException;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EventIdempotenceRepository eventIdempotenceRepository;

    @KafkaListener(topics = "account-events",
            containerFactory = "kafkaListenerContainerFactory",groupId = "notification-handlers-v2")
    public void listen(ConsumerRecord<String, NotificationRequest> record, Acknowledgment acknowledgment) {

        NotificationRequest notification = record.value();
        log.info("Event [{} type:{} source:{} message:{} data:{}]",
                notification.eventId(), notification.eventType(), notification.source(), notification.message(), notification.payload());
        eventIdempotenceRepository.saveEvent(notification.eventId());
        acknowledgment.acknowledge();

    }

}
