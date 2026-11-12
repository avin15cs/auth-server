# Developer Guide

## Prerequisites

- Java 21
- Docker Desktop or another Docker-compatible engine
- A Google OAuth 2.0 Web application client
- Git
- Node.js 22 LTS after the planned `admin-ui/` module is introduced

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

## Database changes

- Add a new versioned migration under `src/main/resources/db/migration/`.
- Never edit an applied migration.
- Keep schema changes backward-compatible with rolling deployment when possible.
- Test migrations against PostgreSQL, not only an in-memory database.
- Flyway is intentionally required because authorization state and audit data must evolve
  deterministically across environments.

## Planned admin UI development

The admin UI belongs in `admin-ui/` in this repository. During development, Vite may run
on a separate port and proxy auth, login, logout, and management paths to port 9000. In
production, build static assets and serve them through the auth-server origin so the UI
uses the server-side `AUTH_SESSION` cookie without storing OAuth tokens in JavaScript.

Before implementing screens, complete the API contracts in
`ADMIN_UI_REQUIREMENTS.md`. Do not use mock production data to hide missing backend
capabilities.

## Branch workflow

1. Update local `dev`.
2. Create a focused feature branch.
3. Implement code, tests, migrations, and documentation together.
4. Open a PR targeting `dev` and wait for required checks.
5. Promote `dev` to `main` only through a production promotion PR.
