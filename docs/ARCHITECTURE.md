# Architecture

The service is an OpenID Connect Provider and OAuth 2.0 Authorization Server. Google is
an upstream identity provider used only to authenticate a person. Client applications
never receive Google access or refresh tokens.

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

## Supported flows

- Authorization Code with PKCE for interactive users.
- Refresh Token with rotation (`reuseRefreshTokens=false`).
- OIDC discovery, UserInfo, RP-initiated logout, revocation, introspection, and JWKS.

Implicit and Resource Owner Password Credentials grants are intentionally unsupported.

## Key management

`AUTH_JWK_SET` must contain a private signing JWK with a non-empty `kid`. Production
deployments should inject it from a managed secret store. During rotation, publish both
old and new public keys until every token signed by the old key has expired; use the new
private key for signing. Never commit JWK material.

