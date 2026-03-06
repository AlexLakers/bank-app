package com.alex.bank.transfer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "transfer")
@Component
@Getter
@Setter
public class TransferServicePropertiesConfig {
    private NotificationServiceProperties notificationService = new NotificationServiceProperties();
    private AccountServiceProperties accountService = new AccountServiceProperties();

    @Getter
    @Setter
    static class NotificationServiceProperties {
        private String baseUrl="http://localhost:8086";
    }
    @Getter
    @Setter
    static class AccountServiceProperties {
        private String baseUrl="http://localhost:8082";
    }
}

