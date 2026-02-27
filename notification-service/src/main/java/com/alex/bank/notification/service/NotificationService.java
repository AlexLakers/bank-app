package com.alex.bank.notification.service;

import com.alex.bank.notification.dto.NotificationRequest;
import com.alex.bank.notification.dto.NotificationResponse;

public interface NotificationService {
     NotificationResponse processNotification(NotificationRequest notificationRequest);

}
