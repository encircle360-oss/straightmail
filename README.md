# <img src="frontend/public/logos/logo.svg" alt="straightmail" width="34"> straightmail

[![Monorepo Full Build](https://github.com/encircle360-oss/straightmail/actions/workflows/monorepo.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/monorepo.yml)
[![Backend Build](https://github.com/encircle360-oss/straightmail/actions/workflows/backend.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/backend.yml)
[![Frontend Build](https://github.com/encircle360-oss/straightmail/actions/workflows/frontend.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/frontend.yml)
[![Container](https://img.shields.io/badge/ghcr.io-encircle360--oss%2Fstraightmail-blue?logo=docker)](https://github.com/encircle360-oss/straightmail/pkgs/container/straightmail)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java 25](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22.1-DD0031?logo=angular&logoColor=white)](https://angular.io)
[![Matrix](https://img.shields.io/badge/Matrix-Join%20Chat-0dbd8b?logo=matrix&logoColor=white)](https://matrix.to/#/#oss:encircle360.com)

**straightmail** is a mail sending API with template and i18n support, combined with a modern Angular administration
console for managing email templates, rendering, and sending operations.

<img src="frontend/docs/files/dashboard.png" alt="Dashboard" width="800">

<table width="800">
  <tr>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/templates.png" alt="Templates" width="190">
      <br><sub>Templates</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/edit%20template.png" alt="Edit Template" width="190">
      <br><sub>Edit Template</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/render%20preview%20html.png" alt="Render Preview HTML" width="190">
      <br><sub>Render Preview</sub>
    </td>
    <td valign="top" width="25%" rowspan="2" align="center" valign="top">
      <img src="frontend/docs/files/mobile.png" alt="Mobile View" width="100">
      <br><sub>Mobile View</sub>
    </td>
  </tr>
  <tr>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/send%20mail%20template.png" alt="Send Mail Template" width="190">
      <br><sub>Send Mail</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/help.png" alt="Help Panel" width="190">
      <br><sub>Help Panel</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/mail.png" alt="Mail Output" width="190">
      <br><sub>Mail Output</sub>
    </td>
  </tr>
</table>

## straightmail Monorepo

This repository contains the straightmail backend and admin frontend as a monorepo.

```
straightmail/
├── backend/          # Spring Boot Application (Java 25)
├── frontend/         # Angular Admin Frontend
├── docker/           # Mode-specific Docker Compose stacks
├── .github/workflows # CI/CD pipelines
└── README.md
```

## Getting Started

### Migrating from 0.4.0

If you're upgrading straightmail from an older version, check this migration guide first:

<details>
<summary><strong>Migration Guide</strong></summary>

### What changed

|                  | 0.4.0                                   | 0.5.0                                                              |
|------------------|-----------------------------------------|--------------------------------------------------------------------|
| `ENCRYPTION_KEY` | not required                            | **required** — `openssl rand -base64 32`                           |
| Auth modes       | not implemented                         | OIDC (default), `api-key`, `none`                                  |
| Admin UI         | separate build required                 | bundled in backend image (only Monorepo), served at `:50003`       |
| Test mail server | MailHog (`mailhog/mailhog`)             | **Mailpit** (`axllent/mailpit`)                                    |
| Database         | not implemented                         | optional — API-only mode needs no database                         |
| File templates   | mount volume or baked into custom image | mount volume + `TEMPLATES_FILE_BASE_PATH: /templates`              |
| Tenant config    | implicit (single tenant)                | declare via `tenants.config` (required without `database` profile) |
| Java runtime     | 21 (Temurin)                            | **25** — only relevant if you run the JAR yourself                 |
| FreeMarker       | unrestricted                            | **sandboxed** — `?new` and `?api` are rejected                     |
| Template paths   | resolved as given                       | confined to the tenant directory — `../` escapes return `404`      |
| Tenant read API  | —                                       | `GET /v1/tenants` and `/v1/tenants/{slug}` require `ROLE_ADMIN`    |
| Actuator         | framework default                       | only `health`; widen via `MANAGEMENT_ENDPOINTS`                    |
| Security headers | none beyond Spring defaults             | CSP, `Referrer-Policy`, HSTS on every filter chain                 |

Your existing `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_ENABLE_TLS`,
`SMTP_ENABLE_SSL`, and `DEFAULT_SENDER` values carry over unchanged.

Everything below the runtime row can break a working 0.4.0 setup. Read
[Sandboxed template rendering](#sandboxed-template-rendering) and
[Security defaults in 0.5.0](#security-defaults-in-050) before you upgrade.

---

### Without Admin UI — API-only (minimal)

> **Required:** You must declare at least one tenant. Add `TENANTS_CONFIG_0_ID` and `TENANTS_CONFIG_0_DISPLAY_NAME`
> — without a tenant entry, requests will fail at tenant resolution.

Add `ENCRYPTION_KEY`, `AUTH_MODE: none`, and the tenant identity to your existing setup.

```yaml
services:
  straightmail:
    image: ghcr.io/encircle360-oss/straightmail:0.5.0
    ports:
      - "50003:50003"
    environment:
      ENCRYPTION_KEY: "<openssl rand -base64 32>"   # new — required
      AUTH_MODE: none                                 # new — open access
      TENANTS_CONFIG_0_ID: default                    # new — required
      TENANTS_CONFIG_0_DISPLAY_NAME: Default          # new — required
      SMTP_HOST: <your-mail-server>
      SMTP_PORT: <port>
      SMTP_USER: <username>
      SMTP_PASSWORD: <password>
      DEFAULT_SENDER: <noreply@example.com>
      SMTP_ENABLE_TLS: "true"
      SMTP_ENABLE_SSL: "false"
```

---

### With Admin UI

The Admin UI is now bundled in the backend image and served at `http://localhost:50003`.
Activate the `database` Spring profile to enable template CRUD and tenant management.

**Option A — API-Key auth (recommended for quick migration)**

```yaml
services:
  straightmail:
    image: ghcr.io/encircle360-oss/straightmail:0.5.0
    ports:
      - "50003:50003"
    volumes:
      - ./data:/data
    environment:
      AUTH_MODE: api-key
      API_KEY: "<your-secure-api-key>"
      ENCRYPTION_KEY: "<openssl rand -base64 32>"
      TENANTS_CONFIG_0_ID: default
      TENANTS_CONFIG_0_DISPLAY_NAME: Default
      SMTP_HOST: <your-mail-server>
      SMTP_PORT: <port>
      SMTP_USER: <username>
      SMTP_PASSWORD: <password>
      DEFAULT_SENDER: <noreply@example.com>
      SMTP_ENABLE_TLS: "true"
      SMTP_ENABLE_SSL: "false"
```

Access the UI at `http://localhost:50003`. Send API requests with `X-API-KEY: <your-secure-api-key>`.

**Option B — OIDC auth (production)**

```yaml
services:
  straightmail:
    image: ghcr.io/encircle360-oss/straightmail:0.5.0
    ports:
      - "50003:50003"
    volumes:
      - ./data:/data
    environment:
      AUTH_MODE: oidc
      OIDC_ISSUER_URI: https://<your-keycloak>/realms/<realm>
      ENCRYPTION_KEY: "<openssl rand -base64 32>"
      TENANTS_CONFIG_0_ID: default
      TENANTS_CONFIG_0_DISPLAY_NAME: Default
      SMTP_HOST: <your-mail-server>
      SMTP_PORT: <port>
      SMTP_USER: <username>
      SMTP_PASSWORD: <password>
      DEFAULT_SENDER: <noreply@example.com>
      SMTP_ENABLE_TLS: "true"
      SMTP_ENABLE_SSL: "false"
```

For a production SQLite setup, see [`docker/oidc-sqlite.yml`](docker/oidc-sqlite.yml).

For a production PostgreSQL setup, see [`docker/oidc-postgres.yml`](docker/oidc-postgres.yml).

---

### Migrating template and i18n files

In 0.4.0, templates and translations were typically baked into a custom image:

```dockerfile
FROM ghcr.io/encircle360-oss/straightmail:0.4.0
ADD backend/templates /resources/templates
ADD i18n /resources/i18n
```

In 0.5.0, mount your local directories instead — no custom image needed:

```yaml
    volumes:
      - ./templates:/templates             # FreeMarker templates
      - ./i18n:/resources/i18n             # translation bundles
    environment:
      TEMPLATES_FILE_BASE_PATH: /templates
```

Add these `volumes` and `environment` entries to whichever Compose snippet you use above.

**⚠️ Breaking change — tenant subdirectory required:** In 0.5.0, templates must live inside a subdirectory
named after the tenant ID. Move your existing template files into a `{tenantId}/` folder before mounting:

```
templates/
└── default/
    ├── welcome.ftl
    ├── welcome_subject.ftl
    └── welcome_plain.ftl
```

The subdirectory name must match your tenant ID (e.g. `default` for `TENANTS_CONFIG_0_ID: default`).
Subdirectories within the tenant folder are supported — `default/emails/welcome.ftl` gets the template ID `emails/welcome`.

**⚠️ Breaking change — template IDs cannot leave the tenant directory:** a `templateId` that resolves
outside its own tenant folder is rejected with `404`, both in the URL and in the JSON body of
`/v1/render` and `/v1/email`. If a 0.4.0 setup addressed templates through a relative path, flatten
those paths into the tenant folder. A `../` that normalises back inside the same tenant still resolves.

**`_plain.ftl` is now optional.** A template without its plain-text variant renders and sends as
HTML only; the `plain` field of the render result stays `null`. In 0.4.0 the missing file was an error.

---

### Sandboxed template rendering

**⚠️ Breaking change — this one fails at render time, not at startup.** Tenants author their own
templates, so FreeMarker now runs with `TemplateClassResolver.ALLOWS_NOTHING_RESOLVER` and the
`?api` built-in disabled. Two things stop working:

| Built-in | 0.4.0                                   | 0.5.0                    |
|----------|-----------------------------------------|--------------------------|
| `?new`   | instantiated any class on the classpath | rejected for every class |
| `?api`   | exposed the underlying Java API         | disabled                 |

A template that still uses them fails the request with `500` and logs:

```
freemarker.core._MiscTemplateException: Instantiating freemarker.template.utility.Execute
is not allowed in the template for security reasons.
```

Check your templates before upgrading:

```bash
rg -n '\?new|\?api' templates/
```

The background: `?new` could instantiate `freemarker.template.utility.Execute`, which runs OS
commands — reachable by any caller holding a per-tenant API key, and enough to read `ENCRYPTION_KEY`
out of the process environment. There is no opt-out flag, and there is deliberately no config to
re-enable it. Templates that need Java behaviour must get it from the model passed into the render.

---

### Configuring tenants

Without the `database` profile, tenant existence is validated against `tenants.config` — a list of
statically declared tenants provisioned at startup. You must declare at least the `default` tenant.

**YAML form** (e.g. mounted `application.yml`):

```yaml
tenants:
  config:
    - id: default
      displayName: Default
```

**Env var form** (Docker Compose `environment:`):

```yaml
environment:
  TENANTS_CONFIG_0_ID: default
  TENANTS_CONFIG_0_DISPLAY_NAME: Default
```

SMTP credentials are set globally via `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`,
`DEFAULT_SENDER`, `SMTP_ENABLE_TLS`, and `SMTP_ENABLE_SSL` — they apply to all tenants.

> With the `database` profile active, `TenantReconciliationService` provisions the `default` tenant
> automatically — `tenants.config` is then optional and only needed to pre-provision additional tenants.

---

### Security defaults in 0.5.0

Four defaults changed in ways an existing integration can notice.

**Tenant reads are admin-only.** `GET /v1/tenants` and `GET /v1/tenants/{slug}` now answer `403`
unless the caller holds `ROLE_ADMIN`. In OIDC mode that role comes from `realm_access.roles`, so the
Admin UI's tenant pages stay empty until your identity provider actually issues it. In API-key mode
the global `API_KEY` counts as admin while a per-tenant key does not. `GET /v1/tenants/me` is
unchanged and still returns the caller's own tenants — scripts that only needed their own tenant
should move to it.

**Actuator exposes `health` only.** `/actuator/info`, `/actuator/metrics` and everything else on
`:50004` answer `404`. Set `MANAGEMENT_ENDPOINTS` to a comma-separated list to widen it again — but
note the management port carries no authentication of its own, so it does not belong on a public interface.

**Every response carries security headers.** All three filter chains send a Content-Security-Policy,
`Referrer-Policy: same-origin` and HSTS. The policy is derived from the deployment: the configured
`auth.issuer-uri` is added to `connect-src`, `frame-src` and `form-action`. If your identity provider,
CDN or asset host lives somewhere else, replace the policy wholesale:

```yaml
environment:
  SECURITY_CONTENT_SECURITY_POLICY: "default-src 'self'; script-src 'self'; connect-src 'self' https://idp.example.com"
```

`script-src` is strict — the Admin UI ships without `unsafe-eval` and runs NGXS in its
CSP-compatible mode. Weakening it defeats the reason the header is there.

**HSTS behind a TLS terminator.** `server.forward-headers-strategy` is set to `framework`, so Spring
Security recognises HTTPS from the `X-Forwarded-*` headers your proxy sends. Without a TLS terminator
in front, HSTS is not emitted at all.

For the rest of the deployment hardening — profiles, credentials, port exposure — see
[`docker/README.md`](docker/README.md).

</details>

### Local Development

**Prerequisites:** JDK 25 (Temurin), Node.js 22, Docker. The Gradle wrapper (9.7.1) is in the repo.

**One-time setup — backend local config:**

The backend reads local overrides from `backend/src/main/resources/application-local.yml`,
which is gitignored. Create it from the example and generate an encryption key:

```bash
cd backend/src/main/resources
cp application-local.yml.example application-local.yml

# Generate a random AES-256 key and paste it into the `encryption.key` value
openssl rand -base64 32
```

Open `application-local.yml` and replace `encryption.key: CHANGE_ME_GENERATE_RANDOM_KEY`
with the generated value. Adjust SMTP, auth, and tenant settings as needed.

**Terminal 1 — Backend:**

```bash
cd backend
./gradlew bootRun
# API available at http://localhost:50003/api/
```

**Terminal 2 — Frontend:**

```bash
cd frontend
npm install
npm start
# Dev server at http://localhost:4200 (proxies /api → :50003)
```

### Docker Compose

The `docker/` directory contains four ready-to-use Compose stacks:

| Stack               | Auth            | Database   | Templates       |
|---------------------|-----------------|------------|-----------------|
| `minimal.yml`       | None (open)     | —          | File-based only |
| `apikey-sqlite.yml` | API-Key         | SQLite     | File + Database |
| `oidc-sqlite.yml`   | OIDC / Keycloak | SQLite     | File + Database |
| `oidc-postgres.yml` | OIDC / Keycloak | PostgreSQL | File + Database |

All four stacks publish their ports on `127.0.0.1` and ship development credentials. They are meant
for local work only — [`docker/README.md`](docker/README.md) lists what a real deployment has to change.

**Prerequisite:** Build the backend JAR first:

```bash
cd backend && ./gradlew bootJar
```

Start a stack (example — OIDC + SQLite):

```bash
cd docker && docker compose -f oidc-sqlite.yml up
```

> **Encryption key:** Each stack sets `services.backend.environment.ENCRYPTION_KEY`
> in its compose file. The default is a placeholder — generate a real 32-byte key
> with `openssl rand -base64 32` and replace it before any shared or production use.
> See [docker/README.md → Encryption Key](docker/README.md#encryption-key).

See [docker/README.md](docker/README.md) for detailed configuration per stack.

### Full Production Build (Backend serves Frontend)

```bash
cd backend
./gradlew build
# Angular is built and embedded into the Spring Boot JAR
```

Skip the Angular build (backend-only):

```bash
SKIP_FRONTEND_BUILD=true ./gradlew build
```

## CI / CD

Five GitHub Actions workflows automate builds, tests, and releases across the monorepo.

| Workflow    | Trigger                                    | Purpose                                              |
|-------------|--------------------------------------------|------------------------------------------------------|
| Feature CI  | push to `feature/**`, `fix/**`, `chore/**` | Build + test both components before a PR             |
| Backend CI  | push / PR to `main` (`backend/**`)         | Build, test, publish backend image                   |
| Frontend CI | push / PR to `main` (`frontend/**`)        | Unit tests + Playwright E2E                          |
| Monorepo CI | push to `main`                             | Full build + combined image to `ghcr.io`             |
| Release     | push to `master` or `v*.*.*` tag           | Production image with SBOM & provenance attestations |

See [`.github/workflows/README.md`](.github/workflows/README.md) for trigger details, job breakdown, and required
permissions.

## Template Sources

straightmail supports three independent template sources that can be combined:

| Source   | Description                                                     | Enabled by default                     |
|----------|-----------------------------------------------------------------|----------------------------------------|
| Database | Templates stored in SQLite / PostgreSQL (full CRUD via UI/API)  | Yes (profile `database`)               |
| File     | Read-only `.ftl` files mounted from the host filesystem         | Yes (`TEMPLATE_FILE_ENABLED=true`)     |
| Git-sync | Templates cloned from a per-tenant Git repository on a schedule | Yes (`TEMPLATE_GIT_SYNC_ENABLED=true`) |

### File Templates

Place FreeMarker templates under `templates/{tenant-id}/` in the project root. The directory is mounted into the
container at `/resources/templates`.

```
templates/
├── acme-1/
│   ├── welcome.ftl           # HTML body
│   ├── welcome_subject.ftl   # Subject line
│   └── welcome_plain.ftl     # Plain-text body (optional)
└── acme-2/
    └── ...
```

| Environment variable    | Default                | Description                          |
|-------------------------|------------------------|--------------------------------------|
| `TEMPLATE_FILE_ENABLED` | `false`                | Enable file-based template source    |
| `TEMPLATE_FILE_PATH`    | `/resources/templates` | Root directory scanned for templates |

### Git-Sync Templates

When enabled, the backend periodically clones a Git repository per tenant and loads `.ftl` files from the working tree.
The Git repo URL and optional access token are configured per tenant via the UI or API (stored encrypted in the
database).

| Environment variable        | Default       | Description                                          |
|-----------------------------|---------------|------------------------------------------------------|
| `TEMPLATE_GIT_SYNC_ENABLED` | `false`       | Enable Git-sync template source                      |
| `GIT_SYNC_CRON`             | `0 0 * * * *` | Cron expression for the sync job (hourly by default) |
| `GIT_SYNC_LOCK_MIN`         | `PT50M`       | Minimum lock duration (ShedLock)                     |
| `GIT_SYNC_LOCK_MAX`         | `PT1H`        | Maximum lock duration (ShedLock)                     |

Both sources are enabled in all Docker Compose variants. To disable one, set the corresponding variable to `"false"`.

## Authentication

straightmail supports two authentication modes configured at startup:

| Mode       | `AUTH_MODE` value | Description                                                                                                                                              |
|------------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| OIDC / JWT | `oidc` (default)  | JWT from an OIDC provider (e.g. Keycloak). Roles are derived from `realm_access.roles` → `ROLE_*`. The tenant is resolved from a configurable JWT claim. |
| API-Key    | `api-key`         | `X-API-KEY` request header carrying a global or per-tenant SHA-256 hash. The tenant is determined by a key lookup in the database.                       |
| None       | `none`            | All endpoints are open. Suitable for internal services with network-level protection.                                                                    |

Key environment variables:

| Variable               | Default | Description                                                                |
|------------------------|---------|----------------------------------------------------------------------------|
| `AUTH_MODE`            | `oidc`  | Authentication mode: `oidc`, `api-key`, or `none`                          |
| `JWT_TENANT_CLAIM`     | —       | JWT claim that contains the single tenant ID                               |
| `JWT_TENANT_IDS_CLAIM` | —       | JWT claim that contains a list of accessible tenant IDs                    |
| `API_KEY`              | —       | Global API key (SHA-256 hash) used when the `database` profile is inactive |

**Roles.** Tenant administration requires `ROLE_ADMIN`: every write in `/v1/tenants`, plus the reads
`GET /v1/tenants` and `GET /v1/tenants/{slug}`. In OIDC mode the role must appear in the token's
`realm_access.roles` — an otherwise valid token without it gets `403` and the Admin UI's tenant pages
stay empty. In API-key mode the global `API_KEY` is admin, a per-tenant key is not. `GET /v1/tenants/me`
is open to any authenticated caller and returns only their own tenants.

Deployment-level settings:

| Variable                           | Default  | Description                                                                    |
|------------------------------------|----------|--------------------------------------------------------------------------------|
| `ENCRYPTION_KEY`                   | —        | **Required.** AES-256 key, Base64-encoded (`openssl rand -base64 32`). A value that is not valid Base64 aborts startup |
| `MANAGEMENT_ENDPOINTS`             | `health` | Comma-separated actuator endpoints exposed on `:50004`                         |
| `SECURITY_CONTENT_SECURITY_POLICY` | derived  | Replaces the generated CSP wholesale; the default adds `auth.issuer-uri` to `connect-src`, `frame-src` and `form-action` |

## Operation Modes (Spring Profiles)

The backend behaviour is controlled by Spring profiles set via `SPRING_PROFILES_ACTIVE`:

| Profile      | Effect                                                                                                                                                                                                                                |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `database`   | Activates database storage for templates and SMTP config, enables the Tenant-CRUD API (`/v1/tenants`), and per-tenant Git-sync. Without this profile the backend runs file-only with a global SMTP config from environment variables. |
| `production` | Disables Swagger UI / API docs (`/swagger-ui`, `/v3/api-docs`), tunes Tomcat thread pool (`max=50`, `min-spare=10`).                                                                                                                  |
| `dev`        | Local development overrides (localhost SMTP relay).                                                                                                                                                                                   |

Example — full local stack:

```bash
SPRING_PROFILES_ACTIVE=dev,database ./gradlew bootRun
```

## Multi-Tenancy

When running with the `database` profile, straightmail is fully multi-tenant:

- **Per-tenant SMTP** — Each tenant configures its own SMTP server, credentials, and sender address via the UI or API.
- **Per-tenant Git-sync** — Each tenant can point to an independent Git repository for template storage, synced on a
  configurable cron schedule (ShedLock protected for HA setups).
- **Infrastructure-as-code provisioning** — Tenants declared under `tenants.config.*` in `application.yml` are
  automatically created or updated at startup (`TenantReconciliationService`). The `default` tenant is never removed,
  enabling zero-touch tenant provisioning without manual API calls.
- **Encryption at rest** — All secrets (SMTP password, Git token, API key) are encrypted using AES-256-GCM by the
  built-in `EncryptionService`. Secrets are never returned in plain text by the API.

## Documentation

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
- [API Endpoints](http://localhost:50003/swagger-ui.html)

## Ports

| Service     | Port(s)     | Note                                                                          |
|-------------|-------------|-------------------------------------------------------------------------------|
| Backend API | 50003       | serves the API and the bundled Admin UI                                       |
| Management  | 50004       | actuator, `health` only by default, **no authentication** — keep it internal  |
| Frontend    | 4200        | dev server only                                                               |
| PostgreSQL  | 5432        |                                                                               |
| Mailpit     | 1025 / 8025 |                                                                               |
| pgAdmin     | 5050        |                                                                               |

## encircle360 OSS Matrix Channel

Join our community for support, discussions, and updates regarding our open-source projects.

[![Matrix](https://img.shields.io/badge/Matrix-Join%20Chat-0dbd8b?logo=matrix&logoColor=white)](https://matrix.to/#/#oss:encircle360.com)
