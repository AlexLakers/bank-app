package com.alex.bank.cash;

import com.alex.bank.cash.client.account.AccountServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("contract-test")
@AutoConfigureStubRunner(
        ids = "com.alex:account-service:+:stubs:8082",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(AccountServiceClientContractTest.TestRestClientConfig.class)
public class AccountServiceClientContractTest {

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setupOAuth2Mock() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
    }

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Test
    void shouldIncreaseBalance() {
        String owner = "alexeev";
        BigDecimal amount = new BigDecimal("200.00");

        BigDecimal newBalance = accountServiceClient.depositCash(owner, amount);

        assertNotNull(newBalance);
        assertEquals(new BigDecimal("1200.00"), newBalance);
    }

    @Test
    void shouldDecreaseBalance() {
        String owner = "alexeev";
        BigDecimal amount = new BigDecimal("200.00");

        BigDecimal newBalance = accountServiceClient.withdrawCash(owner, amount);

        assertNotNull(newBalance);
        assertEquals(new BigDecimal("800.00"), newBalance);
    }

    @TestConfiguration
    static class TestRestClientConfig {
        @Bean
        @Primary
        public RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }
}
