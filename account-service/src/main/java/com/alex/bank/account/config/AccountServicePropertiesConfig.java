package com.alex.bank.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "accounts")
@Component
@Getter
@Setter
public class AccountServicePropertiesConfig {

    private String baseUrl="http://localhost:8086";
}
