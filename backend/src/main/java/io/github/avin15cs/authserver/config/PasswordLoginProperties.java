package io.github.avin15cs.authserver.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.password-login")
public record PasswordLoginProperties(boolean enabled, Duration invitationTtl) {

    public PasswordLoginProperties {
        invitationTtl = invitationTtl == null ? Duration.ofHours(24) : invitationTtl;
        if (invitationTtl.isNegative() || invitationTtl.isZero() || invitationTtl.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Invitation TTL must be between one second and seven days");
        }
    }
}
