# Changelog

All notable changes to the straightmail monorepo are documented in this file.
For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [
`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.5.1] - 2026-11-26

No application code changes; this release changes how the container images are named, tagged and
published. For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and
[`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

### Added

- `ghcr.io/encircle360-oss/straightmail/backend`, the image with the API only, under a name of its
  own. Every build is also pushed with the identical digest as `ghcr.io/encircle360-oss/straightmail`,
  the name used up to 0.5.0, which stays published so existing deployments and `FROM` lines keep
  working
- `ghcr.io/encircle360-oss/straightmail/aio`, the all-in-one image: the API with the Admin UI
  embedded. It replaces `ghcr.io/encircle360-oss/straightmail/monorepo` and, unlike it, gets version
  tags and a `latest` that follows releases
- A smoke test for the images, `.github/scripts/smoke-test-image.sh <image> <backend|aio>`, also
  runnable locally: it starts the image without external services, checks `/actuator/health` and
  `/api/v1/info`, and checks that `aio` serves the Admin UI and `backend` does not. Pull requests
  that touch the backend, the frontend or the image build build both images for `linux/amd64` and
  run it; nothing is pushed for a pull request

### Changed

- Image tags, the same for every image: a release tag `vX.Y.Z` publishes `X.Y.Z`, `X.Y`, `X` and
  `latest`; a pre-release such as `v1.0.0-rc.1` publishes only its own version. Pushes to the
  default branch publish `edge` and the 7-character commit SHA and no longer move `latest`, so
  `latest` is always the most recently tagged release. The commit tag of
  `ghcr.io/encircle360-oss/straightmail` changes from `master-<sha>` to the bare SHA (for example
  `abc1234`). The GHCR cleanup never deletes `edge`
- One workflow, `images.yml`, builds and publishes all images and replaces `release.yml` and
  `monorepo.yml`. It publishes only after the backend and frontend test suites and the smoke test
  passed. Each image has its own GitHub Actions build cache; the registry cache tag
  `…/straightmail/monorepo:buildcache` is no longer written
- The GHCR cleanup covers `straightmail`, `straightmail/backend` and `straightmail/aio`, no longer
  protects a `buildcache` tag, and skips a package that does not exist instead of failing the run
- Dependabot offers TypeScript only patch updates, since Angular 22.3 accepts `>=6.0 <6.1`, and
  updates the Angular packages (`@angular/*`, `@angular-devkit/*`, `@schematics/angular`) in one
  pull request of their own, apart from the other npm minor and patch updates

### Removed

- `ghcr.io/encircle360-oss/straightmail/monorepo`, the name under which 0.5.0 published the image
  with the Admin UI, is no longer updated; use `ghcr.io/encircle360-oss/straightmail/aio`. The
  package stays at its last build, the 0.5.0 commit `0397766` (also its `latest`)

### Fixed

- Build provenance attestations of the images also create the artifact storage record, which lists
  the image on the organization's Linked Artifacts page; the workflow grants `artifact-metadata: write`
- The Trivy scans of the images upload to separate code scanning categories,
  `container-image-backend` and `container-image-aio`; with a shared category, the second upload for
  a commit replaced the results of the first image

## [0.5.0] - 2026-11-26

For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).
The migration guide in the [README](README.md#migrating-from-04x) shows each step with examples.

### Breaking changes — migrating from 0.4.x

- The REST API moved under `/api/v1`: `POST /api/v1/email`, `/api/v1/email/inline`, `/api/v1/render`.
  The prefix comes from `API_PREFIX` (default `/api`), `/v1` is fixed. `POST /email` and the other
  0.4.x paths answer `403`
- `AUTH_MODE` defaults to `oidc`: without `AUTH_MODE=none` and without a reachable OIDC issuer the
  application does not start. `none` is the 0.4.x behaviour
- `ENCRYPTION_KEY` is required and must be the Base64 encoding of exactly 32 bytes
  (`openssl rand -base64 32`); otherwise the application does not start
- The OpenAPI document moved to `/api/v3/api-docs`; `/v3/api-docs` answers `404`. Swagger UI stays
  at `/swagger-ui/index.html`
- The plain-text part derived from the HTML body (templates without `_plain.ftl`) renders links as
  `text(url)` instead of `text ( url )`, and its whitespace can differ
- `ghcr.io/encircle360-oss/straightmail` still contains the backend only. The Admin UI ships only
  in `ghcr.io/encircle360-oss/straightmail/monorepo`, published as `latest` and by commit SHA, not
  by version
- The image is based on Temurin 25 (JRE) instead of `eclipse-temurin:21-jre`, runs as the non-root
  user `straightmail` (UID 10001) and starts the application from `/app` instead of `/straightmail.jar`
- FreeMarker renders sandboxed: `?new`, `?api` and the Spring form-binding macros fail (see Security)
- `/api/v1/render` and the template listing only read the per-tenant layout
  `TEMPLATE_FILE_PATH/<tenant>/<id>.ftl`; only `/api/v1/email` still falls back to the flat 0.4.x
  layout under `/resources/templates`, and only in single-tenant operation (no `database` profile, at
  most one tenant declared in `tenants.config`). `TEMPLATE_FILE_PATH` defaults to
  `/directory/files/templates`
- Actuator exposes `health` only (`MANAGEMENT_ENDPOINTS` widens it again)

### Security

- Base images pinned by digest to Temurin 25.0.5.1+2, which fixes CVE-2026-114938 and the other
  November 2026 OpenJDK CVEs
- Spring Boot 4.2.0, which manages Tomcat 11.0.27 (CVE-2026-65182, CVE-2026-65905, CVE-2026-68525)
  and FreeMarker 2.3.35 (CVE-2026-84939)
- Dependency updates: jsoup 1.24.1, springdoc-openapi 3.2.1, JGit 7.8.1, ShedLock 7.12.0, Lombok
  1.18.48; `commons-lang3` on the Gradle build classpath constrained to 3.22.0 (CVE-2025-48924)
- FreeMarker renders sandboxed: `?new` is rejected for every class and `?api` is disabled. Previously
  `?new` could instantiate `freemarker.template.utility.Execute` and run OS commands, reachable by any
  caller holding a per-tenant API key — **breaking for templates that use either built-in**
- Template IDs are normalised and confined to their tenant directory; `../` escapes no longer read
  another tenant's templates, over the URL or through the JSON body of `/api/v1/render` and `/api/v1/email`
- `GET /api/v1/tenants` and `GET /api/v1/tenants/{slug}` require `ROLE_ADMIN` — they previously let any
  authenticated caller enumerate every tenant's mail relay configuration. `/api/v1/tenants/me` is unchanged
- Content-Security-Policy, `Referrer-Policy` and HSTS on all three filter chains, with
  `server.forward-headers-strategy=framework` so HSTS survives a TLS terminator
- Actuator exposure set explicitly to `health` (`MANAGEMENT_ENDPOINTS` widens it again)
- Production frontend bundles no longer ship the development environment; bearer tokens and API keys
  are scoped by origin as well as path
- The startup configuration log reports `ENCRYPTION_KEY` and `API_KEY` only as set or not set,
  never a part of their value
- `mavenCentral` before `mavenLocal` so a stale local artifact cannot win a build

### Added

- Multi-tenancy support: per-tenant SMTP, API keys, and JWT tenant resolution
- No-authentication (`auth.mode=none`) and API-key (`auth.mode=api-key`) modes alongside OIDC
- Admin UI (Angular) embedded into the backend JAR by the full Gradle build
- Message bundles are read from `I18N_PATH` (default `/resources/i18n`) before the built-in ones
- Modular Docker Compose stacks (`docker/minimal.yml`, `apikey-sqlite.yml`, `oidc-sqlite.yml`,
  `oidc-postgres.yml`)
- Monorepo full-build workflow (`monorepo.yml`) publishing the combined backend+frontend image
- Dependabot for Gradle, npm, Docker and GitHub Actions

### Changed

- Replaced MailHog with Mailpit across all Docker Compose configs and integration tests
- SQLite is the default database of the `database` profile (Liquibase migrations updated)
- Keycloak hostname handling refactored; JWK/issuer URLs split for OIDC setups
- Toolchain: Java 21 → 25, Spring Boot 4.2.0, Gradle 9.9.2, Angular 22.3, Node.js 26 in CI
- Compose stacks bind their ports to `127.0.0.1` instead of `0.0.0.0`; `docker/README.md` states what
  a real deployment has to change
- The OpenAPI document reports the application version from the build instead of a fixed value
- CI: pull requests run `ci.yml`, which ends in a single `CI gate` check; CodeQL, Gradle dependency
  submission and the GHCR cleanup run in-repo. `.github/workflows/README.md` documents all nine
  workflows
- GHCR cleanup protects released tags (`x`, `x.y`, `x.y.z`, `latest`, `buildcache`) with an anchored
  regular expression

### Fixed

- `senderName` sets the display name of the `From` header, as in 0.4.x
- An unset `SMTP_PORT` defaults to `587`, as in 0.4.x, instead of breaking startup
- The OpenAPI document declares no fixed server, as in 0.4.x, so the documented host follows the
  request (including forwarded headers)
- The PostgreSQL stack could not start: `tenants.active`, `smtp_tls` and `smtp_ssl` were created as
  `integer`, which PostgreSQL rejects when Hibernate binds a boolean, so tenant reconciliation failed
  on the first insert. Changeset `0009` converts the three columns; SQLite installations are unaffected
- Three Compose stacks shipped `ENCRYPTION_KEY: "CHANGE_ME_GENERATE_RANDOM_KEY"`, which is not valid
  Base64 and put the container in a crash loop on first start
- With the `database` profile, `/api/v1/email` answers an unknown template ID with `400` (template not
  found) instead of `500`

### Removed

- `trivy.yml`; `release.yml` and `monorepo.yml` scan the image they push with Trivy
- `frontend/.github/workflows/ci.yml`, which GitHub never ran

### Documentation

- README: the template preview renders in an `<iframe srcdoc>` and therefore inherits the page's
  Content-Security-Policy, so images on foreign hosts are blocked by the shipped default. The
  security section carries a complete `SECURITY_CONTENT_SECURITY_POLICY` example that widens
  `img-src` while keeping every other directive, identity provider origin included
- README, component READMEs and changelogs aligned with the 0.5.0 code: migration guide against
  0.4.x, configuration defaults, image contents

## [0.4.1] - 2026-06-03

### Changed

- A missing `_plain.ftl` is no longer an error: the HTML body and `_subject.ftl` are required, the
  plain-text variant is optional and derived from the HTML when absent (#5)
- Each send is logged at INFO on receipt and after the SMTP dispatch — template source and ID,
  recipient counts, locale; no recipient addresses or rendered content (#5)

### Deprecated

- `AbstractTemplateLoader.getFileContent` (for removal) — subclasses use `getRequiredFileContent` or
  `getOptionalFileContent` (#5)

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
