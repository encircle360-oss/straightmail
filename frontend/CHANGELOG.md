# Changelog

All notable changes to the straightmail client will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.5.0] - 2026-09-17

### Added

- Multi-tenancy UI: tenant list, create/edit/delete forms, tenant selector in sidebar
- API-key authentication mode: `/api-key-login` page, `ApiKeyState`, `apiKeyInterceptor`
- `TenantState.isDatabaseMode` selector; DB-mode-only actions (New Template, New Tenant) conditionally rendered
- `TemplatesState` with source-tab filtering (`DATABASE` / `FILE` / `GIT` / `ALL`) and tag filtering
- `TemplateGridComponent` and grid/list view toggle (persisted in `UiState`)
- Read-only `TemplateViewComponent` for GIT/FILE templates (non-editable)
- Git branch badge (`git-branch-badge`) in template table/grid for `source:git` templates
- `DashboardState` tracking Git sync status per tenant
- `FileSyncCard` / `GitSyncCard` components for sync status display
- `UserAvatar`, `TenantAvatar`, `NavIconComponent` reusable UI components
- `HelpPanelComponent`: context-sensitive off-canvas help panel (right panel / bottom sheet on mobile)
- Mobile tab navigation and dynamic iframe resizing on render page
- Tab persistence and validation in tenant form
- Auto-save for render form values (persisted in `LocalStorage` via `RenderState`)
- ngx-translate (`TranslatePipe`) localization for all new tenant and template UI strings
- Playwright E2E test suites: `templates.spec.ts`, `tenants.spec.ts`
- `autoAuth` Playwright fixture: pre-seeds sessionStorage/localStorage before each test
- `editable: true` added to default tenant fixture so `isDatabaseMode()` resolves correctly in E2E tests

### Changed

- Angular 22.2, Vitest 5, Playwright 1.64, npm 12 and all other dependencies on their latest
  releases; TypeScript stays on 6.0 because Angular 22 requires `>=6.0 <6.1`
- `vite` overridden to 8.3.5: `@angular/build` pins 8.3.0, which is affected by
  GHSA-9jrq-w75r-8gcw, GHSA-rq7h-c2jc-7f22, GHSA-vfpm-58rq-9qcg and the `launch-editor` advisories
- `TranslateModule` imports replaced by standalone `TranslatePipe` across all components
- MailHog references replaced with Mailpit in E2E test configurations
- Toast auto-dismiss delay adjusted; locale handling simplified in template options
- Unsaved-changes guard unified via `canDeactivate` across form pages
- Frontend serving and auth configuration consolidated in Docker Compose setups
- Storage keys updated for NGXS persistence plugin compatibility
- Font sizes standardized to `rem` units across all component stylesheets
- Angular upgraded to 22.1.x, Node.js to 22
- NGXS runs with `compatibility.strictContentSecurityPolicy` so the app works under the backend's
  `script-src 'self'` policy without `unsafe-eval`
- Shared form fields bound via `[formControl]` instead of `ngModel`
- i18n bundles cover the API-key login page and its error messages

### Fixed

- `mockTenants` E2E helper now mocks `/v1/tenants/me` (the actual `LoadTenants` endpoint) instead of `/v1/tenants`
- Playwright `autoAuth` fixture seeds tenant with `editable: true` so "New Template" / "New Tenant" buttons render
- Production builds shipped the development environment: `angular.json` had no `fileReplacements`, so
  every production bundle carried the `http://localhost:50003/api` fallback, `http://localhost:8090` as
  the OIDC issuer fallback and `logLevel 1`, which logged the auth flow to the browser console
- Interceptors computed their base path with `new URL(environment.apiUrl)` and no base argument, which
  throws for the relative `/api` the backend returns by default — it only worked because the dev
  environment's absolute fallback was shipping
- Bearer token and API key are scoped by origin as well as path; a request to a foreign host whose path
  happens to start with the API base no longer receives credentials

### Removed

- `public/env.js` runtime-injection shim, left over from the standalone frontend image; the Admin UI is
  served by the backend and reads its configuration from `/api/v1/info`

## [0.2.0] - 2026-03-16

> **Note:** First release of the Angular admin console client, built from scratch and introduced as part of the monorepo restructure.

### Added

- Angular 21 admin console frontend integrated into the monorepo as `client/`
- OIDC authentication via `angular-auth-oidc-client`
- Unauthorized page with auth error handling
- 404 not-found page with route fallback
- Grid layout for the main dashboard view
- NGXS state management
- CodeMirror 6 for syntax highlighting in email template views
- Playwright E2E test configuration
- GitHub Actions workflow for client-only CI/CD builds
- Nginx-based Docker container running as unprivileged user on port 8080
  - Nginx config updated to use `/tmp` for cache and PID paths
  - Multi-platform Docker builds (amd64 + arm64)
- `entrypoint.sh` for runtime injection of environment variables into `env.js`
- Runtime injection of `API_URL` via environment variable (with fallback logic in `ApiService`)
- Runtime injection of `OIDC_AUTHORITY` via environment variable (with fallback logic in `app.config.ts`)
- Runtime injection of `AUTH_ENABLED` flag via environment variable (with fallback logic in `environment.ts`)
- `GIT_COMMIT_HASH` injected as a build-time argument (truncated to 7 characters) for version display
- Version info generation script in `package.json` build pipeline
- API mock routes aligned with `/api/v1` prefix for local dev server proxy

### Changed

- API base URL moved from hardcoded value to runtime-injectable `API_URL` environment variable
- `env.js` moved from `assets/` to `public/` directory
- `entrypoint.sh` switched from `cp` to `cat` for `env.js` injection to support unprivileged file access
- Dockerfile file ownership configured for unprivileged nginx operation
- Client port updated to 8080 across Dockerfile and Docker Compose
- Angular build output path set to `dist` for browser builds
- Dashboard and API status card status binding logic refactored
- API base URL conditionally displayed only in non-production environments
- Applied consistent code formatting across the client codebase
