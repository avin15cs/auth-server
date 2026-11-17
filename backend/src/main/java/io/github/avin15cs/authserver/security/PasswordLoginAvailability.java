package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.config.LocalLoginProperties;
import io.github.avin15cs.authserver.config.PasswordLoginProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class PasswordLoginAvailability {

    private final PasswordLoginProperties passwordLogin;
    private final LocalLoginProperties localLogin;
    private final Environment environment;

    public PasswordLoginAvailability(
            PasswordLoginProperties passwordLogin,
            LocalLoginProperties localLogin,
            Environment environment) {
        this.passwordLogin = passwordLogin;
        this.localLogin = localLogin;
        this.environment = environment;
    }

    public boolean enabled() {
        return passwordLogin.enabled() || localBootstrapEnabled();
    }

    public boolean invitationsEnabled() {
        return passwordLogin.enabled();
    }

    public boolean localBootstrapEnabled() {
        return localLogin.enabled() && environment.acceptsProfiles(Profiles.of("local"));
    }
}
