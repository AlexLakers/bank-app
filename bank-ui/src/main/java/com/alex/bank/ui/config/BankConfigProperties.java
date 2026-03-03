package com.alex.bank.ui.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "bank.gateway")
@Component
@Getter
@Setter
public class BankConfigProperties {

    private String baseUrl="http://localhost:8888";
}
