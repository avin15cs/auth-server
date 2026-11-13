# Auth Server

Standalone OAuth 2.0 Authorization Server and OpenID Connect Provider for
`avin15cs` applications. Customers authenticate with Google; applications trust tokens
issued by this service.

## Security model

- Google login only; no local passwords.
- Authorization Code flow with mandatory PKCE.
- Short-lived JWT access tokens and rotated refresh tokens.
- Scoped Client Credentials tokens for confidential machine clients.
- PostgreSQL persistence for clients, grants, consents, and internal users.
- External private JWK configuration with overlapping-key rotation support.
- Exact redirect URI allowlists and one client registration per application.
- Audience-scoped application roles and an audited administrator boundary.

Set `AUTH_ADMIN_EMAILS` to a comma-separated list of verified Google emails that may
bootstrap the first auth-server administrators. Remove entries after durable
administrators have been assigned unless they are intentionally retained for
break-glass access.

## Local development

Requirements: Java 21, Node.js 22, Docker, a Google Web OAuth client, and a private JWK Set. Use the
committed Maven wrapper; a system Maven installation is optional.

```bash
docker compose up -d postgres
cp .env.example .env
# Generate a private JWK Set, then store the output in your local secret configuration:
cd backend
./mvnw -q exec:java -Dexec.mainClass=io.github.avin15cs.authserver.tools.JwkGenerator
# Fill the remaining secrets and export the values.
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The issuer is `http://localhost:9000`. Discovery is available at
`/.well-known/openid-configuration`.

Persisted `AUTH_ADMIN` users can open the management API documentation at
`/swagger-ui.html`. The OpenAPI JSON is available at `/v3/api-docs`; both endpoints require
the same Google-authenticated administrator session as the management API.

For the administration UI, run `npm ci` and `npm run dev` from `admin-ui/`; Vite proxies
same-origin service paths to port 9000. The production Docker image builds and serves the UI
with the backend.

The repository is organized as a monorepo: `backend/` contains the independently buildable
Spring Boot authorization server, while `admin-ui/` contains the independently buildable React
frontend. Root-level files coordinate local infrastructure, CI, documentation, and the combined
production image.

Project documentation:

- [Architecture](docs/ARCHITECTURE.md)
- [Developer guide](docs/DEVELOPER_GUIDE.md)
- [Admin UI requirements](docs/ADMIN_UI_REQUIREMENTS.md)
- [CI/CD pipeline](docs/CI_CD_PIPELINE.md)
- [Operations](docs/OPERATIONS.md)
- [Prioritized roadmap](docs/ROADMAP.md)
- [Engineering log](docs/PROJECT_LOG.md)

## Client integration

The URL Shortener backend should be an OAuth 2.0 confidential client/BFF. It owns the
client secret and session cookie; the React browser must not store access or refresh
tokens. Its API validates the authenticated session and associates every managed URL
with the internal `uid` claim.

### Machine-to-machine access

Enable the independent automation client and set a strong secret. Then request a token:

```bash
curl --user "$URL_SHORTENER_AUTOMATION_CLIENT_ID:$URL_SHORTENER_AUTOMATION_CLIENT_SECRET" \
  --data "grant_type=client_credentials" \
  --data "scope=urls.read" \
  http://localhost:9000/oauth2/token
```

The URL Shortener API must validate the issuer, signature, expiry, audience
`url-shortener-api`, and required `urls.*` scope. Machine tokens contain
`subject_type=client` and do not contain a user `uid`.

## Status

This repository establishes the hardened service foundation. Before an internet-facing
launch, complete the deployment-specific secret manager, TLS ingress, distributed
session strategy, rate limiting, audit export, backup/restore drill, and end-to-end
Google sandbox tests described in the operations runbook.
