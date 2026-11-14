# Architecture

The service is an OpenID Connect Provider and OAuth 2.0 Authorization Server. Google is
an upstream identity provider used only to authenticate a person. Client applications
never receive Google access or refresh tokens.

The repository is a monorepo with two application modules. `backend/` contains the Java/Spring
authorization server and owns all protocol, persistence, management, and audit behavior.
`admin-ui/` contains the React SPA. Root-level Docker and CI configuration assemble and verify
the modules together without creating a second backend.

Local development reuses one container named `postgres-db` across Auth Server and URL Shortener.
The services remain isolated through separate `authserver` and `urlshortener` databases, users,
and Liquibase histories. Production does not share this topology: each service receives its own
database endpoint and credentials through environment configuration.

```text
Browser -> Client application -> Auth Server -> Google
              |                    |
              |                    +-> PostgreSQL (users, clients, grants, consent)
              +-> validates tokens from Auth Server JWKS
```

## Trust boundaries

- Google establishes the external identity using its immutable `sub` claim.
- The auth server maps `(provider, provider_subject)` to its own immutable `uid`.
- The auth server issues its own signed access, ID, and refresh tokens.
- Each application has a distinct client ID, redirect URI allowlist, scopes, and secret.
- Resource servers authorize using `iss`, signature, expiry, audience/client and scopes.
- Machine identities receive `subject_type=client`; people receive `subject_type=user` and `uid`.
- Human roles are scoped to a resource-server audience. `AUTH_ADMIN` governs the auth
  control plane, while application roles such as `URL_ADMIN` are emitted only for that
  application's audience.

## Administration boundary

The auth server owns the protected management API and its audit trail. A separately
built admin frontend consumes that API through the auth-server origin; it never connects
directly to the authorization database. Verified Google accounts listed in
`AUTH_ADMIN_EMAILS` receive the bootstrap `AUTH_ADMIN` role at login. The allowlist is a
break-glass bootstrap mechanism and should be reduced or removed after durable
administrators are assigned.

Role assignments are stored by `(user_id, audience, role)`. User access tokens include
only roles matching that token's audience. Client Credentials tokens remain machine
identities and never receive human roles or a `uid`. The management boundary checks the
persisted `AUTH_ADMIN` assignment on every request so role revocation does not wait for a
browser session to expire.

`GET /api/session` is available to authenticated Google-backed users and returns safe current
user fields, the current persisted administrator decision, and the CSRF header name and token.
All `/api/admin/**` data and mutations require the persisted `AUTH_ADMIN` assignment. Client
management is exposed under `/api/admin/clients`; user search and audience-scoped role
replacement remain under `/api/admin/users`.

Springdoc generates an OpenAPI description for `/api/**` only. Swagger UI at
`/swagger-ui.html` and its `/v3/api-docs` source require the same persisted `AUTH_ADMIN`
decision; OAuth/OIDC protocol endpoints, actuator endpoints, and internal implementation
details are excluded from the generated management contract. Swagger does not create a second
authentication path: it uses the existing same-origin Google session, and mutations still
require the CSRF header and token returned by `GET /api/session`.

Spring Authorization Server's registered-client rows contain protocol configuration and hashed
secrets. `auth_client_metadata` adds the control-plane client type, resource-server audience,
enabled state, origin, actor references, and timestamps. The protocol-facing registered-client
repository fails closed when metadata is absent or disabled. Token customization reads the
persisted audience by internal registration ID, so dynamically registered clients and bootstrap
clients use the same claim path. Bootstrap configuration remains supported, but it is reconciled
into metadata and is not the administration system of record for dynamic clients.

The `admin-ui/` module is a React frontend, not another auth service. It is built
independently during development but packaged into and served through the auth-server origin in
production.
It authenticates with the server-side `AUTH_SESSION` cookie, sends a CSRF token for
mutations, and never stores bearer tokens or generated client secrets in browser storage.
Spring forwards only the explicit SPA routes `/`, `/dashboard`, `/clients`,
`/clients/new`, `/clients/{id}`, and `/users` to the frontend entry point. Protocol,
discovery, management API, and actuator paths are never handled by the SPA fallback.
See `ADMIN_UI_REQUIREMENTS.md` for its API and screen contract.

```text
Google <-- login --> Auth Server <-- same-origin session --> Auth Admin UI
                         |
                         +--> PostgreSQL
                         |
Client applications <---+-- OAuth/OIDC tokens
```

## Supported flows

- Authorization Code with PKCE for interactive users.
- Refresh Token with rotation (`reuseRefreshTokens=false`).
- Client Credentials for confidential machine clients. These tokens have no user identity,
  OpenID scopes, or refresh token.
- OIDC discovery, UserInfo, RP-initiated logout, revocation, introspection, and JWKS.

Implicit and Resource Owner Password Credentials grants are intentionally unsupported.

## Key management

`AUTH_JWK_SET` must contain a private signing JWK with a non-empty `kid`. Production
deployments should inject it from a managed secret store. During rotation, publish both
old and new public keys until every token signed by the old key has expired; use the new
private key for signing. Never commit JWK material.
