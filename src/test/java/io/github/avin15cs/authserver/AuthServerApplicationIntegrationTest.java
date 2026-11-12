package io.github.avin15cs.authserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Set;
import io.github.avin15cs.authserver.user.AuthUser;
import io.github.avin15cs.authserver.user.AuthUserRepository;
import io.github.avin15cs.authserver.user.AuthUserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
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

    @Autowired
    WebApplicationContext applicationContext;

    @Autowired
    AuthUserRepository users;

    @Autowired
    AuthUserRoleRepository roles;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void startsWithMigratedPersistentStoresAndBootstrappedClient() {
        Integer usersTable = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'auth_user'",
                Integer.class);
        Integer clients = jdbc.queryForObject("""
                SELECT COUNT(*) FROM oauth2_registered_client
                WHERE client_id IN ('url-shortener', 'url-shortener-automation')
                """, Integer.class);
        Integer roleTables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'auth_user_role'",
                Integer.class);

        assertThat(usersTable).isOne();
        assertThat(clients).isEqualTo(2);
        assertThat(roleTables).isOne();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_client_metadata WHERE source = 'BOOTSTRAP'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void protectsManagementApiAndPersistsAuditedBootstrapRole() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(oidcLogin()))
                .andExpect(status().isForbidden());

        AuthUser admin = users.upsertGoogleUser(
                "admin-sub", "admin@example.com", true, "Admin", null);
        assertThat(roles.grantBootstrapAdministrator(admin, java.util.List.of("ADMIN@example.com")))
                .isTrue();
        assertThat(roles.findRoles(admin.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE))
                .containsExactly(AuthUserRoleRepository.AUTH_ADMIN_ROLE);

        mockMvc.perform(get("/api/admin/users")
                        .with(oidcLogin()
                                .idToken(token -> token.subject("admin-sub"))))
                .andExpect(status().isOk());

        roles.replaceRoles(
                admin.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE, java.util.Set.of(), admin.id());
        mockMvc.perform(get("/api/admin/users")
                        .with(oidcLogin()
                                .idToken(token -> token.subject("admin-sub"))))
                .andExpect(status().isForbidden());

        Integer auditEvents = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_event WHERE action = 'BOOTSTRAP_ADMIN_GRANTED'",
                Integer.class);
        assertThat(auditEvents).isOne();
    }

    @Test
    void administratorCanReplaceApplicationRolesButCannotRemoveOwnAdminRole() throws Exception {
        AuthUser admin = users.upsertGoogleUser(
                "role-admin-sub", "role-admin@example.com", true, "Role Admin", null);
        AuthUser target = users.upsertGoogleUser(
                "role-target-sub", "role-target@example.com", true, "Role Target", null);
        roles.grantBootstrapAdministrator(admin, java.util.List.of("role-admin@example.com"));
        var adminLogin = oidcLogin()
                .idToken(token -> token.subject("role-admin-sub"));

        mockMvc.perform(put("/api/admin/users/{userId}/roles/{audience}",
                        target.id(), "url-shortener-api")
                        .with(adminLogin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"URL_ADMIN\"]}"))
                .andExpect(status().isNoContent());

        assertThat(roles.findRoles(target.id(), "url-shortener-api"))
                .containsExactly("URL_ADMIN");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_event
                WHERE action = 'USER_ROLES_REPLACED' AND target_id = ?
                """, Integer.class, target.id().toString())).isOne();

        mockMvc.perform(put("/api/admin/users/{userId}/roles/{audience}",
                        admin.id(), AuthUserRoleRepository.AUTH_SERVER_AUDIENCE)
                        .with(adminLogin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[]}"))
                .andExpect(status().isConflict());
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
        assertThat(jwt.getJWTClaimsSet().getClaim("uid")).isNull();
        assertThat(jwt.getJWTClaimsSet().getClaim("roles")).isNull();
    }

    @Test
    void exposesAuthenticatedSessionAndCsrfContract() throws Exception {
        mockMvc.perform(get("/api/session"))
                .andExpect(status().isUnauthorized());

        AuthUser admin = createAdmin("session-admin-sub", "session-admin@example.com");
        mockMvc.perform(get("/api/session")
                        .with(oidcLogin().idToken(token -> token.subject("session-admin-sub"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(admin.id().toString()))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.csrf.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.csrf.token").isNotEmpty());
    }

    @Test
    void managesServiceClientWithOneTimeHashedSecretDisablementAndAudit() throws Exception {
        createAdmin("client-admin-sub", "client-admin@example.com");
        var login = oidcLogin().idToken(token -> token.subject("client-admin-sub"));
        String request = objectMapper.writeValueAsString(Map.of(
                "clientId", "reporting-service",
                "name", "Reporting Service",
                "clientType", "SERVICE",
                "publicClient", false,
                "grantTypes", Set.of("client_credentials"),
                "redirectUris", Set.of(),
                "postLogoutRedirectUris", Set.of(),
                "scopes", Set.of("reports.read"),
                "audience", "reporting-api"));

        mockMvc.perform(post("/api/admin/clients")
                        .with(login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/admin/clients")
                        .with(login)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client.clientId").value("reporting-service"))
                .andExpect(jsonPath("$.clientSecret").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode creation = objectMapper.readTree(response);
        String id = creation.at("/client/id").asText();
        String secret = creation.path("clientSecret").asText();

        String storedSecret = jdbc.queryForObject(
                "SELECT client_secret FROM oauth2_registered_client WHERE id = ?", String.class, id);
        assertThat(storedSecret).isNotEqualTo(secret);
        assertThat(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches(secret, storedSecret)).isTrue();

        mockMvc.perform(get("/api/admin/clients/{id}", id).with(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
        mockMvc.perform(get("/api/admin/clients").param("search", "reporting").with(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].clientId").value("reporting-service"));

        SignedJWT token = requestClientCredentialsToken("reporting-service", secret, "reports.read", 200);
        assertThat(token.getJWTClaimsSet().getAudience()).containsExactly("reporting-api");
        assertThat(token.getJWTClaimsSet().getStringClaim("subject_type")).isEqualTo("client");
        assertThat(token.getJWTClaimsSet().getClaim("uid")).isNull();
        assertThat(token.getJWTClaimsSet().getClaim("roles")).isNull();
        requestClientCredentialsToken("reporting-service", secret, "reports.write", 400);

        mockMvc.perform(put("/api/admin/clients/{id}/enabled", id)
                        .with(login).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        requestClientCredentialsToken("reporting-service", secret, "reports.read", 401);

        mockMvc.perform(put("/api/admin/clients/{id}/enabled", id)
                        .with(login).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        requestClientCredentialsToken("reporting-service", secret, "reports.read", 200);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_event
                WHERE target_id = ? AND action IN ('CLIENT_CREATED', 'CLIENT_DISABLED', 'CLIENT_ENABLED')
                """, Integer.class, id)).isEqualTo(3);
    }

    @Test
    void validatesClientCombinationsRedirectsScopesAndAuthorization() throws Exception {
        createAdmin("validation-admin-sub", "validation-admin@example.com");
        var adminLogin = oidcLogin().idToken(token -> token.subject("validation-admin-sub"));

        String invalidService = objectMapper.writeValueAsString(Map.of(
                "clientId", "invalid-service",
                "name", "Invalid Service",
                "clientType", "SERVICE",
                "publicClient", false,
                "grantTypes", Set.of("client_credentials"),
                "redirectUris", Set.of(),
                "postLogoutRedirectUris", Set.of(),
                "scopes", Set.of("openid"),
                "audience", "invalid-api"));
        mockMvc.perform(post("/api/admin/clients").with(adminLogin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(invalidService))
                .andExpect(status().isBadRequest());

        String invalidInteractive = objectMapper.writeValueAsString(Map.of(
                "clientId", "invalid-web",
                "name", "Invalid Web",
                "clientType", "INTERACTIVE",
                "publicClient", true,
                "grantTypes", Set.of("refresh_token"),
                "redirectUris", Set.of("https://*.example.com/callback"),
                "postLogoutRedirectUris", Set.of(),
                "scopes", Set.of("profile"),
                "audience", "web-api"));
        mockMvc.perform(post("/api/admin/clients").with(adminLogin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(invalidInteractive))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/clients").with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatesInteractiveClientAndDisabledClientCannotAuthorize() throws Exception {
        createAdmin("web-admin-sub", "web-admin@example.com");
        var login = oidcLogin().idToken(token -> token.subject("web-admin-sub"));
        String create = objectMapper.writeValueAsString(Map.of(
                "clientId", "dashboard-web",
                "name", "Dashboard Web",
                "clientType", "INTERACTIVE",
                "publicClient", true,
                "grantTypes", Set.of("authorization_code", "refresh_token"),
                "redirectUris", Set.of("http://localhost:4200/callback"),
                "postLogoutRedirectUris", Set.of("http://localhost:4200/"),
                "scopes", Set.of("openid", "profile", "dashboard.read"),
                "audience", "dashboard-api"));
        String creation = mockMvc.perform(post("/api/admin/clients").with(login).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(creation).at("/client/id").asText();

        String update = objectMapper.writeValueAsString(Map.of(
                "name", "Dashboard Application",
                "grantTypes", Set.of("authorization_code"),
                "redirectUris", Set.of("http://localhost:4200/callback"),
                "postLogoutRedirectUris", Set.of("http://localhost:4200/"),
                "scopes", Set.of("openid", "dashboard.read"),
                "audience", "dashboard-v2-api"));
        mockMvc.perform(put("/api/admin/clients/{id}", id).with(login).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dashboard Application"))
                .andExpect(jsonPath("$.audience").value("dashboard-v2-api"));

        mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "dashboard-web")
                        .queryParam("redirect_uri", "http://localhost:4200/callback")
                        .queryParam("scope", "openid dashboard.read")
                        .queryParam("state", "enabled-state")
                        .queryParam("code_challenge", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG")
                        .queryParam("code_challenge_method", "S256")
                        .with(login))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(put("/api/admin/clients/{id}/enabled", id).with(login).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "dashboard-web")
                        .queryParam("redirect_uri", "http://localhost:4200/callback")
                        .queryParam("scope", "openid dashboard.read")
                        .queryParam("state", "test-state")
                        .queryParam("code_challenge", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG")
                        .queryParam("code_challenge_method", "S256")
                        .with(login))
                .andExpect(status().is4xxClientError());

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_event
                WHERE target_id = ? AND action IN ('CLIENT_CREATED', 'CLIENT_UPDATED', 'CLIENT_DISABLED')
                """, Integer.class, id)).isEqualTo(3);
    }

    private AuthUser createAdmin(String subject, String email) {
        AuthUser admin = users.upsertGoogleUser(subject, email, true, "Admin", null);
        roles.grantBootstrapAdministrator(admin, java.util.List.of(email));
        return admin;
    }

    private SignedJWT requestClientCredentialsToken(
            String clientId, String secret, String scope, int expectedStatus) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=client_credentials&scope=" + scope))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        if (expectedStatus != 200) {
            return null;
        }
        return SignedJWT.parse((String) JSONObjectUtils.parse(response.body()).get("access_token"));
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
