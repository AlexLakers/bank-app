package com.alex.bank.notification.repository;

import com.alex.bank.notification.entity.Event;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends CrudRepository<Event,String> {

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE event_id=:event_id)")
    boolean existsByEventId(String eventId);
}
