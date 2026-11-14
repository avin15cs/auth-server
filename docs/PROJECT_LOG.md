# Project Activity and Engineering Log

This log records completed architectural milestones. Planned work remains in
`ROADMAP.md`; it must not be presented here as already delivered.

## Milestone summary

| Milestone | Commit | Status | Deliverables |
| --- | --- | --- | --- |
| Standalone authorization server foundation | `740cf19` | Completed | Spring Authorization Server, Google login, Authorization Code with PKCE, refresh-token rotation, PostgreSQL persistence, database migrations, JDBC sessions, external JWK configuration, Docker and CI. |
| Client Credentials support | `e2b099e` | Completed | Confidential machine client, explicit scopes and audience, `subject_type=client`, token-endpoint integration coverage. |
| Audience-scoped administration foundation | `6ecc0d5` | Completed | `AUTH_ADMIN` bootstrap, audience-specific roles, audited role changes, protected management API, immediate role-revocation enforcement. |
| Managed OAuth client lifecycle | Current development | Completed | Session and CSRF contract, paginated client management, generated one-time secrets, persisted audiences and enabled state, protocol-level disablement, audit events, and PostgreSQL lifecycle integration tests. |
| Auth administration UI | Current development | Completed | Same-origin React administration SPA, Google login states, dashboard, client lifecycle screens, one-time secret handling, user-role management, responsive accessible styling, frontend tests, and production container integration. |
| Monorepo module boundary | Current development | Completed | Java and Maven backend isolated under `backend/`, React frontend retained under `admin-ui/`, and root-level CI, Docker, documentation, and local infrastructure updated to orchestrate both modules. |
| Protected management API documentation | Current development | Completed | Springdoc OpenAPI contract and Swagger UI for session and management endpoints, restricted to persisted `AUTH_ADMIN` sessions without relaxing CSRF or exposing OAuth protocol internals. |
| Liquibase migration baseline | Current development | Completed | Pre-production Flyway history replaced with four ordered Liquibase formatted-SQL changesets, a single master changelog, PostgreSQL integration coverage, and documented one-time local database reset. |
| Unified local development command | Current development | Completed | Root npm runner loads ignored local configuration and starts the Maven backend plus Vite administration UI together with labeled output and coordinated shutdown. |
| Shared local PostgreSQL bootstrap | Current development | Completed | Auth Server and URL Shortener reuse the `postgres-db` container while retaining separate databases, roles, and Liquibase histories; either repository can idempotently create the shared server and its owned database. |
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
events are stored in PostgreSQL. Liquibase owns schema evolution so environments cannot
silently diverge.

### Admin UI deployment model

The React admin UI is one frontend for this auth platform. It is built as a separate
module but served through the auth-server origin in production. The Spring Boot
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
- Authenticated session and CSRF discovery under `/api/session`.
- Protected dynamic client registration, safe inspection, update, enable, and disable under
  `/api/admin/clients`.
- Generated confidential-client secrets that are returned once and stored only as password
  hashes.
- Persisted client audience and enabled metadata used by token customization and protocol
  endpoint lookup.
- Same-origin administration UI for session state, safe health information, client
  registration and lifecycle management, and audience-specific user roles.
- One-time confidential-client secret display held only in component memory until the
  administrator acknowledges it.

## Known planned work

- Secret rotation and emergency revocation.
- Rate limits, audit export, alerting, and production delivery automation.
