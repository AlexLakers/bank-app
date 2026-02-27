package com.alex.bank.account.repository;

import com.alex.bank.account.model.Outbox;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends CrudRepository<Outbox, String> {

    @Query("SELECT * FROM outbox WHERE processed_at IS NULL ORDER BY created_at ASC")
     List<Outbox> findNotProcessed();

    @Modifying
    @Query("UPDATE outbox SET processed_at=:processedAt WHERE event_id=:eventId")
    Long markAsProcessed(LocalDateTime processedAt, UUID eventId);
}
