# CI / CD Workflows

straightmail uses eight GitHub Actions workflows to cover the development, security and release
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
| Images | [`images.yml`](images.yml) | push to `master` / `main` (not docs-only), tag `v*.*.*`, PR touching the images | Test gate, smoke test, multi-arch images with SBOM & provenance | `ghcr.io/encircle360-oss/straightmail/backend` (also as `ghcr.io/encircle360-oss/straightmail`), `ghcr.io/encircle360-oss/straightmail/aio` |
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
and as a reusable workflow from `ci.yml` and from `images.yml` (the test gate before publishing). Builds the JAR with Gradle (`SKIP_FRONTEND_BUILD=true`)
and runs the test suite in a separate job. It publishes nothing.

**Jobs:** `build` · `test`  
**Artifacts (on failure, 3 days):** `test-reports` (JUnit + JaCoCo)

---

### `frontend.yml` — Frontend CI

Runs on push and pull requests to `master` or `main` that touch `frontend/**` (push additionally
watches the workflow file), and as a reusable workflow from `ci.yml` and from `images.yml`. Executes Vitest unit tests
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

### `images.yml` — Images

Builds, smoke-tests and publishes the container images. Runs on every push to `master` or `main`,
except pushes that only change Markdown, `docs/**`, `LICENSE` or issue templates, on semantic
version tags (`v*.*.*`; GitHub does not evaluate path filters for tag pushes, so every version tag
publishes), and on pull requests to `master` / `main` that touch `backend/**`, `frontend/**`, the
workflow or the smoke-test script.

On branch and tag pushes, `backend.yml` and `frontend.yml` run first as reusable workflows; the
images are built only when both succeed. Pull requests skip that gate (`ci.yml` runs the same
suites) and build `linux/amd64` only: they run the smoke test and push nothing — no registry login,
no scan upload, no attestation — so pull requests from forks, whose token is read-only, work the
same way.

The `image` job is a matrix over the two variants (`fail-fast: false`), each in its own runner:

| Variant | Image | Also pushed as | JAR |
|---|---|---|---|
| `backend` | `ghcr.io/encircle360-oss/straightmail/backend` | `ghcr.io/encircle360-oss/straightmail` (same digest) | `bootJar` with `SKIP_FRONTEND_BUILD=true` — the API only |
| `aio` | `ghcr.io/encircle360-oss/straightmail/aio` | — | `bootJar` with the Angular admin UI embedded (Node 26) |

Per variant: build the JAR, build the `linux/amd64` image and load it into the runner's Docker,
run [`../scripts/smoke-test-image.sh`](../scripts/smoke-test-image.sh) against it, then — on pushes
only — build and push the `linux/amd64` + `linux/arm64` image from the same cache, scan the pushed
digest with Trivy and attest it under every name it was pushed as. Both variants use
`backend/Dockerfile` with `backend/` as the build context.

