package io.github.avin15cs.authserver.security;

import io.github.avin15cs.authserver.config.LocalLoginProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class LocalLoginAvailability {

    private final LocalLoginProperties properties;
    private final Environment environment;

    public LocalLoginAvailability(LocalLoginProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public boolean enabled() {
        return properties.enabled() && environment.acceptsProfiles(Profiles.of("local"));
    }
}
