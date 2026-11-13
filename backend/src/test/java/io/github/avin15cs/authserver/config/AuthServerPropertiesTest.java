package io.github.avin15cs.authserver.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthServerPropertiesTest {

    @Test
    void normalizesMissingClientList() {
        AuthServerProperties properties = new AuthServerProperties(
                "https://auth.example.com", "jwk-set", Duration.ofMinutes(10), Duration.ofHours(8), null, null);

        assertThat(properties.clients()).isEmpty();
        assertThat(properties.admin().bootstrapEmails()).isEmpty();
    }
}
