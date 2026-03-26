package com.alex.bank.notification.service.impl;

//import com.alex.bank.notification.dto.EventStatus;
//import com.alex.bank.notification.dto.NotificationRequest;
//import com.alex.bank.notification.dto.NotificationResponse;

import com.alex.bank.common.dto.notification.*;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import com.alex.bank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EventIdempotenceRepository notificationRepository;

    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional
    public NotificationResponse processNotification(NotificationRequest request) {
        if (checkNotificationByIdempotenceKey(request.eventId())) {
            log.debug("Дублирующее событие отколнено: {}", request.eventId());
            return new NotificationResponse(request.eventId(), EventStatus.DUPLICATED, LocalDateTime.now());
        }
        log.info("Событие [{} тип:{} отправитель:{} сообщение:{} данные:{}]",
                request.eventId(), request.eventType(), request.source(), request.message(), request.payload());
        // processedEvents.add(request.eventId());
        notificationRepository.saveEvent(request.eventId());

        return new NotificationResponse(request.eventId(), EventStatus.PROCESSED, LocalDateTime.now());

    }

    private boolean checkNotificationByIdempotenceKey(String idempotenceKey) {
        return notificationRepository.existsByEventId(idempotenceKey);
    }

}
