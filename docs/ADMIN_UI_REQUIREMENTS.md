# Auth Administration UI Requirements

## Goal

Provide one secure administration frontend for the standalone auth server. An
`AUTH_ADMIN` must be able to register applications, create machine clients, manage
audience-scoped user roles, and review safe operational information without direct
database access.

The admin UI is not an authorization server and not a second backend. It is a React SPA
module in this repository that calls the Spring Boot management API.

## Deployment model

```text
Browser
  |
  | https://auth.example.com/
  v
Auth Admin UI (React static assets)
  |
  | same-origin AUTH_SESSION cookie + CSRF header
  v
Spring Boot auth-server
  |-- /api/admin/**       management API
  |-- /oauth2/**          OAuth endpoints
  |-- /.well-known/**     discovery
  |-- /login/**           Google login callback
  `-- PostgreSQL          protocol and administration state
```

Development may use a Vite proxy, but production must remain same-origin initially.

## Required delivery order

### 1. Complete the management backend

Implement and integration-test these capabilities before depending on them in the UI:

- Admin session endpoint returning the current internal user, `AUTH_ADMIN` status, and a
  CSRF token suitable for mutating requests.
- Paginated client listing and safe client details. Never return stored secret hashes.
- Client creation for:
  - Authorization Code with PKCE and optional Refresh Token.
  - Client Credentials for confidential machine clients.
- Cryptographically secure client-secret generation. Return plaintext only once and
  persist only a password hash.
- Strict validation of client ID, client type, exact HTTPS redirect URIs in production,
  post-logout URIs, allowed grants, scopes, and audience.
- Enable and disable behavior that is enforced by token and authorization endpoints, not
  merely displayed as metadata.
- Audited create, update, enable, disable, role assignment, and future rotation actions.
- Existing paginated user listing and audience-specific role replacement.

Do not overload the configured bootstrap clients as the long-term management model.
Persist dynamic client metadata, including audience and enabled state, and make token
customization read from persisted registration metadata.

### 2. Build the frontend module

Create `admin-ui/` using React, TypeScript, Vite, and an exact lockfile. Use accessible,
responsive components and a coherent dark/light design system. Avoid unnecessary UI
framework weight unless the repository explicitly adopts one.

Required routes:

- `/` or `/dashboard`: platform summary and safe health/status information.
- `/clients`: searchable client list with type, audience, grants, status, and creation
  metadata.
- `/clients/new`: guided application or service-client registration.
- `/clients/:id`: safe details, configuration updates, enable/disable controls, and later
  rotation entry points.
- `/users`: paginated user search and audience-specific role management.

Required UX behavior:

- Unauthenticated users are sent through the auth server's Google login.
- Authenticated non-admin users receive a clear forbidden screen and cannot load admin
  data.
- Generated client secrets appear in a one-time confirmation view with copy and explicit
  acknowledgement. They must not be written to logs, URLs, analytics, local storage,
  session storage, or persistent frontend state.
- Mutations include the server-provided CSRF token.
- API errors are presented safely without leaking stack traces or sensitive payloads.
- Destructive actions such as disabling a client require confirmation.

## Client registration rules

### Interactive application

- Uses Authorization Code with PKCE.
- May use Refresh Token only together with Authorization Code.
- Requires exact redirect URIs.
- Requests `openid` only when it is an OIDC client.
- Has an explicit API audience and least-privilege scopes.
- Public clients do not receive a client secret; confidential clients do.

### Service application

- Uses Client Credentials only.
- Must be confidential and receive a generated secret.
- Cannot request `openid`, `profile`, `email`, a refresh token, or user roles.
- Has explicit machine scopes and an audience.

## Security acceptance criteria

- Every management endpoint enforces the persisted `AUTH_ADMIN` assignment.
- Revoking `AUTH_ADMIN` takes effect without waiting for the browser session to expire.
- CSRF remains enabled for cookie-authenticated mutations.
- Client secrets are hashed at rest and returned only once.
- Disabled clients cannot authorize or obtain tokens.
- Redirect URIs use exact matching; wildcards are rejected.
- Human roles are emitted only for the access-token audience.
- Client Credentials tokens contain no `uid` or human role claims.
- Every privileged mutation creates an audit event with actor, action, target, and time.

## Testing acceptance criteria

Backend integration tests must use PostgreSQL Testcontainers and cover successful and
rejected client lifecycle operations, one-time secret behavior, disabled clients, scope
rules, redirect validation, role authorization, CSRF, and audit records.

Frontend tests must cover login/forbidden states, client form validation, one-time secret
handling, role changes, API failures, and keyboard-accessible confirmation flows. CI must
run lint, tests, and a production build.

## Out of scope for the first UI slice

- Local username/password authentication.
- Social providers other than Google.
- Multi-tenant organization administration.
- Direct database administration.
- Browser storage of bearer or refresh tokens.
- Production deployment automation before a hosting platform and secret manager are
  selected.
