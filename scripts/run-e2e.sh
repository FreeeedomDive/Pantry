#!/usr/bin/env bash
set -euo pipefail

root_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
compose=(docker compose --project-name pantry-e2e --file "$root_directory/compose.e2e.yaml")
runtime_log=""

cleanup() {
  local status=$?

  if [ "$status" -ne 0 ]; then
    mkdir -p "$root_directory/frontend/test-results"
    "${compose[@]}" logs --no-color > "$root_directory/frontend/test-results/compose.log" || true
    if [ -n "$runtime_log" ] && [ -f "$runtime_log" ]; then
      cp "$runtime_log" "$root_directory/frontend/test-results/runtime.log"
    fi
  fi

  "${compose[@]}" down --volumes --remove-orphans || true
  if [ -n "$runtime_log" ]; then
    rm -f "$runtime_log"
  fi
  return "$status"
}

trap cleanup EXIT
trap 'exit 130' INT TERM

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run E2E tests." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is installed but the daemon is unavailable." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required to run E2E tests." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required to run E2E tests." >&2
  exit 1
fi

"${compose[@]}" down --volumes --remove-orphans || true
"${compose[@]}" up --wait --remove-orphans
runtime_log="$(mktemp "${TMPDIR:-/tmp}/pantry-e2e.XXXXXX")"
set +e
npm --prefix "$root_directory/frontend" run e2e 2>&1 | tee "$runtime_log"
playwright_status=${PIPESTATUS[0]}
set -e

if [ "$playwright_status" -ne 0 ]; then
  exit "$playwright_status"
fi
