package com.alex.bank.account.dto;


import com.alex.bank.account.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationRequest(
        @NotBlank(message = "Идентификатор не может быть пустым")
        String eventId,

        @NotBlank(message = "Отправитель не может быть пустым")
        String source,

        @NotNull(message = "Тип события не может быть null")
        EventType eventType,

        String message,

        @NotNull(message = "Полезные данные не указаны")
        Map<String,Object> payload) {
}
