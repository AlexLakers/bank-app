package com.alex.bank.notification;

import com.alex.bank.notification.repository.CustomJdbcEventsIdempotenceRepository;
import com.alex.bank.notification.repository.DeadLetterQueueRepository;
import com.alex.bank.notification.repository.EventIdempotenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @MockitoBean
    private DeadLetterQueueRepository deadLetterQueueRepository;
    @MockitoBean
    private EventIdempotenceRepository eventIdempotenceRepository;
    @MockitoBean
    private CustomJdbcEventsIdempotenceRepository customJdbcEventsIdempotenceRepository;

    @Test
    void contextLoads() {
    }

}
