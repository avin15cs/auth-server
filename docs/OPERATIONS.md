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

## Signing-key rotation

1. Add the new private JWK and retain the old public JWK in `AUTH_JWK_SET`.
2. Deploy with the new key selected for signing.
3. Wait longer than the maximum token lifetime plus clock skew.
4. Remove the old key and deploy again.
