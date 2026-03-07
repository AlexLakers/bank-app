package com.alex.bank.transfer.scheduler;



import com.alex.bank.transfer.client.notification.NotificationServiceClient;
import com.alex.bank.common.dto.notification.*;
import com.alex.bank.transfer.model.Outbox;
import com.alex.bank.transfer.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxNotificationScheduler {

    private final OutboxRepository outboxRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper;


    @Scheduled(fixedRate = 5000)
    public void processOutbox() {

        List<Outbox> outboxList = outboxRepository.findNotProcessed();
        outboxList.forEach(outbox -> {
                    try {
                        Map<String, Object> payloadMap = objectMapper.readValue(outbox.getPayload(),
                                new TypeReference<Map<String, Object>>() {
                                });

                        log.info("Sending notification: eventId={}, source={}, eventType={}, payload={}",
                                outbox.getEventId(), outbox.getSource(), outbox.getEventType(), payloadMap);

                        NotificationResponse response = notificationServiceClient.sendNotification(new NotificationRequest(
                                outbox.getEventId().toString(),
                                outbox.getSource(),
                                outbox.getEventType(),
                                outbox.getMessage(),
                                payloadMap));

                        outbox.setProcessedAt(response.processedAt());
                        outboxRepository.markAsProcessed(response.processedAt(), UUID.fromString(response.notificationId()));
                        log.info("Событие {} было {} в {}",
                                response.notificationId(), response.status(), response.processedAt());
                    } catch (Exception e) {
                        log.error("Ошибка обработки события {}", outbox.getEventId(), e);
                    }
                }
        );

    }

}
