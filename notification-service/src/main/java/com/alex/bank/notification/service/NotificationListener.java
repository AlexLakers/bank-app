package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EventIdempotenceRepository eventIdempotenceRepository;

    @KafkaListener(topics = "account-events",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, NotificationRequest> record) {

        NotificationRequest notification = record.value();
        log.info("Событие [{} тип:{} отправитель:{} сообщение:{} данные:{}]",
                notification.eventId(), notification.eventType(), notification.source(), notification.message(), notification.payload());
        eventIdempotenceRepository.saveEvent(notification.eventId());

    }

}
