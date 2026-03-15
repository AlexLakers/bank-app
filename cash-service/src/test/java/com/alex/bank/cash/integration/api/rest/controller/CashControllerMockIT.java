package com.alex.bank.cash.integration.api.rest.controller;

import com.alex.bank.cash.api.rest.controller.CashController;
import com.alex.bank.cash.security.SecurityConfig;
import com.alex.bank.cash.service.CashService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.alex.bank.common.dto.cash.*;

@WebMvcTest(CashController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class CashControllerMockIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @MockitoBean
    private InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void withdrawCash_success() throws Exception {

        CashRequest request = new CashRequest(CashAction.GET, BigDecimal.valueOf(100));
        CashResponse response = new CashResponse("3131231-fdsfsd-312fd=fdsf", BigDecimal.valueOf(100));
        when(cashService.processCash(eq("testuser"), any(CashRequest.class)))
                .thenReturn(response);


        mockMvc.perform(post("/api/v1/cash/owner/operations")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("CASH_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("3131231-fdsfsd-312fd=fdsf"))
                .andExpect(jsonPath("$.newBalance").value(100));
    }

    @Test
    void withdrawCash_forbidden_whenMissingCashWriteAuthority() throws Exception {
        CashRequest request = new CashRequest(CashAction.GET, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/v1/cash/owner/operations")
                        .with(jwt().jwt(jwt -> jwt
                                .claim("preferred_username", "testuser")
                                .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void withdrawCash_forbidden_whenNotAuthenticated() throws Exception {
        CashRequest request = new CashRequest(CashAction.GET, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/v1/cash/owner/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}