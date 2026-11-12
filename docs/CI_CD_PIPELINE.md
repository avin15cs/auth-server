# CI/CD Pipeline

## Current state

The repository currently has continuous integration but no production deployment
workflow. `.github/workflows/ci.yml` runs for pushes and pull requests targeting `dev`
or `main` with read-only repository permissions.

## CI jobs

### Backend verification

- Checks out the repository.
- Installs Temurin Java 21.
- Enables the Maven dependency cache.
- Runs `mvn --batch-mode --no-transfer-progress verify`.
- Starts PostgreSQL Testcontainers through the test suite.

### Container verification

- Configures Docker Buildx.
- Builds the multi-stage Dockerfile without publishing an image.
- Confirms the production image can be assembled without embedding source secrets.

## Branch policy

- Feature and documentation PRs target `dev`.
- `main` represents production.
- Production changes should arrive through a `dev` to `main` promotion PR.
- CI must pass before either protected branch is merged.

## Admin UI impact

When `admin-ui/` is introduced, extend CI with a Node 22 job that uses the lockfile and
runs, at minimum:

```text
npm ci
npm run lint
npm test -- --run
npm run build
```

The container job must also build the UI and verify that its static output is served by
the production auth-server image. Dependency versions and container base images should
be pinned deliberately.

## Future CD requirements

Do not add automatic production deployment until the target platform and secret manager
are selected. A production workflow must:

1. Run the same verification as CI.
2. Build an immutable, non-root container.
3. Publish it to the selected registry using least-privilege credentials.
4. Inject database, Google, and signing secrets at runtime rather than build time.
5. Run Flyway before accepting traffic.
6. verify readiness and support rollback.
7. Record the deployed commit and image digest.

Never print JWKs, OAuth secrets, generated client credentials, or database passwords in
workflow logs.
