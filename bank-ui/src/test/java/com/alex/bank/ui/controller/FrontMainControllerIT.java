package com.alex.bank.ui.controller;

//import com.alex.bank.ui.dto.account.AccountDto;
//import com.alex.bank.ui.dto.account.AccountEditDto;
import com.alex.bank.common.dto.account.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FrontMainControllerIT.TestRestClientConfig.class)
public class FrontMainControllerIT {


    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @MockitoBean
    private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;


    @RegisterExtension
    static WireMockExtension wireMockAccount = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension wireMockCash = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("bank.services.account.url", wireMockAccount::baseUrl);
        registry.add("bank.services.cash.url", wireMockCash::baseUrl);
    }

    //TODO test for cash not work because url(before one gateway url after three urls)
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void index_shouldRedirectToAccount() throws Exception {
        mockMvc.perform(get("/ui")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"));
    }

    @Test
    void showMainPage_shouldReturnPageWithAccountAndList() throws Exception {
        AccountDto currentAccount = new AccountDto("testuser", "Test User", BigDecimal.valueOf(1000), "1990-01-01");
        List<AccountDto> otherAccounts = List.of(
                new AccountDto("other1", "Other One", BigDecimal.valueOf(500), "1995-05-05")
        );

        ObjectMapper testMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();

        wireMockAccount.stubFor(WireMock.get(urlEqualTo("/api/v1/accounts/me"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(testMapper.writeValueAsString(currentAccount))));

        wireMockAccount.stubFor(WireMock.get(urlPathEqualTo("/api/v1/accounts"))
                .withQueryParam("excludeCurrent", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(testMapper.writeValueAsString(otherAccounts))));

        mockMvc.perform(get("/ui/account")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("account", currentAccount))
                .andExpect(model().attribute("accounts", otherAccounts))
                .andExpect(model().attributeDoesNotExist("errors"))
                .andExpect(model().attributeDoesNotExist("info"));
    }

    @Test
    void showMainPage_whenAccountFails_shouldReturnEmptyAccountAndError() throws Exception {
        List<AccountDto> otherAccounts = List.of(
                new AccountDto("other1", "Other One", BigDecimal.valueOf(500), "1995-05-05")
        );

        ObjectMapper testMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();

        wireMockAccount.stubFor(WireMock.get(urlEqualTo("/api/v1/accounts/me"))
                .willReturn(aResponse().withStatus(404)));

        wireMockAccount.stubFor(WireMock.get(urlPathEqualTo("/api/v1/accounts"))
                .withQueryParam("excludeCurrent", equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(testMapper.writeValueAsString(otherAccounts))));

        mockMvc.perform(get("/ui/account")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("accounts", otherAccounts))
                .andExpect(model().attribute("errors", hasItem("Сервис аккаунтов временно недоступен. Пожалуйста, попробуйте позже.")))
                .andExpect(model().attributeDoesNotExist("info"));
    }

    @Test
    void showMainPage_whenAccountsFails_shouldReturnEmptyListAndError() throws Exception {
        AccountDto currentAccount = new AccountDto("testuser", "Test User", BigDecimal.valueOf(1000), "1990-01-01");

        ObjectMapper testMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();

        wireMockAccount.stubFor(WireMock.get(urlEqualTo("/api/v1/accounts/me"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(testMapper.writeValueAsString(currentAccount))));

        wireMockAccount.stubFor(WireMock.get(urlPathEqualTo("/api/v1/accounts"))
                .withQueryParam("excludeCurrent", equalTo("true"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/ui/account")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attribute("account", currentAccount))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("accounts", emptyIterable()))
                .andExpect(model().attribute("errors", hasItem("Сервис аккаунтов временно недоступен.")))
                .andExpect(model().attributeDoesNotExist("info"));
    }

    @Test
    void showMainPage_whenBothFail_shouldReturnBothErrors() throws Exception {
        wireMockAccount.stubFor(WireMock.get(urlEqualTo("/api/v1/accounts/me"))
                .willReturn(aResponse().withStatus(404)));

        wireMockAccount.stubFor(WireMock.get(urlPathEqualTo("/api/v1/accounts"))
                .withQueryParam("excludeCurrent", equalTo("true"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/ui/account")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("accounts"))
                .andExpect(model().attribute("accounts", emptyIterable()))
                .andExpect(model().attribute("errors", containsInAnyOrder(
                        "Сервис аккаунтов временно недоступен.",
                        "Сервис аккаунтов временно недоступен. Пожалуйста, попробуйте позже."
                )))
                .andExpect(model().attributeDoesNotExist("info"));
    }

    @Test
    void updateAccount_shouldRedirectWithSuccessFlash() throws Exception {
        AccountEditDto editDto = new AccountEditDto("Updated Name", LocalDate.of(1990, 1, 1));
        ObjectMapper testMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();
        String responseBody = testMapper.writeValueAsString(
                new AccountDto("testuser", "Updated Name", BigDecimal.valueOf(1000.00), "1990-01-01")
        );

        wireMockAccount.stubFor(put(urlEqualTo("/api/v1/accounts"))
                .withRequestBody(equalToJson(testMapper.writeValueAsString(editDto)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(post("/ui/account")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("name", "Updated Name")
                        .param("birthdate", "1990-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("info"))
                .andExpect(flash().attribute("info", "Данные успешно обновлены"));
    }

    @Test
    void updateAccount_whenAccountServiceReturns404_shouldRedirectWithErrorFlash() throws Exception {
        wireMockAccount.stubFor(put(urlEqualTo("/api/v1/accounts"))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(post("/ui/account")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("name", "New Name")
                        .param("birthdate", "1990-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("errors"))
                .andExpect(flash().attribute("errors", List.of("Аккаунт не найден. Проверьте идентификатор.")));
    }

    @Test
    void updateAccount_validationError_shouldRedirectWithoutCallingService() throws Exception {
        mockMvc.perform(post("/ui/account")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("name", "")
                        .param("birthdate", "1990-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("errors"));
    }
    @Test
    void cash_success_withdraw() throws Exception {
        wireMockCash.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/cash/owner/operations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"amount\":100.0}")));

        mockMvc.perform(post("/ui/cash")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("amount", "100")
                        .param("action", "GET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("info"))
                .andExpect(flash().attribute("info", "Снято 100"));
    }

    @Test
    void cash_success_deposit() throws Exception {
        wireMockCash.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/cash/owner/operations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"amount\":50.0}")));

        mockMvc.perform(post("/ui/cash")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("amount", "50")
                        .param("action", "PUT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("info"))
                .andExpect(flash().attribute("info", "Положено 50"));
    }

    @Test
    void cash_validationError() throws Exception {
        mockMvc.perform(post("/ui/cash")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("amount", "-10")
                        .param("action", "GET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("errors"))
                .andExpect(flash().attribute("errors", hasSize(greaterThan(0))));
    }

    @Test
    void cash_serviceError() throws Exception {
        wireMockCash.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/cash/owner/operations"))
                .willReturn(aResponse().withStatus(400)));

        mockMvc.perform(post("/ui/cash")
                        .with(csrf())
                        .with(user("testuser").roles("USER"))
                        .param("amount", "100")
                        .param("action", "GET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/account"))
                .andExpect(flash().attributeExists("errors"))
                .andExpect(flash().attribute("errors", hasItem(containsString("Некорректный запрос"))));
    }

    @TestConfiguration
    static class TestRestClientConfig {
        @Bean
        @Primary
        public RestClient.Builder testRestClientBuilder() {
            return RestClient.builder();
        }
    }
}