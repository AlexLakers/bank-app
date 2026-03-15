package com.alex.bank.ui;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BankUiApplicationTests {

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
