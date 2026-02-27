package com.alex.bank.notification.controller;

import com.alex.bank.notification.dto.NotificationRequest;
import com.alex.bank.notification.dto.NotificationResponse;
import com.alex.bank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/")
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> processNotification(@Validated NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.processNotification(request));
    }

}
