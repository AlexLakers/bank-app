package com.alex.bank.transfer.dto;

import java.time.LocalDateTime;

public record NotificationResponse(String notificationId, EventStatus status, LocalDateTime processedAt) {
}
