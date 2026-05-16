#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGETS=(
  "$ROOT_DIR/k8s/overlays/staging"
  "$ROOT_DIR/k8s/overlays/production"
)

required_keys=(
  DATABASE_USERNAME
  DATABASE_PASSWORD
  JWT_SECRET
  RABBITMQ_USERNAME
  RABBITMQ_PASSWORD
)

for dir in "${TARGETS[@]}"; do
  example="$dir/secrets.env.example"
  target="$dir/secrets.env"

  if [[ ! -f "$example" ]]; then
    echo "Missing template: $example" >&2
    exit 1
  fi

  if [[ ! -f "$target" ]]; then
    cp "$example" "$target"
    echo "Created $target from template"
  else
    echo "Exists: $target"
  fi

  for key in "${required_keys[@]}"; do
    if ! grep -q "^${key}=" "$target"; then
      echo "Missing key '$key' in $target" >&2
      exit 1
    fi
  done
done

echo "Overlay bootstrap complete. Update secrets.env values before non-local deployment."
