# Changelog

All notable changes to the straightmail monorepo are documented in this file.
For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [
`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.5.0] - 2026-09-17

For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

### Security

- FreeMarker renders sandboxed: `?new` is rejected for every class and `?api` is disabled. Previously
  `?new` could instantiate `freemarker.template.utility.Execute` and run OS commands, reachable by any
  caller holding a per-tenant API key — **breaking for templates that use either built-in**
- Template IDs are normalised and confined to their tenant directory; `../` escapes no longer read
  another tenant's templates, over the URL or through the JSON body of `/v1/render` and `/v1/email`
- `GET /v1/tenants` and `GET /v1/tenants/{slug}` require `ROLE_ADMIN` — they previously let any
  authenticated caller enumerate every tenant's mail relay configuration. `/v1/tenants/me` is unchanged
- Content-Security-Policy, `Referrer-Policy` and HSTS on all three filter chains, with
  `server.forward-headers-strategy=framework` so HSTS survives a TLS terminator
- Actuator exposure set explicitly to `health` (`MANAGEMENT_ENDPOINTS` widens it again)
- Production frontend bundles no longer ship the development environment; bearer tokens and API keys
  are scoped by origin as well as path
- `mavenCentral` before `mavenLocal` so a stale local artifact cannot win a build

### Added

- Multi-tenancy support: per-tenant SMTP, API keys, and JWT tenant resolution
- No-authentication (`auth.mode=none`) and API-key (`auth.mode=api-key`) modes alongside OIDC
- Modular Docker Compose stacks (`docker/oidc-sqlite.yml`, `api-key-sqlite.yml`, etc.)
- Feature branch CI workflow (`feature.yml`) for `feature/**`, `fix/**`, `chore/**`
- Monorepo full-build workflow (`monorepo.yml`) producing a combined backend+frontend JAR image
- Frontend standalone build artifact (`frontend-dist`) uploaded by `frontend.yml` on every `main` push

### Changed

- Replaced MailHog with Mailpit across all Docker Compose configs and integration tests
- Fixed `frontend.yml` path filters (`../../frontend/**` → `frontend/**`) so the workflow now triggers correctly
- Added `if: github.ref == 'refs/heads/main'` guard to `backend.yml` docker job (was pushing on PRs)
- SQLite replaces PostgreSQL as default database backend (Liquibase migrations updated)
- Keycloak hostname handling refactored; JWK/issuer URLs split for OIDC setups
- Toolchain: Java 21 → 25, Spring Boot 4.1.1, Gradle 9.7.1, Angular 22.1.x, Node.js 22
- Compose stacks bind their ports to `127.0.0.1` instead of `0.0.0.0`; `docker/README.md` states what
  a real deployment has to change
- CI repaired and extended: dependency and container scanning, the image cleanup workflow moved
  in-repo, and all seven workflows documented

### Fixed

- The PostgreSQL stack could not start: `tenants.active`, `smtp_tls` and `smtp_ssl` were created as
  `integer`, which PostgreSQL rejects when Hibernate binds a boolean, so tenant reconciliation failed
  on the first insert. Changeset `0009` converts the three columns; SQLite installations are unaffected
- Three Compose stacks shipped `ENCRYPTION_KEY: "CHANGE_ME_GENERATE_RANDOM_KEY"`, which is not valid
  Base64 and put the container in a crash loop on first start

## [0.4.0] - 2026-05-21

Security release. For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md).

### Security

- Upgraded off end-of-life Spring Boot 2.5.6 → 3.5.14 (Java 17 → 21, Temurin)
- Patched known-vulnerable dependencies: jsoup 1.18.3 (`Whitelist` → `Safelist`), springdoc-openapi 2.8.17, MapStruct 1.6.3, Lombok 1.18.46
- Supply-chain hardening: published images ship an SBOM and build provenance attestation
- Swagger no longer exposes a hardcoded server URL; the OpenAPI host follows the request host (#2)

### Added

- `EmailRequest.senderName` for `Display Name <addr@example.com>` From headers (#1)
- GitHub Actions CI replacing GitLab CI: build/test on PR + `master`, multi-arch (amd64/arm64) images to `ghcr.io`

### Changed

- Migrated `javax.*` → `jakarta.*`; added `lombok-mapstruct-binding`; Gradle wrapper → 8.10.2
- Switched tests from embedded MongoDB to GreenMail; Dockerfile base → `eclipse-temurin:21-jre`
- Rewrote README (badges, migration banner, contributing/support, disclaimer, Apache-2.0)

### Removed

- MongoDB integration — file-based templates only

## [0.2.0] - 2026-03-16

### Structure

- Moved backend source into `backend/` subdirectory
- Added Angular admin console as `frontend/` via `git subtree`
- Integrated backend and frontend into a unified Gradle build:
  - Added `npmInstall`, `npmBuild`, `copyClientDist` Gradle tasks with `SKIP_FRONTEND_BUILD` flag
  - Added `SpaController` for Angular SPA routing fallback under the backend
  - Added Angular `proxy.conf.json` routing `/api` to backend port during development

### Infrastructure

- Added `docker-compose.yml` with PostgreSQL (incl. health check), backend, and frontend services
- Enabled multi-platform Docker builds (amd64 + arm64) via QEMU and Docker Buildx in CI

### CI/CD

- Added initial GitHub Actions workflows for build, test, and Docker image publishing (`main-deploy.yml`)
- Split into dedicated `backend.yml` and `frontend.yml` workflows; removed obsolete `pr-validation.yml` and
  `release-deploy.yml`

### Documentation

- Added `CONTRIBUTING.md` with guidelines for issues, features, and pull requests
- Added `LICENSE` (Apache 2.0)
- Updated root `README.md` with feature description, UI previews, technology stack badges
