package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@RequiredArgsConstructor
public class IdempotencyKeyRecordFilterStrategy implements RecordFilterStrategy<String, NotificationRequest> {
    private final String IDEMPOTENCY_KEY_HEADER = "idempotency-key";
    private final EventIdempotenceRepository eventIdempotenceRepository;

    @Override
    public boolean filter(ConsumerRecord<String, NotificationRequest> consumerRecord) {
        Header header= consumerRecord.headers().lastHeader(IDEMPOTENCY_KEY_HEADER);

        return header==null
                ? false
                :!checkIdempotencyKey(header.value());
    }

    private boolean checkIdempotencyKey(byte[] rawIdempotencyKey) {
        return rawIdempotencyKey == null || rawIdempotencyKey.length == 0
                ? false
                : eventIdempotenceRepository.existsByEventId(new String(rawIdempotencyKey));
    }


}
