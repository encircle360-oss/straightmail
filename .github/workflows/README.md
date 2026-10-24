# CI / CD Workflows

straightmail uses seven GitHub Actions workflows to cover the full development, security and
release lifecycle — from feature branch validation through to production image publishing with
supply-chain attestations and registry retention.

The repository's default branch is `master`. Workflows that watch a trunk accept both `master`
and `main`, so a rename does not silently disable CI.

## Overview

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| Feature Branch CI | [`feature.yml`](feature.yml) | push to `feature/**`, `fix/**`, `chore/**` | Validate before a PR is opened |
| Backend CI/CD | [`backend.yml`](backend.yml) | push / PR to `master` or `main` (`backend/**`) | Build, test, scan, publish backend image |
| Frontend CI | [`frontend.yml`](frontend.yml) | push / PR to `master` or `main` (`frontend/**`) | Unit tests + Playwright E2E |
| Monorepo Full Build | [`monorepo.yml`](monorepo.yml) | push to `master` or `main` | Full build + combined image |
| Release | [`release.yml`](release.yml) | push to `master` / `main`, tag `v*.*.*` | Production image with SBOM & provenance |
| CodeQL | [`codeql.yml`](codeql.yml) | push / PR to `master` or `main`, weekly cron | Static analysis (Java + TypeScript) |
| Cleanup Old Packages | [`cleanup.yml`](cleanup.yml) | weekly cron, `workflow_dispatch` | GHCR retention for the published images |

---

## Workflows

### `feature.yml` — Feature Branch CI

Runs on every push to `feature/**`, `fix/**`, or `chore/**`. Ensures that both halves build and
pass their tests before a pull request is opened. Frontend linting is best-effort (`npm run lint ||
echo …`), so a missing lint script never fails the branch. Test reports are uploaded on failure
only, for quick diagnosis.

**Jobs:** `backend` (Gradle build + test, `SKIP_FRONTEND_BUILD=true`) · `frontend` (lint + Vitest + Playwright)  
**Artifacts (on failure, 3 days):** `backend-test-reports` (JUnit + JaCoCo) · `playwright-report`

---

### `backend.yml` — Backend CI/CD

Runs on push and pull requests to `master` or `main` that touch `backend/**` or the workflow file
itself. Builds the JAR with Java 25 and Gradle, runs the test suite in a separate job, and — on
direct pushes to a trunk branch — publishes a backend-only multi-arch image, scans it, and attests
its provenance. The `docker` job is skipped on pull requests.

**Jobs:** `build` (JAR artifact, 1 day) · `test` · `docker` (`needs: [build, test]`, trunk only)  
**Image:** `ghcr.io/codeministry/straightmail/backend` — `linux/amd64`, `linux/arm64`  
**Tags:** `latest` · short commit SHA (7 hex)  
**Build cache:** registry cache at `…/backend:buildcache`  
**Security:** SBOM + provenance in the build, Trivy scan (CRITICAL/HIGH, `ignore-unfixed`) uploaded
as SARIF to code scanning, plus `actions/attest-build-provenance` pushed to the registry  
**Artifacts (on failure, 3 days):** `test-reports` (JUnit + JaCoCo)

---

### `frontend.yml` — Frontend CI

Runs on push and pull requests to `master` or `main` that touch `frontend/**` (push additionally
watches the workflow file). Executes Vitest unit tests and Playwright E2E tests against the `:4299`
e2e serve target on Node 22. The Playwright report is uploaded with `if: always()`, so it exists for
green runs too.

**Jobs:** `test` (Vitest unit + Playwright E2E, chromium only)  
**Artifacts (always, 30 days):** `playwright-report`

---

### `monorepo.yml` — Monorepo Full Build

Runs on every push to `master` or `main`, with no path filter. Builds the full production monorepo —
Angular frontend embedded into the Spring Boot JAR via `./gradlew build` — runs backend and frontend
tests, then publishes the combined image. Note that this job pushes to the *same* image repository
and the same `latest` / short-SHA tags as `backend.yml`; on a trunk push that touches `backend/**`,
both workflows run and the later one wins.

**Jobs:** `backend` (full build + tests, `combined-jar` artifact, 7 days) · `frontend` (Vitest only) ·
`docker-combined` (`needs: [backend, frontend]`)  
**Image:** `ghcr.io/codeministry/straightmail/backend` — `linux/amd64`, `linux/arm64`  
**Tags:** `latest` · short commit SHA (7 hex)  
**Security:** identical to `backend.yml` — SBOM, provenance, Trivy → SARIF, attestation

---

### `release.yml` — Release

