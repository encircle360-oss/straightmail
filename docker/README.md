# straightmail — Docker Compose Modes

This folder contains four pre-configured Compose stacks, one per operating mode.
Each stack starts fully without any manual configuration: databases, Keycloak (including
an imported realm), Mailpit, and the app are ready to use immediately.

## These stacks are for local development

All four stacks bind their ports to `127.0.0.1`, ship dev credentials (`admin/admin` for
Keycloak, `straightmail/straightmail` for Postgres, `dev-api-key-change-me` for the API key,
plus a checked-in `ENCRYPTION_KEY` per stack) and run without the `production` Spring profile,
so Swagger UI and `/api/v3/api-docs` are reachable. That is intended here and unsafe anywhere else.

For a real deployment, start from these files but change at least the following:

- set `SPRING_PROFILES_ACTIVE` to include `production` — this disables Swagger UI and the API docs
- generate a fresh `ENCRYPTION_KEY` (`openssl rand -base64 32`) and keep it out of the compose file
- replace every credential above
- do not publish the management port `50004`; it carries no authentication of its own
- put a TLS terminator in front, otherwise HSTS is not sent and the `/api/v1/info` response that
  configures the client's OIDC authority travels in clear text

Upgrading an existing installation rather than starting fresh? The migration guide in the
[root README](../README.md#migrating-from-04x) lists what changed between 0.4.x and 0.5.0.

---

## Prerequisites (Build)

Every stack builds its backend image from `../backend/Dockerfile`, which copies a pre-compiled
JAR — **before the first `docker compose up`, a one-time build is required:**

```bash
# Build the backend JAR (from the repo root). This also builds the Admin UI into the JAR;
# with SKIP_FRONTEND_BUILD=true the stacks run without it.
cd backend && ./gradlew bootJar && cd ..
```

After code changes, run `./gradlew bootJar` again, then `docker compose ... up --build`.

---

## System Requirements

- **Docker** (Desktop or Engine) — required for all modes
- The commands below run from this `docker/` directory. From the repository root, pass the file as
  `-f docker/<mode>.yml`; Compose resolves the relative paths in the file against `docker/` either way.
- File templates are mounted from `docker/data/files/` (gitignored, so create it), one sub-directory
  per tenant — e.g. `docker/data/files/default/welcome.ftl` for `minimal.yml`.

---

## Overview

| File                | Auth            | Database   | Templates                        | Additional Services           |
|---------------------|-----------------|------------|----------------------------------|-------------------------------|
| `minimal.yml`       | None            | —          | Files, Git-sync                  | Mailpit                       |
| `apikey-sqlite.yml` | API-Key         | SQLite     | Database (CRUD), Files, Git-sync | Mailpit                       |
| `oidc-sqlite.yml`   | OIDC / Keycloak | SQLite     | Database (CRUD), Files, Git-sync | Mailpit, Keycloak             |
| `oidc-postgres.yml` | OIDC / Keycloak | PostgreSQL | Database (CRUD), Files, Git-sync | Mailpit, Keycloak, PostgreSQL |

In every stack the Admin UI (when the JAR embeds it) and the API are served by the backend on
`http://localhost:50003`.

---

## Modes

### `minimal.yml` — No Auth, File Templates

The simplest stack for quickly testing the API with file templates.
No Spring `database` profile → no JPA; all endpoints are open. It declares the tenant `default`.

```bash
docker compose -f minimal.yml up
```

| Service        | URL                        |
|----------------|----------------------------|
| Backend API    | http://localhost:50003/api |
| Mailpit Web UI | http://localhost:8025      |

No credentials or headers required.

---

### `apikey-sqlite.yml` — API-Key Auth, SQLite, DB Templates

Full stack with API-key authentication and tenant management.
The tenants `acme-1` and `acme-2` are created in the DB on the first backend start from the
`TENANTS_CONFIG_*` environment variables (`TenantReconciliationService`); later starts leave
existing tenants unchanged.

```bash
docker compose -f apikey-sqlite.yml up
```

| Service        | URL                        | Credentials                                                |
|----------------|----------------------------|------------------------------------------------------------|
| Admin UI       | http://localhost:50003     | API key `dev-api-key-change-me`                            |
| Backend API    | http://localhost:50003/api | `X-API-KEY: dev-api-key-change-me` + `X-Tenant-ID: acme-1` |
| Mailpit Web UI | http://localhost:8025      | —                                                          |

The API key is set as `API_KEY` in `apikey-sqlite.yml`; change it there for any non-local
environment. Additional tenants can be configured by adding `TENANTS_CONFIG_2_*`, `TENANTS_CONFIG_3_*`,
etc.

---

### `oidc-sqlite.yml` — OIDC Auth, SQLite, Keycloak

Full OIDC stack. Keycloak starts with the pre-configured `straightmail` realm
(clients `straightmail` and `acme-backend`, `tenant_ids` mapper, `ADMIN` role, test user).
No manual Keycloak setup required.

```bash
docker compose -f oidc-sqlite.yml up
```

| Service        | URL                        | Credentials                 |
|----------------|----------------------------|-----------------------------|
| Admin UI       | http://localhost:50003     | `admin` / `admin`           |
| Backend API    | http://localhost:50003/api | Bearer token (via Keycloak) |
| Keycloak Admin | http://localhost:8090      | `admin` / `admin`           |
| Mailpit Web UI | http://localhost:8025      | —                           |

The test user `admin` has:

- Attribute `tenant_ids` = `acme-1`, `acme-2` (passed as the `tenant_ids` JWT claim)
- Realm role `ADMIN`

Because the user has two tenants, API requests need `X-Tenant-ID`. The backend waits for Keycloak
via a `healthcheck` before starting.

---

### `oidc-postgres.yml` — OIDC Auth, PostgreSQL, Keycloak

Same as `oidc-sqlite.yml`, but with PostgreSQL 16 instead of SQLite.

```bash
docker compose -f oidc-postgres.yml up
```

| Service        | URL                        | Credentials                                        |
|----------------|----------------------------|----------------------------------------------------|
| Admin UI       | http://localhost:50003     | `admin` / `admin`                                  |
| Backend API    | http://localhost:50003/api | Bearer token (via Keycloak)                        |
| Keycloak Admin | http://localhost:8090      | `admin` / `admin`                                  |
| PostgreSQL     | `localhost:5432`           | `straightmail` / `straightmail` DB: `straightmail` |
| Mailpit Web UI | http://localhost:8025      | —                                                  |

---

## General Notes

### Stop a stack and remove volumes

```bash
# Stop (keep volumes):
docker compose -f <mode>.yml down

# Stop and remove all volumes (clean restart):
docker compose -f <mode>.yml down -v
```

### View logs for a specific service

```bash
docker compose -f <mode>.yml logs -f backend
docker compose -f <mode>.yml logs -f keycloak
```

### Rebuild without the Docker build cache

```bash
docker compose -f <mode>.yml build --no-cache
docker compose -f <mode>.yml up
```

### Encryption Key

Each stack sets the encryption key via `services.backend.environment.ENCRYPTION_KEY`
in its compose file. The `application.yml` fallback is not valid Base64, so a backend without
`ENCRYPTION_KEY` does not start. The shipped value is a development key and **must not** be used
for production or shared environments. For staging/prod, set a dedicated 32-byte Base64 key:

```bash
# Generate a new key:
openssl rand -base64 32
```
