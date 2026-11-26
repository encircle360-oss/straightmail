# Changelog

All notable changes to the straightmail backend will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.5.0] - 2026-11-26

### Breaking changes — migrating from 0.4.x

- All `@RestController` endpoints are prefixed with `api.prefix` (`API_PREFIX`, default `/api`) and
  live under `/v1`: `POST /api/v1/email`, `/api/v1/email/inline`, `/api/v1/render`. Requests to the
  0.4.x paths are not API requests and fail CSRF protection with `403`
- `auth.mode` (`AUTH_MODE`) defaults to `oidc`, which needs a reachable issuer (or `OIDC_JWK_URI`) at
  startup; `none` is the 0.4.x behaviour
- `EncryptionService` is always created and aborts startup unless `ENCRYPTION_KEY` is the Base64
  encoding of exactly 32 bytes
- The OpenAPI document is served under the prefix (`/api/v3/api-docs`); `/v3/api-docs` answers `404`
- `HtmlUtil` renders links in the derived plain-text part as `text(url)` instead of `text ( url )`,
  and the whitespace of that part can differ
- `RenderController` and `TemplateReadController` resolve templates only through
  `CompositeTemplateProvider` (per-tenant layout under `templates.file.base-path`); only
  `EmailService` falls back to `FileTemplateLoader` and the flat `/resources/templates` layout.
  `templates.file.base-path` defaults to `/directory/files/templates`
- Runtime image: Temurin 25 JRE, non-root user `straightmail` (UID 10001), layered application in `/app`
- FreeMarker sandbox and narrowed `springMacroRequestContext` (see Security)

### Security

- FreeMarker locked down in `FreemarkerSecurityConfig`: `TemplateClassResolver.ALLOWS_NOTHING_RESOLVER`
  plus the `?api` built-in disabled, both set once on the shared `Configuration` singleton. `?new` could
  previously instantiate `freemarker.template.utility.Execute` and run OS commands, reachable by any
  caller holding a per-tenant API key via `/api/v1/email/inline` or a stored template — **breaking for
  templates using `?new` or `?api`**
- Base images pinned by digest to Temurin 25.0.5.1+2 (fix for CVE-2026-114938 and the other
  November 2026 OpenJDK CVEs); Dependabot updates the digests
- Spring Boot 4.2.0, which manages Tomcat 11.0.27 (CVE-2026-65182, CVE-2026-65905, CVE-2026-68525)
  and FreeMarker 2.3.35 (CVE-2026-84939)
- jsoup 1.24.1, springdoc-openapi 3.2.1, JGit 7.8.1.202611041320-r, ShedLock 7.12.0, Lombok 1.18.48;
  `commons-lang3` on the buildscript classpath constrained to 3.22.0 (CVE-2025-48924)
- `StartupConfigLogger` reports `encryption.key` and `api.key` only as `(set)` / `(not set)`
- Templates no longer see Spring's `RequestContext`: `springMacroRequestContext` is now the narrow
  `TemplateRequestContext` facade (message and URL methods only). Through
  `springMacroRequestContext.webApplicationContext` any template could read every configuration
  property, `encryption.key` included, and call any bean. `JsonNodeObjectWrapper` also pins
  FreeMarker's `DefaultMemberAccessPolicy`, so `getClass()` and class-loader reflection are blocked —
  **breaking for templates using the form-binding macros (`spring.bind`, `spring.formInput`, …)**
- CSRF protection is enabled on all three filter chains and exempts only the API prefix, which
  authenticates by bearer token or `X-API-KEY` header, never by cookie
- Request-controlled values (template ID, locale, tenant slug, template path) pass through
  `LogSanitizer` before logging, so CR/LF and other control characters cannot forge log entries
- `EncryptionService` concatenates IV and ciphertext without length arithmetic
- Runtime image applies pending Ubuntu security updates and drops the unused `/usr/bin/pebble` Go
  binary, which carried critical Go stdlib CVEs
- `FileTemplateSourceProvider` and `AbstractTemplateLoader` normalise template names and confine them
  to the tenant directory and to the templates root respectively; `../other-tenant/welcome` no longer resolves, over the URL or through the
  `templateId` in the JSON body of `/api/v1/render` and `/api/v1/email`
- `TenantReadController.list()` and `get(slug)` require `ROLE_ADMIN`; both had no authorization at all
  and exposed every tenant's `smtpHost`, `smtpPort`, `smtpUser`, `smtpSender`, `gitRepoUrl` and
  `hasApiKey`. `/api/v1/tenants/me` stays open to any authenticated caller
- `SecurityConfig` sends a Content-Security-Policy, `Referrer-Policy: same-origin` and HSTS on all
  three filter chains; the policy is derived from `auth.issuer-uri` and replaceable via
  `security.content-security-policy`
