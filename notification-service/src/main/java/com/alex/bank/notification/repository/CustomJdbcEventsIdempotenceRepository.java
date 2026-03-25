package com.alex.bank.notification.repository;

public interface CustomJdbcEventsIdempotenceRepository {
    void saveNotification(String id);
}
