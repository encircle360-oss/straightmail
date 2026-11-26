# AGENTS.md

This file provides guidance to coding agents working with code in this repository.

## Commands

### Backend (Spring Boot / Java 25)

```bash
cd backend

# Run dev server (API at http://localhost:50003/api/; needs application-local.yml, see below)
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# Run with database profile
SPRING_PROFILES_ACTIVE=local,database ./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.encircle360.oss.straightmail.TemplatesControllerTest"

# Build JAR (required before Docker Compose)
./gradlew bootJar

# Full production build (embeds Angular frontend into JAR)
./gradlew build

# Skip Angular during backend-only builds
SKIP_FRONTEND_BUILD=true ./gradlew build
```

### Frontend (Angular 22)

```bash
cd frontend

npm install
npm start              # Dev server at http://localhost:4200 (proxies /api → :50003)
npm test               # Unit tests via Vitest (Angular builder)
npm run test:e2e       # Playwright e2e tests (starts the :4299 server, or reuses it outside CI)
npm run start:e2e      # Serve on :4299 for Playwright
npm run test:e2e:ui    # Playwright interactive UI
```

### Docker Compose

```bash
# Build JAR first (one-time or after backend changes)
cd backend && ./gradlew bootJar

# Start a stack from repo root
cd docker && docker compose -f oidc-sqlite.yml up
# Stacks: minimal.yml | apikey-sqlite.yml | oidc-sqlite.yml | oidc-postgres.yml
```

### One-time local setup

```bash
cd backend/src/main/resources
cp application-local.yml.example application-local.yml
openssl rand -base64 32   # paste output into encryption.key in application-local.yml
```

## Architecture

### Monorepo layout

```
backend/    Spring Boot 4 / Java 25 — mail API + FreeMarker renderer
frontend/   Angular 22 — admin console
docker/     Four Compose stacks (minimal, apikey-sqlite, oidc-sqlite, oidc-postgres)
```

### Backend

The backend is a standard Spring Boot layered app (`controller → service → repository`). Key design points:

**Template provider chain** — `CompositeTemplateProvider` aggregates the active `TemplateSourceProvider` beans in injection order: `DatabaseTemplateSourceProvider` (`database` profile), `FileTemplateSourceProvider` (`templates.file.enabled`), and `GitTemplateSourceProvider` (`templates.git-sync.enabled`). First-match wins on resolve; listing merges all and slices in-memory. New template sources implement `TemplateSourceProvider` and are automatically picked up.

**Template loaders** — `FreemarkerService` drives rendering. `EmailService` resolves templates through `CompositeTemplateProvider`; only in single-tenant operation (no `database` profile, at most one tenant in `tenants.config`) does a miss fall back to the flat 0.4.x layout through `FileTemplateLoader`, an implementation of the project's own `TemplateLoader` interface. `GitSyncService` stores synced Git templates in a `GitTemplateRegistry`: `DatabaseGitTemplateRegistry` (JPA entity `GitTemplateCache`, `database` profile) or `InMemoryGitTemplateRegistry` otherwise.

**Multi-tenancy** — `TenantContext` (request-scoped bean) carries the current tenant ID. `JwtTenantClaimsExtractor` reads the tenant claims from the JWT for `JwtTenantResolutionFilter`. With the `database` profile, `TenantReconciliationService` creates tenants declared in `tenants.config` that are missing from the database at startup. SMTP config and Git repository are stored per tenant; the SMTP password and Git token are encrypted with AES-256-GCM via `EncryptionService`, per-tenant API keys are stored as a SHA-256 hash.

**Spring profiles**:
- `database` — activates JPA, Liquibase, template and tenant CRUD, per-tenant API keys, and Git-sync DB storage. Without it tenants come from `tenants.config` and Git-synced templates are kept in memory.
- `production` — disables the OpenAPI document (`/api/v3/api-docs`) and with it Swagger UI, tunes Tomcat thread pool.
- `local` — reads `application-local.yml` (gitignored) for local development.

**Auth modes** (env `AUTH_MODE`): `oidc` (default, JWT), `api-key` (`X-API-KEY` header: the global `API_KEY`, compared as given, or a per-tenant key matched by its SHA-256 hash with the `database` profile), `none` (open).

**Ports**: API `:50003`, management/actuator `:50004`.

**Database migrations** — Liquibase changesets live in `backend/src/main/resources/db/changelog/`. Schema is applied on startup when `database` profile is active.