- `server.forward-headers-strategy=framework` so HSTS is emitted behind a TLS terminator
- `management.endpoints.web.exposure.include` set explicitly to `${MANAGEMENT_ENDPOINTS:health}`
- `mavenCentral` ordered before `mavenLocal` in `build.gradle`

### Added

- Multi-tenancy: `TenantService`, `TenantRepository`, `/v1/tenants` CRUD admin API
- Per-tenant API key authentication (`ApiKeyTenantResolutionFilter`) with SHA-256 hashing via `TenantService.hashApiKey`
- `AbstractTenantResolutionFilter` base class; JWT and API-key resolution now share common logic
- Config-based tenant provisioning via `TenantProperties` + `TenantReconciliationService` (YAML-declared tenants created at startup when they do not exist; existing tenants are not updated)
- `StartupConfigLogger`: logs the resolved configuration at startup; secrets are reported only as set or not set
- `auth.mode` property (`oidc` | `api-key` | `none`) replacing the boolean `auth.enabled` flag
- No-authentication mode: `NoAuthTenantResolutionFilter` resolves tenant from `X-Tenant-ID` header or falls back to default
- Database-backed Git sync status (`GitSyncStatusRepository`) and template caching
- `GitSyncScheduler` with initial sync on application startup; ShedLock uses JVM system time for SQLite compatibility
- Read-only template view (`TemplateView.editable`) distinguishing DATABASE vs FILE/GIT templates
- Implicit source tags (`source:database`, `source:file`, `source:git`) auto-applied by `TemplateService`
- Branch-aware Git template handling: multiple branches can expose the same template ID
- `effectiveTags` unifying explicit and source tags for consistent tag-based filtering
- UUID-based template lookup fallback in `TemplateService`
- Tenant branding fields in `TenantDTO`
- Comprehensive Javadoc on all public REST controller methods and non-obvious service methods
- `MessageSourceConfig`: bundles in `i18n.base-path` (`I18N_PATH`, default `/resources/i18n`) are read
  before the built-in `classpath:i18n/messages`; the image creates `/resources/i18n`
- OpenAPI info version taken from the build info (`springBoot.buildInfo`)

### Changed

- Spring Boot 4.2.0, Gradle 9.9.2; `hibernate-community-dialects`, `sqlite-jdbc` and Testcontainers
  versions now come from the Boot BOM, so the dialect stays aligned with Hibernate core
- Keycloak 26.8.1 in the OIDC Compose stacks
- Switched default database from PostgreSQL to SQLite (`jdbc:sqlite:`); Liquibase changesets updated
- MailHog replaced by Mailpit in Testcontainers (`AbstractTest`) and all Docker Compose stacks
- Keycloak hostname handling updated; JWK endpoint URL and issuer URL split into separate config properties
- `ObjectProvider<T>` used for all beans conditional on the `database` profile (`TenantService`, `TenantMailSenderFactory`)
- Package structure reorganized: `controller/{mail,template,tenant}`, `service/template/{loader,provider}`
  and `tenant/{filter,mail}` sub-packages
- `@Autowired` field injection replaced by constructor injection throughout
- `this.` prefix applied consistently to all same-class method calls
- Toolchain: Java 21 → 25

### Fixed

- `EmailRequestDTO.senderName` sets the display name of the `From` header, as in 0.4.x
- `spring.mail.port` defaults to `SMTP_PORT:587`, as in 0.4.x; an empty default broke the startup of
  `ConfigBasedMailSenderFactory`
- `ApiConfig` declares no fixed OpenAPI server (`http://localhost:50003` is gone), as in 0.4.x
- PostgreSQL could not start: `tenants.active`, `smtp_tls` and `smtp_ssl` were created as `integer` in
  changeset `0002`, which PostgreSQL rejects when Hibernate binds a boolean, so the first insert from
  `TenantReconciliationService` failed at startup. Changeset `0009` converts the three columns to
  `boolean` and is marked as ran on SQLite, leaving existing SQLite installations untouched

### Removed

- `auth.enabled` boolean property (superseded by `auth.mode`)
- `AbstractTemplateLoader.getFileContent`, deprecated in 0.4.1
- Helm chart templates and related CI workflows

## [0.4.1] - 2026-06-03

### Changed

