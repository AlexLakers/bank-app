package com.alex.bank.notification.service;

import com.alex.bank.notification.model.DeadLetterQueue;
import com.alex.bank.notification.repository.DeadLetterQueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeadLetterQueueRecordRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterQueueRepository repository;

    public DeadLetterQueueRecordRecoverer(DeadLetterQueueRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception e) {
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        deadLetterQueue.setMsgTopic(record.topic());
        deadLetterQueue.setMsgPartition(record.partition());
        deadLetterQueue.setMsgOffset(record.offset());
        if (record.key() instanceof String key) {
            deadLetterQueue.setMsgKey(key);
        }
        deadLetterQueue.setErrorMessage(e.getMessage());

        repository.save(deadLetterQueue);
        log.info("Сообщение {} сохранено в DLQ", record.key());
    }
}
