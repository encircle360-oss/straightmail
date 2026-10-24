# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot / Java 25)

```bash
cd backend

# Run dev server (API at http://localhost:50003/api/)
./gradlew bootRun

# Run with database profile
SPRING_PROFILES_ACTIVE=dev,database ./gradlew bootRun

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
npm run test:e2e       # Playwright e2e tests (requires app at :4299)
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

**Template provider chain** — `CompositeTemplateProvider` aggregates three `TemplateSourceProvider` beans in injection order: `DatabaseTemplateSourceProvider`, `FileTemplateSourceProvider`, and `GitTemplateSourceProvider`. First-match wins on resolve; listing merges all and slices in-memory. New template sources implement `TemplateSourceProvider` and are automatically picked up.

**Template loaders** — `FreemarkerService` drives rendering. Each source has its own `TemplateLoader` subclass (`DatabaseTemplateLoader`, `FileTemplateLoader`) that adapts to FreeMarker's `TemplateLoader` interface. Git templates are cached in `GitTemplateCache` (JPA entity) by the `GitSyncService` and served through `DatabaseGitTemplateRegistry`.

**Multi-tenancy** — `TenantContext` (thread-local) carries the current tenant ID. `JwtTenantClaimsExtractor` resolves tenants from JWT claims. `TenantReconciliationService` auto-provisions tenants declared in `tenants.config.*` at startup. SMTP config, Git repo URL, and access token are stored per tenant, encrypted with AES-256-GCM via `EncryptionService`.

**Spring profiles**:
- `database` — activates JPA, Liquibase, Tenant CRUD, and Git-sync DB storage. Without it the app runs file-only with global SMTP env vars.
- `production` — disables Swagger UI, tunes Tomcat thread pool.
- `dev` — localhost SMTP relay defaults.

**Auth modes** (env `AUTH_MODE`): `oidc` (default, JWT), `api-key` (`X-API-KEY` header, SHA-256 hash), `none` (open).

**Ports**: API `:50003`, management/actuator `:50004`.

**Database migrations** — Liquibase changesets live in `backend/src/main/resources/db/changelog/`. Schema is applied on startup when `database` profile is active.

**Controllers**:
- `MailController` — `/api/v1/mail/send` (template by ID) and `/inline` (inline FreeMarker)
- `RenderController` — preview rendering without sending
- `TemplatesController` — full CRUD for DB-stored templates (requires `database` profile)
- `TemplateReadController` — read-only template listing across all sources
- `TenantAdminController` — tenant CRUD (requires `database` profile + admin role)
- `TenantReadController` — current tenant info
- `StatusController` / `InfoController` — health and version endpoints

**Tests** use `AbstractTest` which spins up a Mailpit Testcontainer and in-memory SQLite. Tests that send real mail inherit from `AbstractTest`; unit tests stand alone.

### Frontend

Angular 22 with standalone components and NGXS state management.

**State slices** (`src/app/store/`): `auth`, `api-key`, `tenant`, `toast`, `ui`. State is persisted via `@ngxs/storage-plugin`.

**Auth** — `angular-auth-oidc-client` handles OIDC silent renewal and token injection via `AuthInterceptor`. `ApiKeyInterceptor` handles `X-API-KEY` mode. The active mode is detected from the backend's `/api/v1/info` endpoint.

**API layer** — `ApiService` (`core/services/api.service.ts`) is the single HTTP abstraction, typed against the backend DTOs.

**Routing** — lazy-loaded pages: `dashboard`, `templates`, `tenants`, `send`, `render`, `api-key-login`, `unauthorized`.

**Template editor** — CodeMirror 6 with HTML/JSON language support for editing FreeMarker bodies inline.

**i18n** — `@ngx-translate` with English (`en`) and German (`de`) JSON bundles under `src/assets/i18n/`.

**E2E tests** — Playwright in `frontend/tests/`, run against the `:4299` e2e-specific serve target (auth-mode: `none`).

### Template file naming convention

```
templates/{tenant-id}/
  {templateId}.ftl          # HTML body
  {templateId}_subject.ftl  # Subject (HTML stripped)
  {templateId}_plain.ftl    # Plain-text body (optional)
```

### Encryption

All persisted secrets (SMTP password, Git token, API key) are encrypted at rest. The key comes from `ENCRYPTION_KEY` env var (AES-256-GCM). Never return plain-text secrets from the API — `EncryptionService` is the only place that decrypts.

## Code Conventions — Backend

### DTOs

Mutable request/response DTOs always use this Lombok combination:

```java
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateUpdateTenantDTO", description = "...")
public class CreateUpdateTenantDTO { ... }
```

- Use `@SuperBuilder` instead of `@Builder` when inheritance is involved (`CreateUpdateTemplateDTO extends EmailRequestDTO`)
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
- Every endpoint gets `@Operation(summary = "...")` + `@Tag`
- `@Valid` on every `@RequestBody` parameter

### Lombok Quick Reference

| Context | Annotations |
|---------|-------------|
| Service / Controller | `@RequiredArgsConstructor` (all fields `final`) |
| DTO (mutable) | `@Data + @SuperBuilder + @NoArgsConstructor + @AllArgsConstructor` |
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

- Always `standalone: true` — no NgModules
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

### Thread-Local Tenant Context

`TenantContext` holds the current tenant ID in a ThreadLocal. Never pass the tenant ID as a
method parameter through service layers — always call `TenantContext.get()`.
Resolution happens in `JwtTenantClaimsExtractor` (OIDC) or via API-key lookup.

### Spring Profile Feature Gating

Features are activated via Spring profiles, not in-code feature flags.
`@Profile("database")` and `@ConditionalOnMissingBean` are the standard patterns.
Any feature that only works with the `database` profile must be explicitly annotated.

### Tenant Reconciliation at Startup

`TenantReconciliationService` reads `tenants.config.*` from `application.yml` and provisions
missing tenants on startup. The `default` tenant is never deleted.
Declare new tenants in config — no manual API calls required.

---

## Security Conventions

### Encryption at Rest

All persisted secrets (SMTP password, Git token, API key hash) pass through `EncryptionService` (AES-256-GCM).
Decryption happens exclusively there — never in controllers or DTOs.
The API **never** returns plain-text secrets.

### MapStruct: Always Ignore Sensitive Fields

Every mapper that maps to/from an entity with secrets must explicitly ignore all secret fields
with `@Mapping(target = "...", ignore = true)`. When a new secret field is added to an entity,
**all** affected mapper methods must be updated immediately.

### Frontend: Auth-Mode Gating

Check `environment.authMode` before any auth logic (follow the `authGuard` pattern).
No hard-coded auth bypass in code. `AUTH_MODE=none` is the only legitimate config for open
endpoints — exclusively for internal services protected at the network level.

### Production Profile

The `production` profile disables Swagger UI and `/v3/api-docs`.
Never use `production` in local dev; never use `dev` or no profile in production.
