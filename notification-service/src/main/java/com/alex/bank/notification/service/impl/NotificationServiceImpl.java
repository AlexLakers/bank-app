package com.alex.bank.notification.service.impl;

import com.alex.bank.notification.dto.NotificationRequest;
import com.alex.bank.notification.dto.NotificationResponse;
import com.alex.bank.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override

    public NotificationResponse processNotification(NotificationRequest notificationRequest) {
        return ;
    }
}