**Controllers**:
- `MailController` — `/api/v1/email` (template by ID) and `/api/v1/email/inline` (inline FreeMarker)
- `RenderController` — `/api/v1/render`, preview rendering without sending
- `TemplatesController` — create, update and delete for DB-stored templates (requires `database` profile)
- `TemplateReadController` — read-only template listing across all sources
- `TenantAdminController` — tenant create, update and delete (requires `database` profile + admin role)
- `TenantReadController` — tenant listing and lookup (admin role) and `/api/v1/tenants/me` for the caller's tenants
- `StatusController` — `/api/v1/status` (Git-sync status per tenant, file-template directory accessibility) and `POST /api/v1/sync/git/{tenantSlug}`
- `InfoController` — `/api/v1/info` (application name, status, auth mode, API URL and OIDC issuer for the frontend)

**Tests** use `AbstractTest` which spins up a Mailpit Testcontainer and in-memory SQLite. Tests that send real mail inherit from `AbstractTest`; unit tests stand alone.

### Frontend

Angular 22 with standalone components and NGXS state management.

**State slices** (`src/app/store/`): `auth`, `api-key`, `tenant`, `toast`, `ui`. State is persisted via `@ngxs/storage-plugin`.

**Auth** — `angular-auth-oidc-client` handles OIDC silent renewal and token injection via `authInterceptor`. `apiKeyInterceptor` handles `X-API-KEY` mode. The active mode is detected from the backend's `/api/v1/info` endpoint.

**API layer** — `ApiService` (`core/services/api.service.ts`) is the single HTTP abstraction, typed against the backend DTOs.

**Routing** — lazy-loaded pages: `dashboard`, `templates`, `tenants`, `send`, `render`, `api-key-login`, `unauthorized`.

**Template editor** — CodeMirror 6 with HTML/JSON language support for editing FreeMarker bodies inline.

**i18n** — `@ngx-translate` with English (`en`) and German (`de`) JSON bundles under `frontend/public/assets/i18n/`.

**E2E tests** — Playwright in `frontend/tests/`, run against the `:4299` e2e-specific serve target (auth-mode: `api-key`).

### Template file naming convention

```
{TEMPLATE_FILE_PATH}/{tenant-id}/
  {templateId}.ftl          # HTML body
  {templateId}_subject.ftl  # Subject (HTML stripped)
  {templateId}_plain.ftl    # Plain-text body (optional)
```

### Encryption

The persisted SMTP password and Git token are encrypted at rest; per-tenant API keys are stored only as a SHA-256 hash. The key comes from `ENCRYPTION_KEY` env var (AES-256-GCM). Never return plain-text secrets from the API — `EncryptionService` is the only place that decrypts.

## Code Conventions — Backend

### DTOs

Mutable request/response DTOs use this Lombok combination:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateUpdateTenant")
public class CreateUpdateTenantDTO { ... }
```

- Use `@SuperBuilder` instead of `@Builder` when inheritance is involved (`EmailTemplateFileRequestDTO extends EmailRequestDTO`)
- Use `@Builder.Default` for fields with default values
- Java Records **only** for truly immutable value objects (e.g. `GitSyncStatusDTO`, `FileStatusDTO`)
- No `I`-prefix on interfaces

### Entities

```java
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "tenants")
public class Tenant { ... }
```

- Never use `@Data` on entities — always explicit `@Getter`/`@Setter`
- Hibernate-safe `equals()`/`hashCode()` via `HibernateProxy` check (copy the pattern from existing entities)
- Generated PK: `@GeneratedValue(strategy = GenerationType.UUID)`
- Natural String PK (slug) only for `Tenant`

### Controllers

- Return type always `ResponseEntity<T>` — never a bare type
- Inline error handling: `orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "..."))`
- No global `@RestControllerAdvice`
- Pagination via `PageContainer<T>`, not Spring's `Page<T>` directly
- Every endpoint gets `@Operation(operationId = "...", description = "...")` + `@Tag`
- `@Valid` on every `@RequestBody` parameter

### Lombok Quick Reference

| Context | Annotations |
|---------|-------------|
| Service / Controller | `@RequiredArgsConstructor` (all fields `final`) |
| DTO (mutable) | `@Data + @Builder (or @SuperBuilder) + @NoArgsConstructor + @AllArgsConstructor` |
| Entity | `@Getter + @Setter` (no `@Data`) |
| Logging | `@Slf4j` |

### MapStruct Mappers

- `@Mapper(componentModel = "spring")` — registered as Spring bean
- `INSTANCE` singleton only when no Spring context is available
- Sensitive fields **always** explicitly ignored:
  ```java
  @Mapping(target = "smtpPassword", ignore = true)
  @Mapping(target = "gitToken", ignore = true)
  @Mapping(target = "apiKeyHash", ignore = true)
  Tenant fromDto(CreateUpdateTenantDTO dto);
  ```
- Update methods use `@MappingTarget`

---

## Code Conventions — Frontend

### Formatting

Prettier (`.prettierrc`): 100-char width, single quotes, Angular HTML parser. Run before committing.

### Components

- Always standalone (the Angular default; some components still set `standalone: true`) — no NgModules
- Reactivity via Signals: `signal()`, `computed()`, `toSignal()` for RxJS interop
- `ChangeDetectionStrategy.OnPush` on presentational/dumb components
- `input()` and `output()` instead of `@Input`/`@Output` decorators

### Dependency Injection

`inject()` everywhere — no constructor injection:

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly store = inject(Store);
  private readonly oidcSecurityService = inject(OidcSecurityService);
}
```

