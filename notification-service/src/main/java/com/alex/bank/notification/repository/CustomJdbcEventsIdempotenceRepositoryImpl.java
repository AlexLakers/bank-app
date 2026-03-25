package com.alex.bank.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CustomJdbcEventsIdempotenceRepositoryImpl implements CustomJdbcEventsIdempotenceRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveNotification(String id) {
        String sql = """
                INSERT INTO notifications (notification_id, processed_at) VALUES (?, ?);
                """;
        jdbcTemplate.update(sql, id, LocalDateTime.now());
    }
}
