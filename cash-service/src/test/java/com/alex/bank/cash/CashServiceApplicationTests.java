package com.alex.bank.cash;

import com.alex.bank.cash.config.PostgresTestconteinerConfig;
import com.alex.bank.cash.repository.CashTransactionRepository;
import com.alex.bank.cash.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class CashServiceApplicationTests {

    @MockitoBean
    private CashTransactionRepository cashTransactionRepository;
    @MockitoBean
    private OutboxRepository outboxRepository;

    @Test
    void contextLoads() {
    }
}