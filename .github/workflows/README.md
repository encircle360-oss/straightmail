# CI / CD Workflows

straightmail uses nine GitHub Actions workflows to cover the development, security and release
lifecycle — from feature branch validation through to image publishing with supply-chain
attestations and registry retention. Dependency updates come from Dependabot
([`../dependabot.yml`](../dependabot.yml)).

The repository's default branch is `master`. Workflows that watch a trunk accept both `master`
and `main`, so a rename does not silently disable CI.

## Overview

| Workflow | File | Trigger | Purpose | Publishes |
|---|---|---|---|---|
| ci | [`ci.yml`](ci.yml) | every pull request | Runs the backend and frontend suites the PR touches; ends in `CI gate` | — |
| Backend CI | [`backend.yml`](backend.yml) | push / PR to `master` or `main` (`backend/**`), called by `ci.yml` | Build + test the backend | — |
| Frontend CI | [`frontend.yml`](frontend.yml) | push / PR to `master` or `main` (`frontend/**`), called by `ci.yml` | Unit tests + Playwright E2E | — |
| Feature Branch CI | [`feature.yml`](feature.yml) | push to `feature/**`, `fix/**`, `chore/**` | Validate before a PR is opened | — |
| Monorepo Full Build | [`monorepo.yml`](monorepo.yml) | push to `master` or `main` (not docs-only) | Full build + combined image | `ghcr.io/encircle360-oss/straightmail/monorepo` |
| Release | [`release.yml`](release.yml) | push to `master` / `main` (not docs-only), tag `v*.*.*` | Backend image with SBOM & provenance | `ghcr.io/encircle360-oss/straightmail` |
| CodeQL | [`codeql.yml`](codeql.yml) | push / PR to `master` or `main`, weekly cron | Static analysis (Java + TypeScript) | — |
| Dependency submission | [`dependency-submission.yml`](dependency-submission.yml) | push to `master`, `workflow_dispatch` | Gradle dependency graph for Dependabot alerts | — |
| Cleanup Old Packages | [`cleanup.yml`](cleanup.yml) | weekly cron, `workflow_dispatch` | GHCR retention for the published images | — |

Toolchain across every workflow: **Java 25** (Temurin) and **Node 26**.

---

## Workflows

### `ci.yml` — Pull request CI

Runs on every pull request without a paths filter. A `changes` job (`dorny/paths-filter`) detects
whether `backend/**` or `frontend/**` changed, then calls `backend.yml` and/or `frontend.yml` as
reusable workflows. The final job, `CI gate`, always reports and is green only if every suite that
had to run succeeded (and also when none had to). It is the job to mark as the required status
check: a required check from `backend.yml` or `frontend.yml` would never report on a pull request
outside their paths and block it. A change to `ci.yml` itself runs both suites.

**Jobs:** `changes` · `backend` · `frontend` · `gate` (`CI gate`)  
**Concurrency:** one run per pull request, in-progress runs are cancelled

---

### `backend.yml` — Backend CI

Runs on push and pull requests to `master` or `main` that touch `backend/**` or the workflow file,
and as a reusable workflow from `ci.yml`. Builds the JAR with Gradle (`SKIP_FRONTEND_BUILD=true`)
and runs the test suite in a separate job. It publishes nothing.

**Jobs:** `build` · `test`  
**Artifacts (on failure, 3 days):** `test-reports` (JUnit + JaCoCo)

---

### `frontend.yml` — Frontend CI

Runs on push and pull requests to `master` or `main` that touch `frontend/**` (push additionally
watches the workflow file), and as a reusable workflow from `ci.yml`. Executes Vitest unit tests
and Playwright E2E tests (chromium) against the `:4299` e2e serve target. The Playwright report is
uploaded with `if: always()`, so it exists for green runs too.

**Jobs:** `test`  
**Artifacts (always, 30 days):** `playwright-report`

---

### `feature.yml` — Feature Branch CI

Runs on every push to `feature/**`, `fix/**`, or `chore/**`. Ensures that both halves build and
pass their tests before a pull request is opened. Frontend linting is best-effort (`npm run lint ||
echo …`), so a missing lint script never fails the branch.

**Jobs:** `backend` (Gradle build + test, `SKIP_FRONTEND_BUILD=true`) · `frontend` (lint + Vitest + Playwright)  
**Artifacts (on failure, 3 days):** `backend-test-reports` (JUnit + JaCoCo) · `playwright-report`

---

### `monorepo.yml` — Monorepo Full Build

Runs on every push to `master` or `main`, except pushes that only change Markdown, `docs/**`,
`LICENSE` or issue templates. Builds the full monorepo — Angular console embedded into the Spring
Boot JAR via `./gradlew build` — runs backend and frontend unit tests, then publishes the combined
image. Only one publish runs per ref at a time, so a fast follow-up push cannot leave an older
image behind `latest`.

**Jobs:** `backend` (full build + tests, `combined-jar` artifact, 7 days) · `frontend` (Vitest) ·
`docker-combined` (`needs: [backend, frontend]`)  
**Image:** `ghcr.io/encircle360-oss/straightmail/monorepo` — `linux/amd64`, `linux/arm64`  
**Tags:** `latest` · short commit SHA (7 hex)  
**Build cache:** registry cache at `…/straightmail/monorepo:buildcache`  
**Security:** SBOM + provenance in the build, Trivy scan of the pushed digest (CRITICAL/HIGH,
`ignore-unfixed`) uploaded as SARIF to code scanning (category `container-image`), plus
`actions/attest-build-provenance` pushed to the registry with an artifact storage record

---

### `release.yml` — Release

