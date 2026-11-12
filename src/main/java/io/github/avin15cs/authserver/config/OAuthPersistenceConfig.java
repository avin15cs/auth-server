package io.github.avin15cs.authserver.config;

import io.github.avin15cs.authserver.client.ClientMetadataRepository;
import io.github.avin15cs.authserver.client.EnabledRegisteredClientRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class OAuthPersistenceConfig {

    @Bean
    JdbcRegisteredClientRepository jdbcRegisteredClientRepository(JdbcOperations jdbc) {
        return new JdbcRegisteredClientRepository(jdbc);
    }

    @Bean
    @Primary
    RegisteredClientRepository registeredClientRepository(
            JdbcRegisteredClientRepository delegate,
            ClientMetadataRepository metadata) {
        return new EnabledRegisteredClientRepository(delegate, metadata);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbc,
            RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbc, clients);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbc,
            RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
    }
}
