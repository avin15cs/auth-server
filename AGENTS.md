# Agent Context and Project Instructions

> Read this file and the documents under `docs/` before changing the repository.

## Repository and architecture

- Repository: `https://github.com/avin15cs/auth-server`
- Purpose: standalone OAuth 2.0 Authorization Server and OpenID Connect Provider.
- Backend: Java 21, Spring Boot 4.1.2, Spring Authorization Server, JDBC, Liquibase,
  PostgreSQL 15, PostgreSQL-backed Spring Session, Maven wrapper.
- Upstream login: Google only. Keep the tokens issued to applications provider-neutral;
  Google access and refresh tokens must never leave this service.
- Supported grants: Authorization Code with PKCE, Refresh Token rotation, and Client
  Credentials for confidential service clients.
- Planned admin frontend: React, TypeScript, and Vite under `admin-ui/`, served through
  the auth-server origin in production. It is not a separate authorization service.

## Security invariants

- Never commit OAuth client secrets, database credentials, signing private keys, tokens,
  session identifiers, or user data.
- Store confidential client secrets as password hashes and display generated plaintext
  only once at creation or rotation.
- Use immutable internal `uid` values for user ownership. Never use email as an ownership
  key; email is allowed only for verified bootstrap-administrator configuration.
- Keep human and machine identities distinct. Client Credentials tokens must have
  `subject_type=client` and must never contain a user `uid` or human roles.
- Scope human roles to a resource-server audience. `AUTH_ADMIN` controls this service;
  roles such as `URL_ADMIN` apply only to their application audience.
- The management API owns all admin mutations and audit events. The admin UI must never
  access PostgreSQL directly or place access tokens/client secrets in browser storage.
- Preserve CSRF protection for cookie-authenticated management APIs.
- Use Liquibase for every database change. Do not modify an applied changeset; add a new one.

## Branching

- `dev`: development integration branch. Feature and documentation PRs target `dev`.
- `main`: production branch. Only promotion PRs from `dev` may target `main`.
- Do not push feature work directly to `main`.

## Tests and delivery

- OAuth/OIDC behavior, persistence, authorization boundaries, client lifecycle, and
  migrations require integration tests against PostgreSQL Testcontainers.
- Backend: run `./mvnw verify` or `.\mvnw.cmd verify` from `backend/`.
- Admin UI after it exists: run lint, unit tests, and production build.
- Update CI when adding a new buildable module.
- Keep Docker builds non-root and free of embedded secrets.

## Documentation requirements

When behavior or configuration changes, update the matching documents:

1. `docs/PROJECT_LOG.md` for completed milestones.
2. `docs/ARCHITECTURE.md` for trust boundaries, data models, claims, or endpoints.
3. `docs/DEVELOPER_GUIDE.md` for setup, commands, or environment variables.
4. `docs/CI_CD_PIPELINE.md` for workflow or release changes.
5. `docs/OPERATIONS.md` for deployment, secrets, rotation, backup, or incident procedures.
6. `docs/ROADMAP.md` when a story is completed, added, removed, or reprioritized.
7. `docs/ADMIN_UI_REQUIREMENTS.md` when the admin UI contract or scope changes.
