package com.alex.bank.transfer;

import com.alex.bank.transfer.repository.OutboxRepository;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class TransferServiceApplicationTests {

    @MockitoBean
    private TransferTransactionRepository transferTransactionRepository;
    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @MockitoBean
    private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;

    @Test
    void contextLoads() {
    }
}
