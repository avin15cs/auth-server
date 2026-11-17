package io.github.avin15cs.authserver.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.avin15cs.authserver.config.LocalLoginProperties;
import io.github.avin15cs.authserver.config.PasswordLoginProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class PasswordLoginAvailabilityTest {

    @Test
    void supportsProductionInvitationsOrTheLocalBootstrapGate() {
        PasswordLoginProperties disabled = new PasswordLoginProperties(false, Duration.ofHours(24));
        PasswordLoginProperties enabled = new PasswordLoginProperties(true, Duration.ofHours(24));
        LocalLoginProperties localEnabled = new LocalLoginProperties(true, "admin", "password");
        LocalLoginProperties localDisabled = new LocalLoginProperties(false, "admin", "password");
        MockEnvironment localEnvironment = new MockEnvironment();
        localEnvironment.setActiveProfiles("local");

        assertThat(new PasswordLoginAvailability(disabled, localEnabled, new MockEnvironment()).enabled()).isFalse();
        assertThat(new PasswordLoginAvailability(disabled, localEnabled, localEnvironment).enabled()).isTrue();
        assertThat(new PasswordLoginAvailability(disabled, localEnabled, localEnvironment).invitationsEnabled())
                .isFalse();
        assertThat(new PasswordLoginAvailability(enabled, localDisabled, new MockEnvironment()).enabled()).isTrue();
        assertThat(new PasswordLoginAvailability(enabled, localDisabled, new MockEnvironment()).invitationsEnabled())
                .isTrue();
    }
}
