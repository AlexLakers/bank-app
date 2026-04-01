package com.alex.bank.notification.repository;

import com.alex.bank.notification.model.DeadLetterQueue;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterQueueRepository extends CrudRepository<DeadLetterQueue, Long> {
}
