package io.github.avin15cs.authserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class AuthServerApplicationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15.19-alpine3.24");

    private static final String JWK_SET = generateJwkSet();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-google-client");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-google-secret");
        registry.add("auth.jwk-set", () -> JWK_SET);
        registry.add("auth.clients[0].id", () -> "url-shortener");
        registry.add("auth.clients[0].name", () -> "URL Shortener");
        registry.add("auth.clients[0].secret", () -> "test-client-secret-with-sufficient-entropy");
        registry.add("auth.clients[0].redirect-uris[0]", () -> "http://localhost/callback");
        registry.add("auth.clients[0].post-logout-redirect-uris[0]", () -> "http://localhost/");
        registry.add("auth.clients[0].scopes[0]", () -> "openid");
        registry.add("auth.clients[0].scopes[1]", () -> "profile");
        registry.add("auth.clients[0].public-client", () -> "false");
    }

    @Autowired
    JdbcOperations jdbc;

    @Test
    void startsWithMigratedPersistentStoresAndBootstrappedClient() {
        Integer usersTable = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'auth_user'",
                Integer.class);
        Integer clients = jdbc.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = 'url-shortener'",
                Integer.class);

        assertThat(usersTable).isOne();
        assertThat(clients).isOne();
    }

    private static String generateJwkSet() {
        try {
            return new JWKSet(new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate()).toString(false);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
