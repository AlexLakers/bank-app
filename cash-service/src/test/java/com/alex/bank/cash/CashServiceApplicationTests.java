package com.alex.bank.cash;

import com.alex.bank.cash.config.PostgresTestconteinerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
/*@Testcontainers*/
/*@ImportTestcontainers({PostgresTestconteinerConfig.class})*/
class CashServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
