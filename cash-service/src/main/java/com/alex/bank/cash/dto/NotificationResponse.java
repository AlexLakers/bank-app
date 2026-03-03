package com.alex.bank.cash.dto;

import java.time.LocalDateTime;

public record NotificationResponse(String notificationId, EventStatus status, LocalDateTime processedAt) {
}
