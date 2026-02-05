#!/usr/bin/env bash
# smoke.sh — Quick smoke test against the deployed app.
#
# Usage:
#   ./scripts/smoke.sh [base_url]
#   ./scripts/smoke.sh https://nicolasgrabner.com

set -euo pipefail

BASE="${1:-https://tasktracker.nicolasgrabner.com}"
PASS=0
FAIL=0

check() {
  local desc="$1"
  local url="$2"
  local expected_status="$3"

  actual=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "${url}" 2>/dev/null || echo "000")

  if [ "${actual}" = "${expected_status}" ]; then
    echo "  PASS  ${desc} (${actual})"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  ${desc} — expected ${expected_status}, got ${actual}"
    FAIL=$((FAIL + 1))
  fi
}

echo "Smoke testing ${BASE} ..."
echo ""

check "Health endpoint"       "${BASE}/actuator/health"   "200"
check "Swagger UI"            "${BASE}/swagger-ui/index.html" "200"
check "OpenAPI JSON"          "${BASE}/api-docs"          "200"
check "Tasks (no auth)"       "${BASE}/api/tasks"         "401"
check "Me (no auth)"          "${BASE}/api/me"            "401"
check "HTTP -> HTTPS redirect" "http://tasktracker.nicolasgrabner.com/" "301"

echo ""
echo "Results: ${PASS} passed, ${FAIL} failed"

if [ "${FAIL}" -gt 0 ]; then
  exit 1
fi
