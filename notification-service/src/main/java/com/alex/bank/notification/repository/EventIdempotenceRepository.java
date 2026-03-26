package com.alex.bank.notification.repository;

import com.alex.bank.notification.model.EventIdempotence;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventIdempotenceRepository extends CrudRepository<EventIdempotence,String>, CustomJdbcEventsIdempotenceRepository {

    @Query("SELECT EXISTS(SELECT 1 FROM events_idempotence WHERE event_id=:eventId)")
    boolean existsByEventId(String eventId);
}
