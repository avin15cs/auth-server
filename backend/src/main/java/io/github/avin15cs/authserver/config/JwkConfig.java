package io.github.avin15cs.authserver.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.text.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwkConfig {

    @Bean
    JWKSource<SecurityContext> jwkSource(AuthServerProperties properties) {
        try {
            JWKSet jwkSet = JWKSet.parse(properties.jwkSet());
            boolean hasSigningKey = jwkSet.getKeys().stream().anyMatch(JwkConfig::isPrivateSigningKey);
            if (!hasSigningKey) {
                throw new IllegalStateException("AUTH_JWK_SET must contain at least one private signing key");
            }
            return new ImmutableJWKSet<>(jwkSet);
        } catch (ParseException exception) {
            throw new IllegalStateException("AUTH_JWK_SET is not a valid private JWK Set", exception);
        }
    }

    private static boolean isPrivateSigningKey(JWK key) {
        return key.isPrivate() && key.getKeyID() != null && !key.getKeyID().isBlank();
    }
}

