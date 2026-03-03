package com.alex.bank.ui.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final OAuth2AuthorizedClientService authorizedClientService;


    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient accountsRestClient(RestClient.Builder restClientBuilder,
                                         BankConfigProperties bankConfigProperties) {
        return restClientBuilder
                .baseUrl(bankConfigProperties.getBaseUrl())
                .requestInterceptor(getAuthorizationInterceptor())
                .build();
       // "http://api-gateway"
    }
    @Bean
    public RestClient cashRestClient(RestClient.Builder restClientBuilder) {
        return RestClient.builder()
                .baseUrl("http://localhost:8084")
                .requestInterceptor(getAuthorizationInterceptor())
                .build();
    }
    @Bean
    public RestClient transferRestClient(RestClient.Builder restClientBuilder) {
        return RestClient.builder()
                .baseUrl("http://localhost:8085")
                .requestInterceptor(getAuthorizationInterceptor())
                .build();
    }

    private ClientHttpRequestInterceptor getAuthorizationInterceptor() {
        return (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        oauth2AuthenticationToken.getAuthorizedClientRegistrationId(),
                        oauth2AuthenticationToken.getName());

                OAuth2AccessToken accessToken = authorizedClient != null ? authorizedClient.getAccessToken() : null;

                if (accessToken != null) {
                    request.getHeaders().setBearerAuth(accessToken.getTokenValue());
                }
            }
            return execution.execute(request, body);
        };
    }

}
