package io.github.avin15cs.authserver.config;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

@Component
public class ClientBootstrap implements ApplicationRunner {

    private final RegisteredClientRepository clients;
    private final AuthServerProperties properties;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public ClientBootstrap(RegisteredClientRepository clients, AuthServerProperties properties) {
        this.clients = clients;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        properties.clients().stream()
                .filter(AuthServerProperties.Client::enabled)
                .filter(client -> clients.findByClientId(client.id()) == null)
                .map(this::toRegisteredClient)
                .forEach(clients::save);
    }

    private RegisteredClient toRegisteredClient(AuthServerProperties.Client client) {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.id())
                .clientName(client.name())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(client.grantTypes().contains("authorization_code"))
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(properties.accessTokenTtl())
                        .refreshTokenTimeToLive(properties.refreshTokenTtl())
                        .reuseRefreshTokens(false)
                        .build());

        validateGrantCombination(client);
        client.grantTypes().stream()
                .map(this::toGrantType)
                .forEach(builder::authorizationGrantType);

        if (client.grantTypes().contains("authorization_code") && client.redirectUris().isEmpty()) {
            throw new IllegalStateException(
                    "Authorization Code client '" + client.id() + "' requires at least one redirect URI");
        }

        if (client.publicClient()) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            if (client.secret() == null || client.secret().isBlank()) {
                throw new IllegalStateException("Confidential client '" + client.id() + "' requires a secret");
            }
            builder.clientSecret(passwordEncoder.encode(client.secret()))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }

        client.redirectUris().forEach(builder::redirectUri);
        client.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        client.scopes().forEach(builder::scope);
        return builder.build();
    }

    private void validateGrantCombination(AuthServerProperties.Client client) {
        if (client.publicClient() && client.grantTypes().contains("client_credentials")) {
            throw new IllegalStateException(
                    "Client Credentials client '" + client.id() + "' must be confidential");
        }
        if (client.grantTypes().contains("refresh_token")
                && !client.grantTypes().contains("authorization_code")) {
            throw new IllegalStateException(
                    "Refresh Token grant for client '" + client.id() + "' requires Authorization Code");
        }
    }

    private AuthorizationGrantType toGrantType(String grantType) {
        return switch (grantType) {
            case "authorization_code" -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            default -> throw new IllegalStateException("Unsupported authorization grant type: " + grantType);
        };
    }
}

