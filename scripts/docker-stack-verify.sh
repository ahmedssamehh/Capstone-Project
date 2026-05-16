#!/usr/bin/env sh
# Quick smoke test after: docker compose up --build -d
set -eu

APP_PORT="${APP_PORT:-8080}"
MAX_WAIT="${MAX_WAIT:-180}"

echo "Waiting for app readiness (max ${MAX_WAIT}s)..."
i=0
while [ "$i" -lt "$MAX_WAIT" ]; do
  if curl -fsS "http://127.0.0.1:${APP_PORT}/actuator/health/readiness" >/dev/null 2>&1; then
    echo "OK: readiness probe passed"
    curl -fsS "http://127.0.0.1:${APP_PORT}/actuator/health" | head -c 500
    echo ""
    exit 0
  fi
  i=$((i + 5))
  sleep 5
done

echo "FAIL: app not ready within ${MAX_WAIT}s"
docker compose ps
docker compose logs app --tail 80
exit 1
