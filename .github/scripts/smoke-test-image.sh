#!/usr/bin/env bash
# Smoke test for a straightmail container image.
#
# Usage: smoke-test-image.sh <image> <variant>
#   variant  backend  the API only; the admin UI must not be served
#            aio      the API with the admin UI embedded
#
# Starts the image with the least configuration that boots without external services:
# AUTH_MODE=none, a freshly generated ENCRYPTION_KEY and the mail health indicator disabled,
# since there is no SMTP server to reach. Waits for /actuator/health on the management port,
# checks /api/v1/info, then checks whether the admin UI is served as the variant requires.
#
# Environment:
#   SMOKE_TIMEOUT  seconds to wait for the health endpoint (default 120)
set -euo pipefail

usage() {
  echo "usage: $0 <image> <backend|aio>" >&2
  exit 2
}

[ "$#" -eq 2 ] || usage
IMAGE=$1
VARIANT=$2
case "$VARIANT" in backend | aio) ;; *) usage ;; esac
TIMEOUT=${SMOKE_TIMEOUT:-120}

# Angular's root element; index.html carries it, an API response never does.
APP_SHELL_MARKER='<app-root'

CONTAINER="straightmail-smoke-$VARIANT-$$"
WORKDIR=$(mktemp -d)

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  rm -rf -- "$WORKDIR"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  echo "----- container logs ($CONTAINER) -----" >&2
  docker logs "$CONTAINER" >&2 2>&1 || true
  echo "----- end of container logs -----" >&2
  exit 1
}

pass() {
  echo "ok:   $*"
}

# Writes the body to $WORKDIR/body and prints the HTTP status (000 when the request failed).
fetch() {
  local url=$1
  shift
  curl -sS -o "$WORKDIR/body" -w '%{http_code}' --max-time 10 "$@" "$url" 2>/dev/null || true
}

echo "Smoke test: $IMAGE as variant '$VARIANT'"

ENCRYPTION_KEY=$(head -c 32 /dev/urandom | base64)
docker run -d --name "$CONTAINER" \
  -p 127.0.0.1::50003 -p 127.0.0.1::50004 \
  -e AUTH_MODE=none \
  -e ENCRYPTION_KEY="$ENCRYPTION_KEY" \
  -e MANAGEMENT_HEALTH_MAIL_ENABLED=false \
  "$IMAGE" >/dev/null || fail "container did not start"

API=$(docker port "$CONTAINER" 50003/tcp | head -n 1)
MANAGEMENT=$(docker port "$CONTAINER" 50004/tcp | head -n 1)
[ -n "$API" ] && [ -n "$MANAGEMENT" ] || fail "ports 50003 and 50004 are not published"

deadline=$((SECONDS + TIMEOUT))
until [ "$(fetch "http://$MANAGEMENT/actuator/health")" = 200 ] && command grep -q '"status":"UP"' "$WORKDIR/body"; do
  if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != true ]; then
    fail "container exited before it became healthy"
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    fail "/actuator/health did not report UP within ${TIMEOUT}s"
  fi
  sleep 2
done
pass "/actuator/health reports UP"

status=$(fetch "http://$API/api/v1/info" -H 'Accept: application/json')
[ "$status" = 200 ] || fail "/api/v1/info answered $status, expected 200"
command grep -q '"application":"straightmail"' "$WORKDIR/body" || fail "/api/v1/info does not name the application: $(cat "$WORKDIR/body")"
command grep -q '"authMode":"none"' "$WORKDIR/body" || fail "/api/v1/info does not report authMode none: $(cat "$WORKDIR/body")"
pass "/api/v1/info answers $(cat "$WORKDIR/body")"

for path in / /templates; do
  status=$(fetch "http://$API$path" -H 'Accept: text/html')
  serves_shell=false
  if [ "$status" = 200 ] && command grep -q "$APP_SHELL_MARKER" "$WORKDIR/body"; then
    serves_shell=true
  fi
  case "$VARIANT" in
    aio)
      [ "$serves_shell" = true ] || fail "$path answered $status without the admin UI app shell"
      pass "$path serves the admin UI app shell"
      ;;
    backend)
      [ "$serves_shell" = false ] || fail "$path serves the admin UI app shell, the backend image must not embed it"
      pass "$path does not serve the admin UI (status $status)"
      ;;
  esac
done

echo "PASS: $IMAGE behaves as variant '$VARIANT'"
