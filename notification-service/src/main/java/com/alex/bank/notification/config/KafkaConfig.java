package com.alex.bank.notification.config;

import com.alex.bank.notification.service.DeadLetterQueueRecordRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    CommonErrorHandler commonErrorHandler(DeadLetterQueueRecordRecoverer deadLetterQueueRecordRecoverer) {
        // Обработчик, который делает 10 попыток переполучения сообщения перед помещением в DLQ.
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(deadLetterQueueRecordRecoverer);

        // Обработчик, который отправляет сообщение в DLQ сразу (без попыток).
        DefaultErrorHandler deadLetterQueueErrorHandler = new DefaultErrorHandler(deadLetterQueueRecordRecoverer, new FixedBackOff(0, 0));

        // Обработчик, который делегирует в deadLetterQueueErrorHandler в случае IllegalArgumentException
        CommonDelegatingErrorHandler errorHandler = new CommonDelegatingErrorHandler(defaultErrorHandler);
        errorHandler.setErrorHandlers(Map.of(IllegalArgumentException.class, deadLetterQueueErrorHandler));

        return errorHandler;
    }
}
