package com.alex.bank.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(String notificationId, LocalDateTime processedAt) {
}
