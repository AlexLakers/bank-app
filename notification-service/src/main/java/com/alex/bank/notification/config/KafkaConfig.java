package com.alex.bank.notification.config;

import com.alex.bank.common.dto.notification.NotificationRequest;
import com.alex.bank.notification.exception.NotificationDeserializationException;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import com.alex.bank.notification.service.DeadLetterQueueRecordRecoverer;
import com.alex.bank.notification.service.IdempotencyKeyRecordFilterStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    CommonErrorHandler commonErrorHandler(DeadLetterQueueRecordRecoverer deadLetterQueueRecordRecoverer) {

        // 10 retries with delay 1000ms(general handler)
       DefaultErrorHandler retryHandler=new DefaultErrorHandler(deadLetterQueueRecordRecoverer, new FixedBackOff(1000L, 9));

       //handler without retry for deserialization
       DefaultErrorHandler noRetryHandler=new DefaultErrorHandler(deadLetterQueueRecordRecoverer, new FixedBackOff(0, 0));

       CommonDelegatingErrorHandler commonDelegatingErrorHandler=new CommonDelegatingErrorHandler(retryHandler);
       commonDelegatingErrorHandler.setErrorHandlers(Map.of(NotificationDeserializationException.class,noRetryHandler));
        return commonDelegatingErrorHandler;
    }

    @Bean
    RecordFilterStrategy<String, NotificationRequest> idempotencyKeyRecordFilterStrategy(EventIdempotenceRepository eventIdempotenceRepository) {
        return new IdempotencyKeyRecordFilterStrategy(eventIdempotenceRepository);
    }

    //base bean for consumer
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> kafkaListenerContainerFactory(
            ConsumerFactory<String, NotificationRequest> consumerFactory,
            CommonErrorHandler commonErrorHandler,
            RecordFilterStrategy<String, NotificationRequest> idempotencyKeyRecordFilterStrategy
    ) {

        ConcurrentKafkaListenerContainerFactory<String, NotificationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory); // for base settings of consumer
        factory.setRecordFilterStrategy(idempotencyKeyRecordFilterStrategy); //for idempotency
        factory.setCommonErrorHandler(commonErrorHandler); //for error handling
        factory.setConcurrency(2); //for two partitions in every topic
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setAckDiscarded(true); //ack discard filter records for no loop

        return factory;
    }

}
