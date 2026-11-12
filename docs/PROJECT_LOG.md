# Project Activity and Engineering Log

This log records completed architectural milestones. Planned work remains in
`ROADMAP.md`; it must not be presented here as already delivered.

## Milestone summary

| Milestone | Commit | Status | Deliverables |
| --- | --- | --- | --- |
| Standalone authorization server foundation | `740cf19` | Completed | Spring Authorization Server, Google login, Authorization Code with PKCE, refresh-token rotation, PostgreSQL persistence, Flyway, JDBC sessions, external JWK configuration, Docker and CI. |
| Client Credentials support | `e2b099e` | Completed | Confidential machine client, explicit scopes and audience, `subject_type=client`, token-endpoint integration coverage. |
| Audience-scoped administration foundation | `6ecc0d5` | Completed | `AUTH_ADMIN` bootstrap, audience-specific roles, audited role changes, protected management API, immediate role-revocation enforcement. |
| Documentation baseline | Current development | Completed | Repository instructions, architecture, developer guide, admin UI contract, CI/CD guide, operations, roadmap, and engineering log aligned. |

## Architectural decisions

### Provider-neutral identity boundary

Google authenticates the person, but applications trust only tokens issued by this auth
server. The stable internal user identifier is `uid`; Google tokens are never forwarded
to client applications.

### Separate human and machine identities

Interactive users receive `subject_type=user` and `uid`. Client Credentials tokens
receive `subject_type=client` and `client_id`, with no human identity or roles.

### Audience-scoped authorization

Application roles are stored by user, audience, and role. An auth-platform administrator
uses `AUTH_ADMIN`; URL Shortener administration will use `URL_ADMIN` only for the
`url-shortener-api` audience.

### Persistent protocol state

Registered clients, authorizations, consents, sessions, internal users, roles, and audit
events are stored in PostgreSQL. Flyway owns schema evolution so environments cannot
silently diverge.

### Admin UI deployment model

The planned React admin UI is one frontend for this auth platform. It will be built as a
separate module but served through the auth-server origin in production. The Spring Boot
service remains the only backend and authorization server; there is no second auth
backend.

## Current capability

- Google-backed interactive authentication.
- OIDC discovery, authorization, token, UserInfo, logout, revocation, introspection, and
  JWKS endpoints supplied by Spring Authorization Server.
- Authorization Code with PKCE and rotated refresh tokens.
- Client Credentials for confidential service clients.
- Config-driven client bootstrap.
- Internal users with stable UUIDs.
- Audience-scoped role persistence and token claims.
- Protected user listing and role replacement under `/api/admin/users`.
- Audited bootstrap and administrator role changes.

## Known planned work

- Dynamic client-management API and validation.
- One-time generated client secrets and lifecycle management.
- Complete admin session and CSRF API contract.
- Auth administration frontend.
- Secret rotation and emergency revocation.
- Rate limits, audit export, alerting, and production delivery automation.
