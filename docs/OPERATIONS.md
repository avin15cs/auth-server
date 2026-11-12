# Operations

## Required secrets

- `GOOGLE_CLIENT_SECRET`
- `URL_SHORTENER_CLIENT_SECRET`
- `DATABASE_PASSWORD`
- `AUTH_JWK_SET` containing the private signing key
- `URL_SHORTENER_AUTOMATION_CLIENT_SECRET` when
  `URL_SHORTENER_AUTOMATION_ENABLED=true`

Supply secrets through the deployment platform's secret manager. Do not put them in
environment files in source control, container images, logs, or CI variables visible to
forked pull requests.

## Google configuration

Create a Web application OAuth client and register this exact callback:

```text
https://<auth-host>/login/oauth2/code/google
```

For local development, also register:

```text
http://localhost:9000/login/oauth2/code/google
```

Only `openid`, `profile`, and `email` are requested.

## Bootstrap administrator

Set `AUTH_ADMIN_EMAILS` to one or more comma-separated, verified Google email addresses.
At the next successful login, each matching account receives `AUTH_ADMIN` for the
`auth-server` audience and the grant is written to `admin_audit_event`. The comparison is
case-insensitive. Treat this setting as break-glass configuration: keep it in deployment
configuration rather than source control and remove entries that should no longer regain
administrator access on login.

The management endpoints under `/api/admin/**` require the session authority
`ROLE_AUTH_ADMIN`. Role changes replace the target user's role set for one audience and
are audited. An administrator cannot remove their own `AUTH_ADMIN` role through the API.

## Health and rollout

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- Run Flyway before accepting traffic.
- Browser sessions are stored in PostgreSQL, so multiple instances can run behind TLS
  without sticky sessions. Use rolling deployments and keep session schema compatible.

## Machine clients

Give each workload its own client ID and secret, the minimum required scopes, and an
explicit resource-server audience. Do not reuse the interactive URL Shortener client
secret or enable OpenID scopes for Client Credentials clients.

Create new clients through the authenticated management API. Confidential-client plaintext
secrets are available only in the creation response; transfer them directly to the workload's
secret manager and explicitly complete the UI acknowledgement. The database stores only the
password hash, so a lost secret cannot be recovered and requires the planned credential-rotation
capability. Disabling a client removes it from protocol lookup immediately, preventing new
authorization and token requests; already-issued self-contained access tokens remain valid until
expiry and resource servers must continue enforcing their short lifetime.

The configured URL Shortener clients remain bootstrap registrations. Their deployment secrets
still come from runtime secret configuration, while their audience and enabled state are
reconciled into `auth_client_metadata` at startup. Prefer the management API for new clients.

## Signing-key rotation

1. Add the new private JWK and retain the old public JWK in `AUTH_JWK_SET`.
2. Deploy with the new key selected for signing.
3. Wait longer than the maximum token lifetime plus clock skew.
4. Remove the old key and deploy again.