Triggered on push to `master` / `main` and on semantic version tags (`v*.*.*`). Builds the
production `bootJar` with `SKIP_FRONTEND_BUILD=true` (the release image carries the backend only)
and publishes a multi-arch image with full supply-chain metadata. Only one release flow runs per
ref at a time, and a tagged release is never cancelled mid-flight.

**Job:** `build-and-push`  
**Image:** `ghcr.io/codeministry/straightmail` — `linux/amd64`, `linux/arm64`  
**Tags:** `latest` (default branch only) · `{{version}}` · `{{major}}.{{minor}}` · `{{major}}` from a
`v*.*.*` tag · `<branch>-<sha>` for branch builds — e.g. tag `v0.5.0` yields `0.5.0`, `0.5`, `0`  
**Build cache:** GitHub Actions cache (`type=gha`)  
**Security:** SBOM + provenance in the build, Trivy scan → SARIF, `actions/attest-build-provenance`
pushed to the registry  
**Concurrency:** `release-${{ github.ref }}`, `cancel-in-progress: false`

---

### `codeql.yml` — CodeQL

Static analysis for both halves of the monorepo, on push and pull requests to `master` / `main` and
weekly (Mondays 03:00 UTC) so newly published queries reach existing code without waiting for a
push. The backend renders untrusted FreeMarker templates and resolves caller-supplied template
paths — injection and path traversal are exactly what the `java-kotlin` queries cover.

**Job:** `analyze` (matrix, `fail-fast: false`)  
**Languages:** `java-kotlin` (`build-mode: manual`, compiled via `./gradlew compileJava`) ·
`javascript-typescript` (`build-mode: none`)  
**Query suite:** `security-extended`

---

### `cleanup.yml` — Cleanup Old Packages

GHCR retention for this repository's packages, scheduled Sundays 02:00 UTC and runnable manually
via `workflow_dispatch` (input `dry-run`, default `true`). It uses `dataaxiom/ghcr-cleanup-action`
rather than a blanket "delete untagged" rule: buildx attaches provenance, so a pushed artifact is an
OCI index plus untagged child manifests, and deleting all untagged versions would break `docker pull`
on a tag that still resolves.

A shell guard step composes the action inputs and enforces two invariants: `keep-n-tagged` is never
`0` or non-numeric (the action reads `0` as "delete every tagged image"), and `latest` plus
`buildcache` are always excluded — `buildcache` is the registry cache `backend.yml` and
`monorepo.yml` write to, so dropping it turns every subsequent build cold.

**Job:** `images`  
**Packages:** `straightmail`, `straightmail/backend`  
**Keeps:** 15 newest tagged images; excludes `latest`, `buildcache`, `v*`, `[0-9]*.[0-9]*`  
**Deletes:** untagged, ghost, partial and orphaned images (orphaned covers the `sha256-<digest>`
attestation referrer tags once their parent is gone)

> **Rollout gate:** the scheduled run is still forced into dry-run mode
> (`github.event_name == 'schedule' || inputs.dry-run`). Remove that clause — leaving
> `inputs.dry-run || false` — only after a manual dry-run has been reviewed for this repository.

---

## Required Permissions & Secrets

No repository secrets need to be configured manually.

| Requirement | Used by | Details |
|---|---|---|
| `GITHUB_TOKEN` | all `docker` / `release` / `cleanup` jobs | Auto-provided by GitHub — pushes images to `ghcr.io` and deletes package versions |
| `packages: write` | `backend.yml`, `monorepo.yml`, `release.yml`, `cleanup.yml` | Declared in each workflow's `permissions:` block |
| `security-events: write` | `backend.yml`, `monorepo.yml`, `release.yml`, `codeql.yml` | Uploading Trivy and CodeQL SARIF to code scanning |
| `id-token: write` | `backend.yml`, `monorepo.yml`, `release.yml` | OIDC tokens for SBOM and provenance attestations |
| `attestations: write` | `backend.yml`, `monorepo.yml`, `release.yml` | Required for `actions/attest-build-provenance` |

`feature.yml`, `backend.yml`, `frontend.yml`, `monorepo.yml` and `codeql.yml` set a read-only
`contents: read` default at the top level and widen permissions only on the jobs that need it.
`release.yml` and `cleanup.yml` declare their wider scopes at the top level because they each have a
single job.

## Notes for maintainers

- **Local Compose stacks do not pull these images.** The four stacks under `docker/` build from
  `../backend` with `build.context`, so a published image is never required for local development.
- **Image repositories differ by purpose.** `release.yml` publishes to
  `ghcr.io/codeministry/straightmail`; `backend.yml` and `monorepo.yml` publish to the nested
  `…/straightmail/backend`. Deployments that track 7-hex commit tags consume the latter.
- **Java 25 / Node 22** are the pinned toolchain versions across every workflow.
