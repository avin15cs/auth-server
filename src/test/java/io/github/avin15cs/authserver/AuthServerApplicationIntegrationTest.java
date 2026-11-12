package io.github.avin15cs.authserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServerApplicationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15.19-alpine3.24");

    private static final String JWK_SET = generateJwkSet();

    @LocalServerPort
    int port;

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
        registry.add("auth.clients[0].enabled", () -> "true");
        registry.add("auth.clients[0].redirect-uris[0]", () -> "http://localhost/callback");
        registry.add("auth.clients[0].post-logout-redirect-uris[0]", () -> "http://localhost/");
        registry.add("auth.clients[0].scopes[0]", () -> "openid");
        registry.add("auth.clients[0].scopes[1]", () -> "profile");
        registry.add("auth.clients[0].grant-types[0]", () -> "authorization_code");
        registry.add("auth.clients[0].grant-types[1]", () -> "refresh_token");
        registry.add("auth.clients[0].audience", () -> "url-shortener-api");
        registry.add("auth.clients[0].public-client", () -> "false");
        registry.add("auth.clients[1].id", () -> "url-shortener-automation");
        registry.add("auth.clients[1].name", () -> "URL Shortener Automation");
        registry.add("auth.clients[1].secret", () -> "test-automation-secret-with-sufficient-entropy");
        registry.add("auth.clients[1].enabled", () -> "true");
        registry.add("auth.clients[1].scopes[0]", () -> "urls.read");
        registry.add("auth.clients[1].scopes[1]", () -> "urls.write");
        registry.add("auth.clients[1].grant-types[0]", () -> "client_credentials");
        registry.add("auth.clients[1].audience", () -> "url-shortener-api");
        registry.add("auth.clients[1].public-client", () -> "false");
    }

    @Autowired
    JdbcOperations jdbc;

    @Test
    void startsWithMigratedPersistentStoresAndBootstrappedClient() {
        Integer usersTable = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'auth_user'",
                Integer.class);
        Integer clients = jdbc.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client",
                Integer.class);

        assertThat(usersTable).isOne();
        assertThat(clients).isEqualTo(2);
    }

    @Test
    void issuesScopedClientCredentialsToken() throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                "url-shortener-automation:test-automation-secret-with-sufficient-entropy"
                        .getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=client_credentials&scope=urls.read"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String accessToken = (String) JSONObjectUtils.parse(response.body()).get("access_token");
        SignedJWT jwt = SignedJWT.parse(accessToken);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("url-shortener-automation");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("url-shortener-api");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("subject_type")).isEqualTo("client");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("client_id"))
                .isEqualTo("url-shortener-automation");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("scope")).containsExactly("urls.read");
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
