package io.github.avin15cs.authserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.local-login")
public record LocalLoginProperties(boolean enabled, String username, String password) {

    public LocalLoginProperties {
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
    }
}
