package com.alex.bank.transfer.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }


    private ClientHttpRequestInterceptor createTokenInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return (request, body, execution) -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("transfer-service")
                    .principal("transfer-service")
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient == null) {
                throw new IllegalStateException("No authorized client for registration 'transfer-service'");
            }

            request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestClient accountsRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            RestClient.Builder restClientBuilder,
            TransferServicePropertiesConfig transferServicePropertiesConfig) {
        return restClientBuilder
                .baseUrl(transferServicePropertiesConfig.getAccountService().getBaseUrl())
                .requestInterceptor(createTokenInterceptor(authorizedClientManager))
                .build();
    }


    @Bean
    public RestClient notificationRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            RestClient.Builder restClientBuilder,
            TransferServicePropertiesConfig transferServicePropertiesConfig
    ) {
        return restClientBuilder
                .baseUrl(transferServicePropertiesConfig.getNotificationService().getBaseUrl())
                .requestInterceptor(createTokenInterceptor(authorizedClientManager))
                .build();
    }
}