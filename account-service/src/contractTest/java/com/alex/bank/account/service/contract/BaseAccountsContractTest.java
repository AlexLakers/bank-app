package com.alex.bank.account.service.contract;

import com.alex.bank.account.service.AccountService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("contract-test")
public abstract class BaseAccountsContractTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected AccountService accountService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);


        when(accountService.increaseBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("1200.00"));

        when(accountService.decreaseBalance(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("800.00"));
    }
}

