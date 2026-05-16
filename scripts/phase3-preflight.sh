#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_command() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
  echo "OK command: $cmd"
}

require_path() {
  local path="$1"
  if [[ ! -e "$path" ]]; then
    echo "Missing required path: $path" >&2
    exit 1
  fi
  echo "OK path: $path"
}

require_command docker
require_command kubectl
require_command mvn

if command -v terraform >/dev/null 2>&1; then
  TERRAFORM_AVAILABLE=true
  echo "OK command: terraform"
else
  TERRAFORM_AVAILABLE=false
  echo "WARN terraform not found; Terraform execution checks skipped."
fi

required_paths=(
  "$ROOT_DIR/Dockerfile"
  "$ROOT_DIR/docker-compose.yml"
  "$ROOT_DIR/DEPLOYMENT.md"
  "$ROOT_DIR/.github/workflows/ci.yml"
  "$ROOT_DIR/.github/workflows/terraform-validate.yml"
  "$ROOT_DIR/k8s/base/deployment.yaml"
  "$ROOT_DIR/k8s/base/service.yaml"
  "$ROOT_DIR/k8s/base/configmap.yaml"
  "$ROOT_DIR/k8s/base/poddisruptionbudget.yaml"
  "$ROOT_DIR/k8s/base/secret.example.yaml"
  "$ROOT_DIR/terraform/main.tf"
  "$ROOT_DIR/terraform/variables.tf"
  "$ROOT_DIR/terraform/outputs.tf"
  "$ROOT_DIR/terraform/terraform.tfvars.example"
  "$ROOT_DIR/terraform/README.md"
  "$ROOT_DIR/PHASE3-EVIDENCE-INDEX.md"
)

for path in "${required_paths[@]}"; do
  require_path "$path"
done

pushd "$ROOT_DIR" >/dev/null
docker compose -f docker-compose.yml config >/dev/null
echo "COMPOSE_CONFIG_OK"

"$ROOT_DIR/scripts/k8s-overlay-bootstrap.sh" >/dev/null
"$ROOT_DIR/scripts/k8s-overlay-validate.sh" >/dev/null

if [[ "$TERRAFORM_AVAILABLE" == "true" ]]; then
  terraform -chdir="$ROOT_DIR/terraform" fmt -check -recursive
  echo "TERRAFORM_FMT_OK"
fi
popd >/dev/null

echo "PHASE3_PREFLIGHT_OK"
