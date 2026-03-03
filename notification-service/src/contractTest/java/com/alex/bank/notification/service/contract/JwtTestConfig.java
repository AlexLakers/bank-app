package com.alex.bank.notification.service.contract;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("contract-test")
public class JwtTestConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            Instant now = Instant.now();

            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("contract-test")
                    .claim("realm_access", Map.of(
                            "roles", List.of("SERVICE","NOTIFICATION_WRITE")
                    ))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build();
        };
    }
}
