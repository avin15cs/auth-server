package io.github.avin15cs.authserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("auth")
public record AuthServerProperties(
        @NotBlank String issuer,
        @NotBlank String jwkSet,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        List<@Valid Client> clients) {

    public AuthServerProperties {
        clients = clients == null ? new ArrayList<>() : List.copyOf(clients);
    }

    public record Client(
            @NotBlank String id,
            String secret,
            @NotBlank String name,
            List<@NotBlank String> redirectUris,
            List<@NotBlank String> postLogoutRedirectUris,
            @NotEmpty List<@NotBlank String> scopes,
            @NotEmpty List<@NotBlank String> grantTypes,
            @NotBlank String audience,
            boolean enabled,
            boolean publicClient) {

        public Client {
            redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
            postLogoutRedirectUris = postLogoutRedirectUris == null ? List.of() : List.copyOf(postLogoutRedirectUris);
            scopes = scopes == null ? List.of() : List.copyOf(scopes);
            grantTypes = grantTypes == null ? List.of() : List.copyOf(grantTypes);
        }
    }
}