Runs on push to `master` / `main` (with the same docs-only exclusion as `monorepo.yml`) and on
semantic version tags (`v*.*.*`); GitHub does not evaluate path filters for tag pushes, so every
version tag releases. Builds the `bootJar` with `SKIP_FRONTEND_BUILD=true` — the release image
carries the backend only — and publishes a multi-arch image with supply-chain metadata. Only one
release flow runs per ref at a time, and a tagged release is never cancelled mid-flight.

**Job:** `build-and-push`  
**Image:** `ghcr.io/encircle360-oss/straightmail` — `linux/amd64`, `linux/arm64`  
**Tags:** `latest` (default branch only) · `{{version}}` · `{{major}}.{{minor}}` · `{{major}}` from a
`v*.*.*` tag · `<branch>-<sha>` for branch builds — e.g. tag `v0.5.0` yields `0.5.0`, `0.5`, `0`  
**Build cache:** GitHub Actions cache (`type=gha`)  
**Security:** SBOM + provenance in the build, Trivy scan of the pushed digest → SARIF (category
`container-image`), `actions/attest-build-provenance` pushed to the registry with an artifact
storage record

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

### `dependency-submission.yml` — Dependency submission

Submits the resolved Gradle dependency graph of `backend/` to GitHub on every push to `master`, so
Dependabot alerts also cover transitive dependencies that appear in no build file.

**Job:** `dependency-submission` (`gradle/actions/dependency-submission`)

---

### `cleanup.yml` — Cleanup Old Packages

GHCR retention for the published packages, scheduled Sundays 02:00 UTC and runnable manually via
`workflow_dispatch` (input `dry-run`, default `true`). It uses `dataaxiom/ghcr-cleanup-action`
rather than a blanket "delete untagged" rule: buildx attaches provenance, so a pushed artifact is an
OCI index plus untagged child manifests, and deleting all untagged versions would break `docker pull`
on a tag that still resolves.

A shell guard step composes the action inputs and enforces two invariants: `keep-n-tagged` is never
`0` or non-numeric (the action reads `0` as "delete every tagged image"), and `latest` plus
`buildcache` are always excluded — `buildcache` is the registry cache `monorepo.yml` writes to, so
dropping it turns every subsequent build cold.

Tag exclusion runs with `use-regex: true`, as one anchored expression. The action's default
wildcard syntax knows only `?`, `*` and `**` and has no character classes, so a pattern like
`[0-9]*.[0-9]*` would match no version tag.

**Job:** `images`  
**Packages:** `straightmail`, `straightmail/monorepo`  
**Keeps:** 15 newest tagged images per package  
**Never deletes:** `latest`, `buildcache`, the released tags `0.4.0`, `0.4.1`, `0.4`, `0`, and any
`x`, `x.y`, `x.y.z` tag (optionally `v`-prefixed; a bare major has at most three digits, so a
digits-only 7-character commit SHA tag is not exempt)  
**Deletes:** untagged, ghost, partial and orphaned images (orphaned covers the `sha256-<digest>`
attestation referrer tags once their parent is gone)

> **Dry-run gate:** the scheduled run is forced into dry-run mode
> (`github.event_name == 'schedule' || inputs.dry-run`). Remove that clause — leaving
> `inputs.dry-run || false` — only after a manual dry-run has been reviewed for this repository.

---

## Required Permissions & Secrets

No repository secrets need to be configured manually.

| Requirement | Used by | Details |
|---|---|---|
| `GITHUB_TOKEN` | `monorepo.yml`, `release.yml`, `cleanup.yml` | Auto-provided by GitHub — pushes images to `ghcr.io` and deletes package versions |
| `packages: write` | `monorepo.yml`, `release.yml`, `cleanup.yml` | Declared in each workflow's `permissions:` block |
| `security-events: write` | `monorepo.yml`, `release.yml`, `codeql.yml` | Uploading Trivy and CodeQL SARIF to code scanning |
| `id-token: write` | `monorepo.yml`, `release.yml` | OIDC tokens for SBOM and provenance attestations |
| `attestations: write` | `monorepo.yml`, `release.yml` | Required for `actions/attest-build-provenance` |
| `artifact-metadata: write` | `monorepo.yml`, `release.yml` | Lets `actions/attest-build-provenance` create the artifact storage record for the pushed image |
| `contents: write` | `dependency-submission.yml` | Submitting the dependency graph |
| `pull-requests: read` | `ci.yml` | `changes` job reads the pull request's changed files |

`ci.yml`, `feature.yml`, `backend.yml`, `frontend.yml`, `monorepo.yml` and `codeql.yml` set a
read-only `contents: read` default at the top level and widen permissions only on the jobs that need
it. `release.yml`, `cleanup.yml` and `dependency-submission.yml` declare their wider scopes at the
top level because they each have a single job.

For `cleanup.yml`, each package must also grant this repository the **Admin** role under
**Package Settings → Manage Actions access**.

## Notes for maintainers

- **Local Compose stacks do not pull these images.** The four stacks under `docker/` build from
  `../backend` with `build.context`, so a published image is never required for local development.
- **Image repositories differ by content.** `release.yml` publishes the backend-only image to
  `ghcr.io/encircle360-oss/straightmail` (version tags on release); `monorepo.yml` publishes the
  image with the embedded admin console to `ghcr.io/encircle360-oss/straightmail/monorepo`
  (7-hex commit tags). Nothing publishes `…/straightmail/backend`.
- **Several workflows can run for the same push or pull request** (for example `ci.yml` together
  with `backend.yml`/`frontend.yml`, or `monorepo.yml` together with `release.yml`).