### Guards

Functional guards (`CanActivateFn` / `CanDeactivateFn`) — no class-based guards.

### NGXS State

Actions in namespaces, states with generic type parameter:

```typescript
export namespace TenantActions {
  export class LoadTenants { static readonly type = '[Tenant] Load Tenants'; }
  export class SelectTenant { constructor(public tenantId: string) {} }
}

@State<TenantStateModel>({ name: 'tenant', defaults: { ... } })
@Injectable()
export class TenantState {
  @Selector() static selectedId(state: TenantStateModel): string { ... }
}
```

### Naming Conventions

File names: `*.component.ts` · `*.service.ts` · `*.guard.ts` · `*.state.ts` · `*.actions.ts` · `*.model.ts` · `*.resolver.ts` · `*.interceptor.ts`

- No barrel exports (`index.ts`)
- No `I`-prefix on interfaces

---

## Architecture Patterns

### Composite Template Provider

`CompositeTemplateProvider` aggregates all `TemplateSourceProvider` beans via Spring injection.
**Priority = bean registration order**: Database → File → Git.
Adding a new template source means implementing `TemplateSourceProvider`, annotating it `@Service`,
and it is picked up automatically — no changes needed in `CompositeTemplateProvider`.

Listing aggregates all sources in-memory first, then slices — heterogeneous sources cannot be
jointly paginated at the query level. This is an intentional design decision, not a bug.

### Request-Scoped Tenant Context

`TenantContext` is a request-scoped bean that holds the current tenant ID. Never pass the tenant ID as a
method parameter through service layers — inject `TenantContext` (or `ObjectProvider<TenantContext>`
where code may run outside a request) and call `getTenantId()`.
Resolution happens in the tenant resolution filters: `JwtTenantResolutionFilter` (OIDC, via
`JwtTenantClaimsExtractor`), `ApiKeyAuthenticationFilter` / `ApiKeyTenantResolutionFilter` (API key),
`NoAuthTenantResolutionFilter` (none).

### Spring Profile Feature Gating

Features are activated via Spring profiles and configuration properties, not in-code feature flags.
`@Profile(DatabaseConfig.PROFILE)` and `@ConditionalOnProperty` (e.g. `auth.mode`,
`templates.git-sync.enabled`) are the standard patterns.
Any feature that only works with the `database` profile must be explicitly annotated.

### Tenant Reconciliation at Startup

With the `database` profile, `TenantReconciliationService` reads `tenants.config.*` from `application.yml`
and creates missing tenants on startup. Existing tenants are never modified or deleted.
Declare new tenants in config — no manual API calls required.

---

## Security Conventions

### Encryption at Rest

The persisted SMTP password and Git token pass through `EncryptionService` (AES-256-GCM); per-tenant
API keys are not encrypted but stored as a SHA-256 hash (`TenantService.hashApiKey`).
Decryption happens exclusively through `EncryptionService` — never in controllers or DTOs.
The API **never** returns plain-text secrets.

### MapStruct: Always Ignore Sensitive Fields

Every mapper that maps to/from an entity with secrets must explicitly ignore all secret fields
with `@Mapping(target = "...", ignore = true)`. When a new secret field is added to an entity,
**all** affected mapper methods must be updated immediately.

### Frontend: Auth-Mode Gating

Check `environment.authMode` before any auth logic (follow the `authGuard` pattern); it returns the
auth mode the backend reports through `/api/v1/info`, fetched in `main.ts` before bootstrap.
No hard-coded auth bypass in code. `AUTH_MODE=none` is the only legitimate config for open
endpoints — exclusively for internal services protected at the network level.

### Production Profile

The `production` profile disables the OpenAPI document (`/api/v3/api-docs`) and with it Swagger UI.
Never use `production` in local dev; never run production without it.
