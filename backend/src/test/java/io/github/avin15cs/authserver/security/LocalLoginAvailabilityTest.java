package io.github.avin15cs.authserver.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.avin15cs.authserver.config.LocalLoginProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LocalLoginAvailabilityTest {

    @Test
    void requiresBothTheFlagAndLocalProfile() {
        LocalLoginProperties enabled = new LocalLoginProperties(true, "admin", "password");
        MockEnvironment localEnvironment = new MockEnvironment();
        localEnvironment.setActiveProfiles("local");

        assertThat(new LocalLoginAvailability(enabled, new MockEnvironment()).enabled()).isFalse();
        assertThat(new LocalLoginAvailability(enabled, localEnvironment).enabled()).isTrue();
        assertThat(new LocalLoginAvailability(
                new LocalLoginProperties(false, "admin", "password"),
                localEnvironment).enabled()).isFalse();
    }
}
