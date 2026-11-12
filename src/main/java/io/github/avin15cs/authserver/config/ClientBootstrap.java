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
                .filter(client -> clients.findByClientId(client.id()) == null)
                .map(this::toRegisteredClient)
                .forEach(clients::save);
    }

    private RegisteredClient toRegisteredClient(AuthServerProperties.Client client) {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.id())
                .clientName(client.name())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(properties.accessTokenTtl())
                        .refreshTokenTimeToLive(properties.refreshTokenTtl())
                        .reuseRefreshTokens(false)
                        .build());

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
}

