package io.github.avin15cs.authserver.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.avin15cs.authserver.admin.AdminAuditRepository;
import io.github.avin15cs.authserver.config.AuthServerProperties;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientManagementService {

    private static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{2,99}");
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,199}");
    private static final Pattern SCOPE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");
    private static final Set<String> INTERACTIVE_GRANTS = Set.of("authorization_code", "refresh_token");
    private static final Set<String> SERVICE_GRANTS = Set.of("client_credentials");
    private static final Set<String> USER_SCOPES = Set.of("openid", "profile", "email");

    private final JdbcRegisteredClientRepository clients;
    private final ClientMetadataRepository metadata;
    private final AdminAuditRepository audit;
    private final AuthServerProperties properties;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientManagementService(
            JdbcRegisteredClientRepository clients,
            ClientMetadataRepository metadata,
            AdminAuditRepository audit,
            AuthServerProperties properties) {
        this.clients = clients;
        this.metadata = metadata;
        this.audit = audit;
        this.properties = properties;
    }

    public ClientPage list(String search, int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0) {
            throw badRequest("Invalid pagination");
        }
        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw badRequest("Search is too long");
        }
        List<ClientView> content = metadata.findPage(normalizedSearch, limit, offset).stream()
                .map(this::requireView)
                .toList();
        return new ClientPage(content, metadata.count(normalizedSearch), limit, offset);
    }

    public ClientView get(String id) {
        return requireView(id);
    }

    @Transactional
    public ClientCreation create(CreateCommand command, UUID actorUserId) {
        validate(command.clientId(), command.name(), command.clientType(), command.publicClient(),
                command.grantTypes(), command.redirectUris(), command.postLogoutRedirectUris(),
                command.scopes(), command.audience());
        if (clients.findByClientId(command.clientId()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client ID already exists");
        }

        String plaintextSecret = command.publicClient() ? null : generateSecret();
        RegisteredClient registeredClient = build(
                UUID.randomUUID().toString(), command.clientId(), command.name(), command.publicClient(),
                command.grantTypes(), command.redirectUris(), command.postLogoutRedirectUris(),
                command.scopes(), plaintextSecret == null ? null : passwordEncoder.encode(plaintextSecret), null);
        clients.save(registeredClient);
        metadata.insertDynamic(registeredClient.getId(), command.clientType(), command.audience(), actorUserId);
        audit.record(actorUserId, "CLIENT_CREATED", "OAUTH_CLIENT", registeredClient.getId(),
                "client_id=" + command.clientId() + ",type=" + command.clientType()
                        + ",audience=" + command.audience());
        return new ClientCreation(toView(registeredClient, metadata.findByRegisteredClientId(registeredClient.getId()).orElseThrow()),
                plaintextSecret);
    }

    @Transactional
    public ClientView update(String id, UpdateCommand command, UUID actorUserId) {
        RegisteredClient existing = requireClient(id);
        ClientMetadata currentMetadata = requireMetadata(id);
        validate(existing.getClientId(), command.name(), currentMetadata.clientType(), isPublic(existing),
                command.grantTypes(), command.redirectUris(), command.postLogoutRedirectUris(),
                command.scopes(), command.audience());

        RegisteredClient updated = build(
                existing.getId(), existing.getClientId(), command.name(), isPublic(existing),
                command.grantTypes(), command.redirectUris(), command.postLogoutRedirectUris(),
                command.scopes(), existing.getClientSecret(), existing);
        clients.save(updated);
        metadata.update(id, command.audience(), actorUserId);
        audit.record(actorUserId, "CLIENT_UPDATED", "OAUTH_CLIENT", id,
                "client_id=" + existing.getClientId() + ",audience=" + command.audience());
        return requireView(id);
    }

    @Transactional
    public ClientView setEnabled(String id, boolean enabled, UUID actorUserId) {
        RegisteredClient client = requireClient(id);
        requireMetadata(id);
        if (metadata.setEnabled(id, enabled, actorUserId)) {
            audit.record(actorUserId, enabled ? "CLIENT_ENABLED" : "CLIENT_DISABLED", "OAUTH_CLIENT", id,
                    "client_id=" + client.getClientId());
        }
        return requireView(id);
    }

    private RegisteredClient build(
            String id,
            String clientId,
            String name,
            boolean publicClient,
            Set<String> grants,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String storedSecret,
            RegisteredClient existing) {
        RegisteredClient.Builder builder = RegisteredClient.withId(id)
                .clientId(clientId)
                .clientName(name.trim())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(grants.contains("authorization_code"))
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(properties.accessTokenTtl())
                        .refreshTokenTimeToLive(properties.refreshTokenTtl())
                        .reuseRefreshTokens(false)
                        .build());
        if (existing != null) {
            builder.clientIdIssuedAt(existing.getClientIdIssuedAt())
                    .clientSecretExpiresAt(existing.getClientSecretExpiresAt());
        }
        if (publicClient) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientSecret(storedSecret);
        }
        grants.stream().map(this::grantType).forEach(builder::authorizationGrantType);
        redirectUris.forEach(builder::redirectUri);
        postLogoutRedirectUris.forEach(builder::postLogoutRedirectUri);
        scopes.forEach(builder::scope);
        return builder.build();
    }

    private void validate(
            String clientId,
            String name,
            ClientType type,
            boolean publicClient,
            Set<String> grants,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String audience) {
        if (clientId == null || !CLIENT_ID.matcher(clientId).matches()) {
            throw badRequest("Invalid client ID");
        }
        if (type == null) {
            throw badRequest("Client type is required");
        }
        if (name == null || name.isBlank() || name.length() > 200) {
            throw badRequest("Invalid client name");
        }
        if (audience == null || !VALUE.matcher(audience).matches()) {
            throw badRequest("Invalid audience");
        }
        if (grants == null || grants.isEmpty() || scopes == null || scopes.isEmpty() || scopes.size() > 50
                || scopes.stream().anyMatch(scope -> scope == null || !SCOPE.matcher(scope).matches())) {
            throw badRequest("Invalid grants or scopes");
        }
        if (redirectUris == null || postLogoutRedirectUris == null
                || redirectUris.size() > 20 || postLogoutRedirectUris.size() > 20) {
            throw badRequest("Invalid redirect URI collection");
        }

        if (type == ClientType.SERVICE) {
            if (publicClient || !grants.equals(SERVICE_GRANTS) || !redirectUris.isEmpty()
                    || !postLogoutRedirectUris.isEmpty() || !disjoint(scopes, USER_SCOPES)) {
                throw badRequest("Service clients must be confidential, use only Client Credentials, and exclude user scopes");
            }
        } else {
            if (!grants.contains("authorization_code") || !INTERACTIVE_GRANTS.containsAll(grants)
                    || redirectUris.isEmpty()) {
                throw badRequest("Interactive clients require Authorization Code and exact redirect URIs");
            }
            if ((scopes.contains("profile") || scopes.contains("email")) && !scopes.contains("openid")) {
                throw badRequest("profile and email scopes require openid");
            }
        }
        redirectUris.forEach(uri -> validateUri(uri, "redirect URI"));
        postLogoutRedirectUris.forEach(uri -> validateUri(uri, "post-logout redirect URI"));
    }

    private void validateUri(String value, String label) {
        try {
            if (value == null || value.contains("*") || value.length() > 1000) {
                throw badRequest("Invalid " + label);
            }
            URI uri = URI.create(value);
            boolean production = "https".equalsIgnoreCase(URI.create(properties.issuer()).getScheme());
            boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost())
                    || "[::1]".equals(uri.getHost()) || "::1".equals(uri.getHost()));
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || (production && !"https".equalsIgnoreCase(uri.getScheme()))
                    || (!production && !"https".equalsIgnoreCase(uri.getScheme()) && !localHttp)) {
                throw badRequest("Invalid " + label);
            }
        } catch (IllegalArgumentException exception) {
            throw badRequest("Invalid " + label);
        }
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        return left.stream().noneMatch(right::contains);
    }

    private AuthorizationGrantType grantType(String value) {
        return switch (value) {
            case "authorization_code" -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            default -> throw badRequest("Unsupported grant type");
        };
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ClientView requireView(String id) {
        return toView(requireClient(id), requireMetadata(id));
    }

    private RegisteredClient requireClient(String id) {
        if (id == null || id.length() > 100) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        RegisteredClient client = clients.findById(id);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return client;
    }

    private ClientMetadata requireMetadata(String id) {
        return metadata.findByRegisteredClientId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    private ClientView toView(RegisteredClient client, ClientMetadata details) {
        Set<String> grants = new LinkedHashSet<>();
        client.getAuthorizationGrantTypes().forEach(grant -> grants.add(grant.getValue()));
        Set<String> authenticationMethods = new LinkedHashSet<>();
        client.getClientAuthenticationMethods().forEach(method -> authenticationMethods.add(method.getValue()));
        return new ClientView(
                client.getId(), client.getClientId(), client.getClientName(), details.clientType(),
                isPublic(client), details.audience(), details.enabled(), grants,
                Set.copyOf(client.getRedirectUris()), Set.copyOf(client.getPostLogoutRedirectUris()),
                Set.copyOf(client.getScopes()), authenticationMethods, client.getClientIdIssuedAt(),
                details.createdAt(), details.updatedAt());
    }

    private static boolean isPublic(RegisteredClient client) {
        return client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record CreateCommand(
            String clientId,
            String name,
            ClientType clientType,
            boolean publicClient,
            Set<String> grantTypes,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String audience) {
        public CreateCommand {
            grantTypes = immutable(grantTypes);
            redirectUris = immutable(redirectUris);
            postLogoutRedirectUris = immutable(postLogoutRedirectUris);
            scopes = immutable(scopes);
        }
    }

    public record UpdateCommand(
            String name,
            Set<String> grantTypes,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            String audience) {
        public UpdateCommand {
            grantTypes = immutable(grantTypes);
            redirectUris = immutable(redirectUris);
            postLogoutRedirectUris = immutable(postLogoutRedirectUris);
            scopes = immutable(scopes);
        }
    }

    private static <T> Set<T> immutable(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    public record ClientPage(List<ClientView> content, long total, int limit, int offset) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientCreation(ClientView client, String clientSecret) {
    }

    public record ClientView(
            String id,
            String clientId,
            String name,
            ClientType clientType,
            boolean publicClient,
            String audience,
            boolean enabled,
            Set<String> grantTypes,
            Set<String> redirectUris,
            Set<String> postLogoutRedirectUris,
            Set<String> scopes,
            Set<String> authenticationMethods,
            Instant clientIdIssuedAt,
            Instant createdAt,
            Instant updatedAt) {
    }
}
