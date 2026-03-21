package com.alex.bank.ui.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "bank.services")
@Getter
@Setter
@Component
@Slf4j
public class BankConfigProperties {
    private ServiceUrl account;
    private ServiceUrl transfer;
    private ServiceUrl cash;
    private ServiceUrl gateway;

   @Getter
   @Setter
    public static class ServiceUrl {
        private String url;
    }
}
