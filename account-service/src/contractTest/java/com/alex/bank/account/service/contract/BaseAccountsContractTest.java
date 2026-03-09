package com.alex.bank.account.service.contract;

import com.alex.bank.account.repository.AccountRepository;
import com.alex.bank.account.repository.OutboxRepository;
import com.alex.bank.account.service.AccountService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("contract-test")
@Import({JwtTestConfig.class, BaseAccountsContractTest.TestOAuth2Config.class})
public abstract class BaseAccountsContractTest{

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected AccountService accountService;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);


        when(accountService.increaseBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("1200.00"));

        when(accountService.decreaseBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("800.00"));
    }
    @TestConfiguration
    static class TestOAuth2Config {
        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(
                    ClientRegistration.withRegistrationId("keycloak")
                            .clientId("test-client")
                            .clientSecret("test-secret")
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .tokenUri("http://localhost:8080/token")
                            .build()
            );
        }

        @Bean
        @Primary
        public OAuth2AuthorizedClientService authorizedClientService() {
            return mock(OAuth2AuthorizedClientService.class);
        }
    }
}

