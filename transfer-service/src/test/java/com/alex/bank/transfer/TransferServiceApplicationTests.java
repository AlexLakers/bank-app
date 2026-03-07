package com.alex.bank.transfer;

import com.alex.bank.transfer.repository.OutboxRepository;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class TransferServiceApplicationTests {

    @MockitoBean
    private TransferTransactionRepository transferTransactionRepository;
    @MockitoBean
    private OutboxRepository outboxRepository;

    @Test
    void contextLoads() {
    }
}