- `AbstractTemplateLoader` distinguishes required template files (HTML, `_subject.ftl`) from the
  optional `_plain.ftl`; a missing plain-text variant is no longer logged as an error, and
  `EmailService` derives the plain text from the HTML (#5)
- `EmailService.sendMail` logs at INFO on request entry and after the SMTP dispatch (template source
  and ID, recipient counts, locale — no recipient addresses) (#5)

### Deprecated

- `AbstractTemplateLoader.getFileContent` (`forRemoval = true`); use `getRequiredFileContent` or
  `getOptionalFileContent` (#5)

## [0.4.0] - 2026-05-21

Security release: moves the application off end-of-life Spring Boot 2.5.6 and patches
known-vulnerable dependencies, and hardens the build/release supply chain.

### Security

- Upgraded off end-of-life Spring Boot 2.5.6 → 3.5.14 (Java 17 → 21, Temurin) to pick up security patches
- Patched dependencies with known vulnerabilities: jsoup → 1.18.3 (`Whitelist` → `Safelist`), springdoc-openapi → 2.8.17, MapStruct → 1.6.3, Lombok → 1.18.46
- Supply-chain hardening: published images now ship an SBOM and build provenance attestation
- Dropped the hardcoded Swagger server URL so the OpenAPI host follows the request host (#2)

### Added

- `EmailRequest.senderName` so the `From` header renders as `Display Name <addr@example.com>` (#1)
- GitHub Actions CI: build/test on PR + `master`, multi-arch (amd64/arm64) image published to `ghcr.io`

### Changed

- Migrated `javax.*` → `jakarta.*` (servlet, mail, validation)
- Added `lombok-mapstruct-binding` for builder/setter interop with the upgraded MapStruct/Lombok
- Gradle wrapper → 8.10.2
- Switched tests from embedded MongoDB to GreenMail (embedded SMTP)
- Updated Dockerfile base image to `eclipse-temurin:21-jre`
- Rewrote README with badges, migration banner, contributing & support sections, disclaimer, and Apache-2.0 reference

### Removed

- MongoDB integration (template CRUD, repository, `MongoDbTemplateLoader`, `mongo` profile) — file-based templates only

## [0.2.0] - 2026-03-16

> **Note:** Version 0.2.0 encompasses the Spring Boot upgrade and all subsequent changes since November 2025.

### Added

- `InfoController` exposing a `/v1/info` endpoint with application metadata
- CORS configuration via `cors.allowed-origins` property in `ApiConfig`
- JWT-based authentication backed by OAuth2 resource server, togglable via `auth.enabled` property
- `SecurityConfig` with configurable `issuer-uri` for JWT validation
- OpenTelemetry tracing via `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp` dependencies
- Liquibase for database schema management and migrations
- PostgreSQL Testcontainers (`DatabaseProfileTest`) for integration tests under the `database` profile
- `docker-compose.yml` with PostgreSQL service definition and health checks
- Custom `findAllByTagsContains` query in `TemplateRepository` using JPA (previously MongoDB `$all` operator)
- SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui` 2.8.15) for Swagger UI
- GitHub Actions workflow (`backend.yml`) for backend-only CI/CD builds
- `.http` request files (`mail-controller.http`, `templates-controller.http`, `render-controller.http`) for API testing
- `CONTRIBUTING.md` and `LICENSE` files
- Mockito inline mock maker enabled in Gradle for JDK 21+ compatibility

### Changed

- Upgraded Spring Boot from 2.5.6 to 3.5.7 (Java 17 → 21)
- Upgraded Gradle Wrapper from 8.14 to 9.3.1
- Updated Spring Dependency Management from 1.0.11.RELEASE to 1.1.7
- Updated Lombok from 1.18.22 to 1.18.34
- Updated MapStruct from 1.3.1.Final to 1.6.3
- Updated jsoup from 1.13.1 to 1.18.1
- Upgraded Testcontainers to 2.0.2
- Migrated database from MongoDB to PostgreSQL; all repositories and queries updated to JPA
- Replaced Flapdoodle embedded MongoDB with `MongoDbTestContainer` (later superseded by PostgreSQL migration)
- Replaced `TestApplication`/`TestDatabaseConfig` with `StraightmailApplication` + `DatabaseProfileTest` for tests
- Updated API prefix to `/v1` across all controllers and test routes
- Moved backend source into `backend/` subdirectory as part of monorepo restructure
- Excluded unused Spring Boot auto-configurations for JDK 21 compatibility (`application.yml`)
- Transitioned from `javax` to `jakarta` annotations throughout the codebase
- Updated `application.yml` SMTP defaults to `localhost:1025` for local development
- Updated email placeholder examples to use domain-specific values across templates and tests
- Scoped `RenderController.mapper` as `static final`; improved type safety (`HashMap` → `Map`)
- Renamed `deploy.yml` to `backend.yml` in GitHub Actions; removed obsolete `pr-validation.yml` and `release-deploy.yml`
  workflows
- Reformatted IntelliJ run configuration file

### Removed

- All MongoDB / Flapdoodle dependencies
- GitLab CI configuration (`.gitlab-ci.yml`)
