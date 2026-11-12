# Auth Server

Standalone OAuth 2.0 Authorization Server and OpenID Connect Provider for
`avin15cs` applications. Customers authenticate with Google; applications trust tokens
issued by this service.

## Security model

- Google login only; no local passwords.
- Authorization Code flow with mandatory PKCE.
- Short-lived JWT access tokens and rotated refresh tokens.
- PostgreSQL persistence for clients, grants, consents, and internal users.
- External private JWK configuration with overlapping-key rotation support.
- Exact redirect URI allowlists and one client registration per application.

## Local development

Requirements: Java 21, Maven, Docker, a Google Web OAuth client, and a private JWK Set.

```bash
docker compose up -d postgres
cp .env.example .env
# Generate a private JWK Set, then store the output in your local secret configuration:
mvn -q exec:java -Dexec.mainClass=io.github.avin15cs.authserver.tools.JwkGenerator
# Fill the remaining secrets and export the values.
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The issuer is `http://localhost:9000`. Discovery is available at
`/.well-known/openid-configuration`.

See [architecture](docs/ARCHITECTURE.md) and [operations](docs/OPERATIONS.md).

## Client integration

The URL Shortener backend should be an OAuth 2.0 confidential client/BFF. It owns the
client secret and session cookie; the React browser must not store access or refresh
tokens. Its API validates the authenticated session and associates every managed URL
with the internal `uid` claim.

## Status

This repository establishes the hardened service foundation. Before an internet-facing
launch, complete the deployment-specific secret manager, TLS ingress, distributed
session strategy, rate limiting, audit export, backup/restore drill, and end-to-end
Google sandbox tests described in the operations runbook.