**Concurrency:** one run per ref. A publishing run is never cancelled, so a fast follow-up push
cannot leave an older image behind `edge`; a pull request run is cancelled by the next push to the
same pull request. A tag is a ref of its own, so its build runs alongside the branch build of the
same commit.  
**Jobs:** `backend` · `frontend` (the test gate, reusable workflows) · `image` (matrix `backend`,
`aio`)  
**Tags:** from a `v*.*.*` tag `{{version}}` · `{{major}}.{{minor}}` · `{{major}}` · `latest`; from
the default branch `edge` · short commit SHA (7 hex) — the same for every image, see
[Image tags](#image-tags)  
**Build cache:** GitHub Actions cache (`type=gha`), one scope per variant (`image-backend`,
`image-aio`); pull requests read it but do not write it  
**Smoke test:** starts the image with `AUTH_MODE=none`, a generated `ENCRYPTION_KEY` and the mail
health indicator disabled, waits for `/actuator/health` on `:50004`, checks `/api/v1/info`, then
requires the admin UI app shell at `/` and `/templates` for `aio` and its absence for `backend`.
Run it locally with `.github/scripts/smoke-test-image.sh <image> <backend|aio>`  
**Security:** SBOM + provenance in the build, Trivy scan of the pushed digest (CRITICAL/HIGH,
`ignore-unfixed`) uploaded as SARIF to code scanning (category `container-image-<variant>`), plus
`actions/attest-build-provenance` pushed to the registry with an artifact storage record — for the
`backend` variant once per name

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
`0` or non-numeric (the action reads `0` as "delete every tagged image"), and `latest`, `edge` and
`buildcache` are always excluded — `buildcache` is the registry cache `monorepo.yml` writes to, so
dropping it turns every subsequent build cold.

Tag exclusion runs with `use-regex: true`, as one anchored expression. The action's default
wildcard syntax knows only `?`, `*` and `**` and has no character classes, so a pattern like
`[0-9]*.[0-9]*` would match no version tag.

**Job:** `images`  
**Packages:** `straightmail`, `straightmail/monorepo`  
**Keeps:** 15 newest tagged images per package  
**Never deletes:** `latest`, `edge`, `buildcache`, the released tags `0.4.0`, `0.4.1`, `0.4`, `0`, and any
`x`, `x.y`, `x.y.z` tag (optionally `v`-prefixed; a bare major has at most three digits, so a
digits-only 7-character commit SHA tag is not exempt)  
**Deletes:** untagged, ghost, partial and orphaned images (orphaned covers the `sha256-<digest>`
attestation referrer tags once their parent is gone)

> **Dry-run gate:** the scheduled run is forced into dry-run mode
> (`github.event_name == 'schedule' || inputs.dry-run`). Remove that clause — leaving
> `inputs.dry-run || false` — only after a manual dry-run has been reviewed for this repository.

---

## Image tags

`images.yml` tags every image by the same rules (`docker/metadata-action`):

| Event | Tags of `…/straightmail/backend`, `…/straightmail` and `…/straightmail/aio` |
|---|---|
| pull request | nothing is pushed |
| push to `master` (commit `abc1234…`) | `edge`, `abc1234` |
| tag `v0.6.0` | `0.6.0`, `0.6`, `0`, `latest` |
| tag `v1.0.0-rc.1` | `1.0.0-rc.1` |

`latest` therefore always points at the most recently tagged release and never at an unreleased
build; `edge` follows the default branch. A pre-release publishes only its own version: the action reduces the
`{{major}}.{{minor}}` and `{{major}}` patterns of a pre-release to `{{version}}`, and its default
flavor `latest=auto` adds `latest` only for a release. A push to `main` while `master` is the
default branch publishes the commit tag only.

## Required Permissions & Secrets

No repository secrets need to be configured manually.

| Requirement | Used by | Details |
|---|---|---|
| `GITHUB_TOKEN` | `images.yml`, `cleanup.yml` | Auto-provided by GitHub — pushes images to `ghcr.io` and deletes package versions |
| `packages: write` | `images.yml` (`image` job), `cleanup.yml` | Declared in the job's or workflow's `permissions:` block |
| `security-events: write` | `images.yml` (`image` job), `codeql.yml` | Uploading Trivy and CodeQL SARIF to code scanning |
| `id-token: write` | `images.yml` (`image` job) | OIDC tokens for SBOM and provenance attestations |
| `attestations: write` | `images.yml` (`image` job) | Required for `actions/attest-build-provenance` |
| `artifact-metadata: write` | `images.yml` (`image` job) | Lets `actions/attest-build-provenance` create the artifact storage record for the pushed image |
| `contents: write` | `dependency-submission.yml` | Submitting the dependency graph |
| `pull-requests: read` | `ci.yml` | `changes` job reads the pull request's changed files |

`ci.yml`, `feature.yml`, `backend.yml`, `frontend.yml`, `images.yml` and `codeql.yml` set a
read-only `contents: read` default at the top level and widen permissions only on the jobs that need
it. `cleanup.yml` and `dependency-submission.yml` declare their wider scopes at the top level because
they each have a single job. On pull requests from forks GitHub issues a read-only token whatever a
workflow requests; `images.yml` pushes nothing on pull requests, so it needs no more there.

For `cleanup.yml`, each package must also grant this repository the **Admin** role under
**Package Settings → Manage Actions access**.

## Notes for maintainers

- **Local Compose stacks do not pull these images.** The four stacks under `docker/` build from
  `../backend` with `build.context`, so a published image is never required for local development.
- **Image repositories differ by content.** `…/straightmail/backend` carries the API only and is
  pushed with the identical digest as `ghcr.io/encircle360-oss/straightmail`, the name used up to
  0.5.0, so existing deployments and `FROM` lines keep working. `…/straightmail/aio` carries the API
  with the admin UI embedded. `…/straightmail/monorepo` is no longer published and stays at its
  last build, the 0.5.0 commit `0397766`.
- **Each variant has its own code scanning category** (`container-image-backend`,
  `container-image-aio`). Code scanning replaces earlier results of the same tool and category
  for a commit, so a shared category would keep only the image whose scan uploads last.
- **Several workflows can run for the same push or pull request** (for example `ci.yml` together
  with `backend.yml`/`frontend.yml` and `images.yml`). On a push to `master`, `backend.yml` and
  `frontend.yml` run once on their own (when their paths changed) and once more inside
  `images.yml` as its test gate.
