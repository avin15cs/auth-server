# Developer Guide

## Prerequisites

- Java 21
- Docker Desktop or another Docker-compatible engine
- A Google OAuth 2.0 Web application client
- Git
- Node.js 22 LTS and npm

Use the committed Maven wrapper instead of relying on a machine-specific Maven version.

## Local backend setup

1. Start PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

2. Copy `.env.example` to an ignored local environment file or configure the variables
   through the IDE. Never commit populated secrets.

3. Generate a development signing JWK Set:

   ```powershell
   .\mvnw.cmd -q exec:java -Dexec.mainClass=io.github.avin15cs.authserver.tools.JwkGenerator
   ```

4. Register this Google callback for local development:

   ```text
   http://localhost:9000/login/oauth2/code/google
   ```

5. Start the service with the local profile:

   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
   ```

The local issuer is `http://localhost:9000`. Discovery is available at
`http://localhost:9000/.well-known/openid-configuration`.

## Environment variables

| Variable | Purpose | Secret |
| --- | --- | --- |
| `DATABASE_URL` | PostgreSQL JDBC URL | No |
| `DATABASE_USERNAME` | PostgreSQL user | No |
| `DATABASE_PASSWORD` | PostgreSQL password | Yes |
| `DATABASE_POOL_SIZE` | Maximum JDBC pool size | No |
| `AUTH_ISSUER` | Externally visible, exact issuer URL | No |
| `AUTH_JWK_SET` | Private signing JWK Set | Yes |
| `ACCESS_TOKEN_TTL` | Access-token lifetime | No |
| `REFRESH_TOKEN_TTL` | Refresh-token lifetime | No |
| `GOOGLE_CLIENT_ID` | Google Web client identifier | No |
| `GOOGLE_CLIENT_SECRET` | Google Web client secret | Yes |
| `AUTH_ADMIN_EMAILS` | Verified break-glass bootstrap emails | Sensitive config |
| `URL_SHORTENER_CLIENT_ID` | Interactive URL Shortener client ID | No |
| `URL_SHORTENER_CLIENT_SECRET` | Interactive confidential-client secret | Yes |
| `URL_SHORTENER_REDIRECT_URI` | Exact Authorization Code callback | No |
| `URL_SHORTENER_POST_LOGOUT_URI` | Exact post-logout redirect | No |
| `URL_SHORTENER_AUDIENCE` | URL API access-token audience | No |
| `URL_SHORTENER_AUTOMATION_ENABLED` | Enables the service client | No |
| `URL_SHORTENER_AUTOMATION_CLIENT_ID` | Machine-client ID | No |
| `URL_SHORTENER_AUTOMATION_CLIENT_SECRET` | Machine-client secret | Yes |
| `COOKIE_SECURE` | Requires HTTPS for the session cookie | No |

Production must set `COOKIE_SECURE=true` and terminate TLS before requests reach the
application. `AUTH_ISSUER` must exactly match the public HTTPS issuer.

## Tests

Run the complete verification suite:

```powershell
.\mvnw.cmd verify
```

Integration tests use PostgreSQL Testcontainers and therefore require Docker. Tests must
cover migrations, persistent client behavior, token claims, authorization boundaries,
and management mutations rather than mocking away protocol behavior.

## Management API development

The browser first calls `GET /api/session` after Google login. Its response contains the safe
current-user view, the current persisted `admin` decision, and `csrf.headerName` plus
`csrf.token`. Send that header on every `POST`, `PUT`, or other mutating management request.
Do not disable CSRF for local frontend development.

Client endpoints are:

- `GET /api/admin/clients?search=&limit=50&offset=0`
- `GET /api/admin/clients/{id}`
- `POST /api/admin/clients`
- `PUT /api/admin/clients/{id}`
- `PUT /api/admin/clients/{id}/enabled`

Creation accepts an `INTERACTIVE` or `SERVICE` client with explicit grants, scopes, audience,
redirect URIs, and public/confidential status. A confidential creation response contains
`clientSecret` once. Subsequent list, detail, and update responses contain neither plaintext nor
hashed secrets. Local HTTP redirects are accepted only for loopback hosts; an HTTPS issuer makes
HTTPS redirects mandatory.

User listing supports `search`, `limit`, and `offset` query parameters. Role replacement remains
`PUT /api/admin/users/{userId}/roles/{audience}`.

## Database changes

- Add a new versioned migration under `src/main/resources/db/migration/`.
- Never edit an applied migration.
- Keep schema changes backward-compatible with rolling deployment when possible.
- Test migrations against PostgreSQL, not only an in-memory database.
- Flyway is intentionally required because authorization state and audit data must evolve
  deterministically across environments.

## Admin UI development

Install the exact locked dependencies and start Vite from `admin-ui/`:

```powershell
cd admin-ui
npm ci
npm run dev
```

Vite proxies API, OAuth, login, logout, actuator, and discovery paths to port 9000. Keep
the Spring Boot service running there while developing the UI. The browser continues to
use the server-side `AUTH_SESSION` cookie and never stores OAuth tokens in JavaScript.

Run all frontend checks before opening a PR:

```powershell
npm run lint
npm test -- --run
npm run build
```

The production Dockerfile builds the locked frontend with Node 22, copies its static output
into the Spring Boot application, and serves both from the auth-server origin.

The backend contracts required by `ADMIN_UI_REQUIREMENTS.md` are available. Do not use mock
production data in place of them.

## Branch workflow

1. Update local `dev`.
2. Create a focused feature branch.
3. Implement code, tests, migrations, and documentation together.
4. Open a PR targeting `dev` and wait for required checks.
5. Promote `dev` to `main` only through a production promotion PR.
