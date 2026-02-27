package com.alex.bank.account.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@UtilityClass
public class SecurityUtil {

    public static String getCurrentUsernameFromJwtToken(Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse("anonymous");
    }
}
