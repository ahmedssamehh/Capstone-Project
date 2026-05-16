#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

required_files=(
  "$ROOT_DIR/k8s/overlays/staging/secrets.env"
  "$ROOT_DIR/k8s/overlays/production/secrets.env"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing $file. Run ./scripts/k8s-overlay-bootstrap.sh first." >&2
    exit 1
  fi
done

pushd "$ROOT_DIR" >/dev/null
kubectl kustomize k8s/base >/dev/null
echo "KUSTOMIZE_BUILD_BASE_OK"
kubectl kustomize k8s/overlays/local >/dev/null
echo "KUSTOMIZE_BUILD_LOCAL_OK"
kubectl kustomize k8s/overlays/staging >/dev/null
echo "KUSTOMIZE_BUILD_STAGING_OK"
kubectl kustomize k8s/overlays/production >/dev/null
echo "KUSTOMIZE_BUILD_PRODUCTION_OK"
popd >/dev/null
