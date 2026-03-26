package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.exception.NotificationDeserializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.serializer.FailedDeserializationInfo;

import java.util.function.Function;

@Slf4j
public class NotificationErrorProvider implements Function<FailedDeserializationInfo, NotificationRequest> {
    @Override
    public NotificationRequest apply(FailedDeserializationInfo failedDeserializationInfo) {
        Exception deserializeException = failedDeserializationInfo.getException();
        log.error("Deserialization notification error : {}", deserializeException.getMessage(), deserializeException);
        throw new NotificationDeserializationException(deserializeException.getMessage(),deserializeException);
    }
}
