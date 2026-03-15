package com.alex.bank.account;

import com.alex.bank.account.config.PostgresTestconteinerConfig;
import com.alex.bank.account.repository.AccountRepository;
import com.alex.bank.account.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
@ActiveProfiles("test")
class AccountServiceApplicationTests {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private OAuth2AuthorizedClientManager authorizedClientManager;

	@MockitoBean
	private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

	@MockitoBean
	private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;
	@Test
	void contextLoads() {
	}

}
