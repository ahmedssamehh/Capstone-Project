# Phase 3 Evidence Index

This index maps each SWAPD 452 Phase 3 rubric requirement to repository evidence and verification steps.

## 1. Docker + Compose End-to-End (5)

Requirement: Dockerfile + docker-compose with app, PostgreSQL, and RabbitMQ.

- Evidence files:
  - `Dockerfile`
  - `docker-compose.yml`
  - `docker-compose.prod.yml`
- Key proof points:
  - multi-stage build (`deps`, `builder`, `extractor`, `runtime`)
  - non-root runtime user
  - health checks and dependency gating
  - externalized environment variables and required secrets
- Verification:
  - `docker compose -f docker-compose.yml config`
  - `docker compose up -d`
  - `docker compose ps`
  - `curl http://localhost:8080/actuator/health`

## 2. Kubernetes Manifests + Probes/Config (5)

Requirement: deployment, service, configmap, secret pattern, readiness/liveness probes.

- Evidence files:
  - `k8s/base/deployment.yaml`
  - `k8s/base/service.yaml`
  - `k8s/base/configmap.yaml`
  - `k8s/base/secret.example.yaml`
  - `k8s/base/poddisruptionbudget.yaml`
  - `k8s/overlays/staging/kustomization.yaml`
  - `k8s/overlays/production/kustomization.yaml`
  - `k8s/overlays/production/networkpolicy.yaml`
- Key proof points:
  - startup/liveness/readiness probes point to actuator endpoints
  - secrets consumed via `secretKeyRef`
  - production overlay includes network policy and higher replica count
- Verification:
  - `./scripts/k8s-overlay-bootstrap.sh` or `.ps1`
  - `./scripts/k8s-overlay-validate.sh` or `.ps1`
  - `kubectl apply -k k8s/overlays/staging`
  - `kubectl -n workhub rollout status deployment/workhub-api`

## 3. Terraform IaC Completeness + Reproducibility (6)

Requirement: terraform folder with required files and reproducible infrastructure.

- Evidence files:
  - `terraform/main.tf`
  - `terraform/variables.tf`
  - `terraform/outputs.tf`
  - `terraform/terraform.tfvars.example`
  - `terraform/README.md`
  - `terraform/TERRAFORM-EVIDENCE.md`
  - `terraform/modules/workhub-platform/*`
  - `terraform/environments/dev/*`
  - `terraform/environments/staging/*`
  - `terraform/environments/production/*`
- Key proof points:
  - module-based IaC with sensitive variable handling and validation
  - environment roots for reproducible promotion
  - outputs for verification and operations
- Verification:
  - `terraform -chdir=terraform fmt -check -recursive`
  - `terraform -chdir=terraform init -backend=false`
  - `terraform -chdir=terraform validate`
  - `terraform -chdir=terraform/environments/dev init -backend=false`
  - `terraform -chdir=terraform/environments/dev validate`

## 4. CI Pipeline Build + Tests + Image Build (4)

Requirement: GitHub Actions runs tests and builds image.

- Evidence files:
  - `.github/workflows/ci.yml`
  - `.github/scripts/verify-no-skipped-tests.py`
  - `.github/scripts/collect-integration-diagnostics.sh`
- Key proof points:
  - unit tests and integration tests are separate jobs
  - integration suite enforces minimum test count and zero skipped tests
  - Docker image is built in CI with Buildx
  - diagnostics artifact is uploaded on integration failures
- Verification:
  - run workflow from GitHub Actions UI
  - check job outputs and uploaded artifacts (`surefire-*`, `integration-diagnostics-*`, `ci-build-metadata-*`)

## 5. Terraform CI Validation (Hardening)

Requirement: reproducibility and defensible IaC checks.

- Evidence files:
  - `.github/workflows/terraform-validate.yml`
- Key proof points:
  - fmt check
  - validate root stack
  - validate environment stacks
- Verification:
  - trigger Terraform Validate workflow
  - confirm all matrix targets pass

## 6. Documentation Deliverables

Requirement: mandatory Phase 3 deployment documentation.

- Evidence files:
  - `DEPLOYMENT.md`
  - `CLOUD-NATIVE-ARCHITECTURE.md`
  - `DOCKER.md`
  - `.github/CI.md`
  - `terraform/README.md`
- Verification:
  - follow `DEPLOYMENT.md` commands on a clean workstation
  - run `scripts/phase3-preflight.sh` or `.ps1`

## 7. Observability, Probe Safety, and Security Hardening

- Evidence files:
  - `OBSERVABILITY.md`
  - `k8s/base/deployment.yaml`
  - `docker-compose.yml`
  - `docker-compose.prod.yml`
- Key proof points:
  - actuator health/readiness/liveness available
  - non-root runtime and hardening controls
  - rollout and disruption controls
  - production network restrictions and secret externalization
- Verification:
  - `curl /actuator/health*`
  - `kubectl describe deploy workhub-api`
  - `kubectl get pdb -n workhub`
  - `kubectl get networkpolicy -n workhub`

## 8. Grading and Defense Checklist

Before submission:

- run `scripts/phase3-preflight.ps1` or `scripts/phase3-preflight.sh`
- ensure `DEPLOYMENT.md` commands are executable in demo environment
- capture screenshots/log excerpts for:
  - compose stack healthy
  - kustomize validation pass
  - terraform validate pass
  - CI jobs pass (unit, integration, package, terraform validate)
- keep this index available during oral defense for rapid evidence lookup
