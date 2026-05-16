#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-target/ci-diagnostics}"
mkdir -p "$OUT_DIR"

{
  echo "# Docker Version"
  docker version
  echo
  echo "# Docker Info"
  docker info
} > "$OUT_DIR/docker.txt" 2>&1 || true

{
  echo "# Running containers"
  docker ps -a
} > "$OUT_DIR/docker-ps.txt" 2>&1 || true

if [[ -d target/surefire-reports ]]; then
  cp -R target/surefire-reports "$OUT_DIR/surefire-reports" || true
fi

if compgen -G "target/*.log" >/dev/null; then
  cp target/*.log "$OUT_DIR/" || true
fi

{
  echo "# Disk usage"
  df -h || true
  echo
  echo "# Memory"
  free -h || true
} > "$OUT_DIR/system.txt" 2>&1 || true

echo "Integration diagnostics written to $OUT_DIR"
