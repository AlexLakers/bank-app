package com.alex.bank.account.integration.controller;

import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.dto.MoneyOperationRequest;
import com.alex.bank.account.integration.BaseIntegrationTest;
import com.alex.bank.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class AccountControllerIT extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAuthAccount_shouldReturnAuthCurrentAccount_whenUserIsAuth() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("realm_access", Map.of("roles", List.of("USER")))
                                        .claim("preferred_username", "testov1"))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testov1"))
                .andExpect(jsonPath("$.name").value("Testov Test"))
                .andExpect(jsonPath("$.birthdate").value("1993-01-01"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void getAuthAccount_shouldSet403Status_whenRoleIsNotValid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("realm_access", Map.of("roles", List.of("USER-NOT-VALID")))
                                        .claim("preferred_username", "testov1"))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_USER-NOT-VALID")))))
                .andExpect(status().isForbidden());
    }
    @Test
    void getAuthAccount_whenUserNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("realm_access", Map.of("roles", List.of("USER")))
                                        .claim("preferred_username", "nonexistent"))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountsExcludeAuth_shouldReturnEmptyAccountsList_whenIsOnlyAuth() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .param("excludeCurrent","true")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("realm_access", Map.of("roles", List.of("USER", "ACCOUNTS_WRITE")))
                                        .claim("preferred_username", "testov1"))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ACCOUNTS_WRITE")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    void updateUserDataAccount_shouldUpdateAndReturnAccount_whenEditDtoIsValid() throws Exception {

            String newName = "Updated Name";
            LocalDate newBirthdate = LocalDate.of(1990, 5, 15);
            AccountEditDto editDto = new AccountEditDto(newName, newBirthdate);

            mockMvc.perform(put("/api/v1/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(editDto))
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .claim("preferred_username", "testov1")
                                            .claim("realm_access", Map.of("roles", List.of("USER", "ACCOUNTS_WRITE")))
                                    )
                                    .authorities(List.of(
                                            new SimpleGrantedAuthority("ROLE_USER"),
                                            new SimpleGrantedAuthority("ACCOUNTS_WRITE")
                                    ))
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testov1"))
                    .andExpect(jsonPath("$.name").value(newName))
                    .andExpect(jsonPath("$.birthdate").value(newBirthdate.toString()))
                    .andExpect(jsonPath("$.balance").value(1000.00));
        }

    @Test
    void updateUserDataAccount_shouldSetStatus404_whenUserNotHaveRoles() throws Exception {

        String newName = "Updated Name";
        LocalDate newBirthdate = LocalDate.of(1990, 5, 15);
        AccountEditDto editDto = new AccountEditDto(newName, newBirthdate);

        mockMvc.perform(put("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto))
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .claim("preferred_username", "testov1")
                                        .claim("realm_access", Map.of("roles", List.of("USER")))
                                )
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                ))
                        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void increaseBalance_shouldIncrementBalanceAndReturnNewBalance_success() throws Exception {
        String owner = "testov1";
        BigDecimal amount = new BigDecimal("200.00");
        MoneyOperationRequest request = new MoneyOperationRequest(amount);

        mockMvc.perform(patch("/api/v1/accounts/{owner}/balance/increase", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("preferred_username", owner))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ACCOUNTS_WRITE")
                                ))
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("1200.00"));
    }

    @Test
    void decreaseBalance_shouldDecrementBalanceAndReturnNewBalance_success() throws Exception {
        String owner = "testov1";
        BigDecimal amount = new BigDecimal("100.00");
        MoneyOperationRequest request = new MoneyOperationRequest(amount);

        mockMvc.perform(patch("/api/v1/accounts/{owner}/balance/decrease", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("preferred_username", owner))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ACCOUNTS_WRITE")
                                ))
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("900.00"));
    }

    @Test
    void decreaseBalance_negativeAmount_shouldReturnBadRequest() throws Exception {
        String owner = "testov1";
        BigDecimal amount = new BigDecimal("-50.00"); // отрицательная сумма
        MoneyOperationRequest request = new MoneyOperationRequest(amount);

        mockMvc.perform(patch("/api/v1/accounts/{owner}/balance/decrease", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("preferred_username", owner))
                                .authorities(List.of(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ACCOUNTS_WRITE")
                                ))
                        ))
                .andExpect(status().isBadRequest()); // ожидаем 400 из-за @Positive
    }
}