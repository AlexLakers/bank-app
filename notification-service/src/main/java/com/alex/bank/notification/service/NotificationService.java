package com.alex.bank.notification.service;

import com.alex.bank.common.dto.notification.*;

public interface NotificationService {
     NotificationResponse processNotification(NotificationRequest notificationRequest);

}
