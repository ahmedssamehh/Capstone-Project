# WorkHub Phase 3 Deployment Guide

This document is the authoritative Phase 3 deployment runbook for:

- local Docker Compose deployment
- Kubernetes deployment with Kustomize overlays
- Terraform Infrastructure as Code (Kubernetes track)
- CI/CD validation flow
- operational verification and production readiness checks

## 1. Prerequisites

### Required tools

- Java 17+
- Maven 3.9+
- Docker Engine + Docker Compose v2
- kubectl v1.25+
- Terraform v1.5+
- Python 3 (for CI helper script parity)

### Validate tools

```bash
# Linux/macOS
java -version
mvn -version
docker version
docker compose version
kubectl version --client
terraform version
python3 --version
```

```powershell
# Windows PowerShell
java -version
mvn -version
docker version
docker compose version
kubectl version --client
terraform version
python --version
```

## 2. Local Docker Compose Deployment

### 2.1 Environment setup

1. Copy local environment template:

```bash
cp .env.example .env
```

2. Set required values in `.env`:

- `POSTGRES_PASSWORD`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `JWT_SECRET` (min 32 chars)

### 2.2 Startup

```bash
docker compose up --build -d
```

### 2.3 Verification

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/actuator/health/liveness
```

Expected:

- `postgres`, `rabbitmq`, and `app` are `Up` and `healthy`
- Actuator endpoints return HTTP 200 with `"status":"UP"`

Platform scripts:

```powershell
./scripts/docker-stack-verify.ps1
```

```bash
./scripts/docker-stack-verify.sh
```

### 2.4 Troubleshooting

- Compose config errors:

```bash
docker compose config
```

- App logs:

```bash
docker compose logs app --tail 200
```

- RabbitMQ logs:

```bash
docker compose logs rabbitmq --tail 200
```

- Postgres logs:

```bash
docker compose logs postgres --tail 200
```

- Full reset:

```bash
docker compose down -v
docker compose up --build -d
```

## 3. Kubernetes Deployment (Kustomize)

## 3.1 Namespace and base resources

Base manifests include:

- Namespace
- ServiceAccount
- ConfigMap
- Deployment
- Service
- PodDisruptionBudget

Render base:

```bash
kubectl kustomize k8s/base
```

Apply base:

```bash
kubectl apply -k k8s/base
```

## 3.2 Overlay bootstrap (staging/production)

Staging and production overlays require `secrets.env` generated from example templates.

Bootstrap command (creates missing files from `.example`):

```powershell
./scripts/k8s-overlay-bootstrap.ps1
```

```bash
./scripts/k8s-overlay-bootstrap.sh
```

Then set real values in:

- `k8s/overlays/staging/secrets.env`
- `k8s/overlays/production/secrets.env`

Required keys:

- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`

## 3.3 Render and validate overlays

```powershell
./scripts/k8s-overlay-validate.ps1
```

```bash
./scripts/k8s-overlay-validate.sh
```

Manual equivalent:

```bash
kubectl kustomize k8s/overlays/local
kubectl kustomize k8s/overlays/staging
kubectl kustomize k8s/overlays/production
```

## 3.4 Apply overlays

```bash
kubectl apply -k k8s/overlays/staging
# or
kubectl apply -k k8s/overlays/production
```

## 3.5 Rollout and health validation

```bash
kubectl -n workhub rollout status deployment/workhub-api --timeout=180s
kubectl -n workhub get pods,svc
kubectl -n workhub get events --sort-by=.metadata.creationTimestamp | tail -20
```

Probe verification from cluster:

```bash
kubectl -n workhub port-forward svc/workhub-api 8080:80
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

## 4. Terraform Deployment (Kubernetes Track)

Terraform lives under `terraform/` and provisions Kubernetes resources for WorkHub.

### 4.1 Variable setup

Option A (recommended in CI/CD): set sensitive values using `TF_VAR_*` environment variables.

Option B (local only): copy `secrets.tfvars.example` to `secrets.tfvars` (never commit).

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
cp secrets.tfvars.example secrets.tfvars
```

### 4.2 Init, plan, apply

```bash
cd terraform
terraform init
terraform fmt -check -recursive
terraform validate
terraform plan -var-file=terraform.tfvars -var-file=secrets.tfvars
terraform apply -var-file=terraform.tfvars -var-file=secrets.tfvars
```

