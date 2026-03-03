package com.alex.bank.account;

import com.alex.bank.account.config.PostgresTestconteinerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
class AccountServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
