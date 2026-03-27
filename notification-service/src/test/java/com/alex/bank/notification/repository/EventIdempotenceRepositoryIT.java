package com.alex.bank.notification.repository;

import com.alex.bank.notification.config.PostgresTestconteinerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@ActiveProfiles("test")
class EventIdempotenceRepositoryIT {

    @Autowired
    private EventIdempotenceRepository repository;

    @Test
    void existsByEventId_shouldReturnTrueWhenRecordExists() {
        String eventId = "existing-id";
        repository.saveEvent(eventId);
        boolean exists = repository.existsByEventId(eventId);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEventId_shouldReturnFalseWhenRecordDoesNotExist() {
        boolean exists = repository.existsByEventId("non-existing");
        assertThat(exists).isFalse();
    }

    @Test
    void saveEvent_shouldReturnTrueWhenRecordExists() {
        repository.saveEvent("event-id");
        boolean exists = repository.existsByEventId("event-id");
        assertThat(exists).isTrue();
    }
}