Environment-root flow (recommended for separation):

```bash
cd terraform/environments/dev
terraform init
terraform validate
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

### 4.3 Destroy (rollback of infra)

```bash
cd terraform
terraform destroy -var-file=terraform.tfvars -var-file=secrets.tfvars
```

### 4.4 Rollback explanation

- Application rollback: deploy previous image tag via Kustomize patch or Terraform `image_tag` variable, then re-apply.
- Infra rollback: use Terraform state + version-controlled `.tf` files; apply previous commit to converge back.
- Emergency rollback: switch to prior known-good image and keep infra unchanged.

### 4.5 Example outputs (sanitized)

Typical outputs after apply:

- namespace: `workhub`
- deployment_name: `workhub-api`
- service_name: `workhub-api`
- service_cluster_dns: `workhub-api.workhub.svc.cluster.local`
- config_map_name: `workhub-api-config`

Detailed sample evidence is documented in `terraform/TERRAFORM-EVIDENCE.md`.

## 5. CI/CD Pipeline Explanation

Workflow file: `.github/workflows/ci.yml`

Stages:

1. `unit-tests`
- compiles and runs unit tests only
- enforces no skipped tests

2. `integration-tests`
- verifies Docker daemon
- pre-pulls RabbitMQ Testcontainers image
- runs integration suites including `Phase2EnterpriseIntegrationTest`
- enforces minimum integration test count and zero skipped tests

3. `package-and-docker`
- builds executable JAR
- builds Docker image via Buildx
- publishes checksum and build metadata artifacts

Terraform CI validation:

- `.github/workflows/terraform-validate.yml`
- runs `terraform fmt -check`, `init -backend=false`, and `validate` for root and environment stacks

## 6. Operational Verification

### 6.1 Actuator endpoints

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/actuator/health/liveness
```

### 6.2 RabbitMQ verification

```bash
# Management UI
# http://localhost:15672

# Queue and broker checks
docker compose exec rabbitmq rabbitmq-diagnostics -q ping
docker compose exec rabbitmq rabbitmq-diagnostics -q check_port_listener 5672
```

### 6.3 PostgreSQL verification

```bash
docker compose exec postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

### 6.4 Log verification

```bash
docker compose logs app --tail 200
docker compose logs rabbitmq --tail 200
docker compose logs postgres --tail 200
```

Check for:

- app startup complete
- DB connectivity established
- RabbitMQ connectivity established
- no repetitive liveness failures

### 6.5 Probe verification in Kubernetes

```bash
kubectl -n workhub describe deploy workhub-api
kubectl -n workhub get pods
kubectl -n workhub describe pod <pod-name>
```

Confirm:

- `startupProbe` points to `/actuator/health/liveness`
- `livenessProbe` points to `/actuator/health/liveness`
- `readinessProbe` points to `/actuator/health/readiness`

## 7. Production Readiness Controls

Implemented controls:

- multi-stage Docker build with minimal runtime image
- non-root application container execution
- `no-new-privileges` in compose services
- Kubernetes security context hardening
- readiness/liveness/startup probes for safe orchestration
- rolling update strategy and PodDisruptionBudget
- production network policy (overlay)
- externalized secrets via env files and Kubernetes secret generation
- actuator + metrics endpoints and correlation logging

## 8. Preflight and One-Command Checks

Project preflight (tools + files + render checks):

```powershell
./scripts/phase3-preflight.ps1
```

```bash
./scripts/phase3-preflight.sh
```

These checks are designed for grading defensibility and reproducible operator onboarding.

## 9. Chosen Free Deployment Option (Phase 3 Requirement)

Selected option for this project: Local Kubernetes (kind/minikube) with Terraform Kubernetes provider.

Why this option was chosen:

- no paid cloud dependency for grading/demo
- deterministic local reproducibility for instructor validation
- direct alignment with Kubernetes track Terraform requirements

Suggested demo flow on a clean machine:

1. Create a local cluster (kind or minikube).
2. Run overlay bootstrap and validation scripts.
3. Run Terraform validation and apply.
4. Verify rollout, probes, and service health using commands in Sections 3 to 6.
