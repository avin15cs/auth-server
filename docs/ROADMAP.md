# Auth Server Delivery Roadmap

This backlog is ordered by security and dependency, not by UI visibility. GitHub issues
can mirror these stable story IDs after the GitHub integration is connected. Detailed
admin frontend acceptance criteria are maintained in `ADMIN_UI_REQUIREMENTS.md`.

## P0 -- Control plane

- [x] `AUTH-001` Audience-scoped roles and administrator bootstrap -- persist roles,
  emit only roles for the access-token audience, protect management APIs, prevent
  administrator self-lockout, and audit role changes.
- [x] `AUTH-002` Client-management API -- allow `AUTH_ADMIN` users to create, inspect,
  disable, and update Authorization Code and Client Credentials registrations with
  strict redirect URI, grant, scope, and audience validation. Return a generated client
  secret once and store only its password hash.
- [x] `AUTH-003` Client lifecycle integration tests -- prove registration, token issuance,
  disablement, redirect validation, scope enforcement, and audit behavior against real
  PostgreSQL.

## P1 -- Administrator experience

- [x] `AUTH-004` Auth administration UI -- provide same-origin Google login, client
  registration, one-time secret display, user search, and audience-specific role
  assignment without direct database access.
- [ ] `AUTH-005` Credential rotation and revocation -- support planned overlap, explicit
  expiry, emergency revocation, and complete audit history without exposing stored
  secrets.
- [ ] `AUTH-006` Operations dashboard -- expose safe client status, recent administrator
  actions, signing-key metadata, and health signals without leaking token or secret data.

## P2 -- Production operations

- [ ] `AUTH-007` Rate limits and abuse controls for login, token, and management endpoints.
- [ ] `AUTH-008` Audit export, alerting, backup/restore drill, and signing-key rotation drill.
