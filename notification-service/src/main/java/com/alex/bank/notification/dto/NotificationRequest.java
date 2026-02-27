package com.alex.bank.notification.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationRequest(
        @NotBlank(message = "Идентификатор не может быть пустым")
        String eventId,

        @NotBlank(message = "Отправитель не может быть пустым")
        String source,

        @NotNull(message = "Тип события не может быть null")
        EventType eventType,

        @NotBlank(message = "Полезные данные не указаны")
        String payload) {
}
