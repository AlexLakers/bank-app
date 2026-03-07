package com.alex.bank.notification.service.contract;

import com.alex.bank.notification.dto.EventStatus;
import com.alex.bank.notification.dto.NotificationRequest;
import com.alex.bank.notification.dto.NotificationResponse;
import com.alex.bank.notification.service.NotificationService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("contract-test")
public abstract class BaseNotificationsContractTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected NotificationService notificationService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        NotificationResponse mockResponse = new NotificationResponse(
                "3504103f-750d-4622-b6e9-dd9136a23b43",
                EventStatus.PROCESSED,
                LocalDateTime.parse("2026-03-01T00:47:24.775009")
        );

        when(notificationService.processNotification(any(NotificationRequest.class)))
                .thenReturn(mockResponse);
    }
}

