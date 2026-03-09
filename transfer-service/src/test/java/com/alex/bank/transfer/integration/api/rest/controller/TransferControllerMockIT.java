package com.alex.bank.transfer.integration.api.rest.controller;

import com.alex.bank.transfer.api.rest.controller.TransferController;
import com.alex.bank.common.dto.transfer.*;
import com.alex.bank.transfer.repository.OutboxRepository;
import com.alex.bank.transfer.repository.TransferTransactionRepository;
import com.alex.bank.transfer.security.SecurityConfig;
import com.alex.bank.transfer.service.TransferService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class TransferControllerMockIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;
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
    void transfer_success() throws Exception {
        TransferRequest request = new TransferRequest("alexeev", "sergeev", BigDecimal.valueOf(1200));
        TransferResponse response = new TransferResponse(
                UUID.randomUUID().toString(),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(1200)
        );
        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(response.transactionId()))
                .andExpect(jsonPath("$.newBalanceSender").value(response.newBalanceSender()))
                .andExpect(jsonPath("$.newBalanceReceiver").value(response.newBalanceReceiver()));
    }

    @Test
    void transfer_forbidden_whenMissingTransferWriteAuthority() throws Exception {
        TransferRequest request = new TransferRequest("alexeev", "sergeev", BigDecimal.valueOf(200));

        mockMvc.perform(post("/api/v1/transfer")
                        .with(jwt().jwt(jwt -> jwt
                                .claim("preferred_username", "testuser")
                                .claim("realm_access", Map.of("roles", List.of("USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_unauthorized_whenNotAuthenticated() throws Exception {
        TransferRequest request = new TransferRequest("alexeev", "sergeev", BigDecimal.valueOf(200));

        mockMvc.perform(post("/api/v1/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}