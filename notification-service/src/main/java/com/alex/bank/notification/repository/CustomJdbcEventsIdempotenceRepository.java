package com.alex.bank.notification.repository;

public interface CustomJdbcEventsIdempotenceRepository {
    void saveEvent(String id);
}
