# WorkHub Cloud-Native Architecture Guide

## 1) Executive Summary

WorkHub is a Spring Boot multi-tenant SaaS backend that is implemented as a cloud-native application stack with:

- Containerized runtime and dependencies for local and pre-production parity
- Kubernetes manifests and overlays for environment-aware orchestration
- Terraform modules for repeatable Infrastructure as Code
- GitHub Actions CI for automated quality gates and build integrity

This guide documents the architecture as implemented in this repository and explains why these patterns matter for enterprise delivery.

## 2) Deployment Architecture

### 2.1 Runtime Topology

The deployed system is split into a stateless API tier and stateful infrastructure dependencies:

- API: Spring Boot application, horizontally scalable, containerized
- Data: PostgreSQL
- Messaging: RabbitMQ for asynchronous processing and decoupled workflows

The API is the main workload managed by Kubernetes and Terraform in this repo. PostgreSQL and RabbitMQ are consumed as infrastructure dependencies (in-cluster or managed externally depending on environment).

### 2.2 Deployment Layers

- Local/Integration Layer:
  - Docker Compose orchestrates API + PostgreSQL + RabbitMQ
- Orchestrated Layer:
  - Kubernetes base manifests define canonical resources
  - Kustomize overlays adapt for local, staging, production
- IaC Layer:
  - Terraform provisions equivalent Kubernetes resources and environment-specific settings
- CI Layer:
  - GitHub Actions validates code and integration behavior before packaging artifacts

### 2.3 Deployment Architecture Diagram (ASCII)

    +---------------------------------------------------------------+
    |                    Delivery and Runtime Plane                 |
    +---------------------------------------------------------------+
    | Source -> CI (tests) -> Package/JAR -> Container image        |
    |                    |                                           |
    |                    v                                           |
    |             Deploy controls                                    |
    |      (Kustomize overlays / Terraform modules)                 |
    |                    |                                           |
    |                    v                                           |
    |              Kubernetes namespace                              |
    |   +---------------------+      +----------------------------+  |
    |   | Service (ClusterIP) |----->| Deployment: workhub-api   |  |
    |   +---------------------+      | replicas, probes, rolling |  |
    |                                +-------------+--------------+  |
    |                                              |                 |
    |                      +-----------------------+----------------+
    |                      |                                        |
    |                      v                                        v
    |               PostgreSQL dependency                    RabbitMQ dependency
    +---------------------------------------------------------------+

## 3) Docker Architecture

### 3.1 Build Architecture

The Dockerfile uses a production-grade multi-stage strategy:

- deps stage: Maven dependency resolution, cache-efficient
- builder stage: compile and package Spring Boot application
- extractor stage: layered jar extraction for cache-friendly image layers
- runtime stage: minimal JRE runtime, non-root execution

This pattern reduces image size, improves rebuild speed, and lowers runtime attack surface.

### 3.2 Compose Topology

Compose defines three services:

- postgres
- rabbitmq (management plugin enabled)
- app

Key behaviors:

- Health checks on PostgreSQL and RabbitMQ gate app startup
- app waits on healthy dependencies
- Named volumes preserve PostgreSQL and RabbitMQ state
- Production overlay removes host exposure for DB and broker and enforces prod profile

### 3.3 Docker Operational Diagram (ASCII)

    Host
      |
      +-- docker network: workhub-net
            |
            +-- postgres   (health: pg_isready)
            +-- rabbitmq   (health: ping + listener)
            +-- app        (depends_on healthy postgres + rabbitmq)

    Startup chain:
    postgres healthy -> rabbitmq healthy -> app starts -> app liveness/readiness healthy

## 4) Kubernetes Architecture

### 4.1 Base Resource Model

The Kubernetes base manifests define:

- Namespace
- ServiceAccount
- ConfigMap (non-sensitive configuration)
- Secret (credentials injected via overlays, not committed)
- Deployment (workhub-api)
- Service (ClusterIP)
- PodDisruptionBudget

### 4.2 Deployment Design Characteristics

- RollingUpdate strategy with maxUnavailable: 0 for safer rollouts
- Startup, liveness, and readiness probes using actuator endpoints
- Security hardening:
  - runAsNonRoot
  - seccomp RuntimeDefault
  - dropped Linux capabilities
  - automountServiceAccountToken disabled
- Availability and distribution:
  - pod anti-affinity
  - topology spread constraints
- Production isolation controls:
  - production overlay adds NetworkPolicy
  - egress narrowed to DNS plus namespace-scoped DB/AMQP ports

### 4.3 Overlay Model

- local overlay: local-specific settings
- staging overlay: staged image tags and config patches
- production overlay:
  - scales API replicas to 3
  - includes NetworkPolicy
  - includes production patches and generated secrets

### 4.4 Kubernetes Operational Diagram (ASCII)

    Ingress/Gateway (external or internal)
               |
               v
      +---------------------+
      | Service: workhub-api|
      +----------+----------+
                 |
                 v
      +-------------------------------+
      | Deployment: workhub-api       |
      | - replicas                    |
      | - rolling strategy            |
      | - probes                      |
      | - resource requests/limits    |
      +---------+---------------------+
                |
      +---------+----------------------------+
      |                                      |
      v                                      v
    ConfigMap                          Secret
    non-sensitive                      credentials

    Pod runtime dependencies:
      -> PostgreSQL endpoint
      -> RabbitMQ endpoint

## 5) Terraform Architecture

### 5.1 IaC Structure

Terraform is organized into:

- Root module for environment wiring
- Reusable workhub-platform module
- Environment folders for dev, staging, production

The module provisions Kubernetes resources corresponding to the application deployment model (namespace, config, secret, deployment, service, PDB), with validations and defaults aligned to manifest intent.

### 5.2 Important IaC Behaviors

- Sensitive values are declared sensitive and expected from secure input channels
- Resource naming can align with Kubernetes manifest naming to reduce drift
- Defaults generate in-cluster PostgreSQL and RabbitMQ DNS endpoints when not overridden
- Probe and rollout policies are codified and versionable

### 5.3 Terraform Flow Diagram (ASCII)

    tfvars/secrets/TF_VAR -> terraform plan -> terraform apply
                                      |
                                      v
                          Kubernetes API Server
                                      |
                                      v
                    Namespace / ConfigMap / Secret
                                      |
                                      v
                          Deployment / Service / PDB

## 6) CI/CD Lifecycle

### 6.1 Current Lifecycle in Repo

The current workflow provides mature CI and package stages:

1. Unit tests (fast validation)
2. Integration tests (including Testcontainers and RabbitMQ-backed scenarios)
3. Package and Docker build (artifact + image reproducibility)

This creates a quality gate chain where later stages only run if earlier stages succeed.

### 6.2 CD Posture

The repo currently emphasizes CI and deploy-ready artifacts. CD can be attached via controlled promotion using either:

- Kustomize apply paths per environment
- Terraform apply paths per environment

This separation is common in enterprise contexts where deployment authorization and environment promotion are governed separately from build validation.

### 6.3 CI/CD Lifecycle Diagram (ASCII)

    Commit/PR
      |
      v
    Unit Tests
      |
      v
    Integration Tests (Docker/Testcontainers)
      |
      v
    Package JAR + Docker Image
      |
      +--> Artifact retention (reports, hashes, metadata)
      |
      v
    Promotion Decision (manual or policy gate)
      |
      +--> Deploy via Kustomize overlay
      +--> Deploy via Terraform environment

## 7) Why Cloud-Native Deployment Matters

Cloud-native deployment matters because it improves speed, reliability, and operability under real production pressure.

For WorkHub specifically, cloud-native patterns provide:

- Portability: same container artifact can run across environments
- Elasticity: API replicas can scale independently of data services
- Operability: probes and metrics integrate with orchestration health models
- Security posture: runtime hardening, network boundaries, and secret externalization
- Faster recovery: orchestrator can replace unhealthy instances automatically

## 8) Why Infrastructure as Code Matters

IaC is critical because it turns infrastructure from tribal operational knowledge into versioned, reviewable system design.

Benefits in this project context:

- Repeatable environment creation (dev/staging/production)
- Reduced config drift across teams and environments
- Auditable change history for deployment controls
- Safer change previews through planning before apply
- Easier disaster recovery and bootstrap consistency

## 9) Why CI/CD Matters

CI/CD matters because software quality and delivery confidence depend on automated, consistent verification.

In this repo’s lifecycle:

- Unit and integration gates detect regressions before packaging
- Testcontainers improve realism for async and messaging behavior
- Build artifacts and checksums improve traceability and release confidence
- Pipeline concurrency and fail-fast behavior reduce wasted compute and feedback delay

## 10) Operational Resilience Concepts

Resilience mechanisms represented in this architecture include:

- Health model separation:
  - startup probe handles long boot and warm-up
  - liveness probe detects dead/stuck processes
  - readiness probe controls traffic admission
- Rolling deployment safeguards:
  - maxUnavailable set for zero-downtime intent
  - minReadySeconds to avoid premature routing
- Disruption tolerance:
  - PodDisruptionBudget preserves minimum service availability
- Fault isolation:
  - stateless API tier separated from stateful data and messaging tiers
- Defense-in-depth controls:
  - securityContext hardening
  - least privilege container posture
  - production network policy limits lateral movement

## 11) Scalability Concepts

Scalability mechanisms represented in this design include:

- Horizontal scaling:
  - API replicas increase to handle concurrent request load
- Queue-based decoupling:
  - RabbitMQ absorbs spikes and smooths async workloads
- Environment-specific scaling:
  - overlays and Terraform variables adjust replicas/resources by stage
- Resource governance:
  - requests and limits protect cluster fairness and reduce noisy-neighbor impact
- Deployment safety at scale:
  - rolling updates with readiness checks maintain service continuity

## 12) Practical Operating Model

A practical enterprise operating model for this repo is:

- Build and verify in CI on every push/PR
- Promote only immutable artifacts that passed CI
- Apply environment-specific deployment with Kustomize or Terraform
- Validate runtime health via actuator probes and observability endpoints
- Continuously tune replicas, probe timings, and resource limits based on production telemetry

## 13) Source Mapping

This guide is derived from and aligned with:

- [Dockerfile](Dockerfile)
- [docker-compose.yml](docker-compose.yml)
- [docker-compose.prod.yml](docker-compose.prod.yml)
- [k8s/base/kustomization.yaml](k8s/base/kustomization.yaml)
- [k8s/base/deployment.yaml](k8s/base/deployment.yaml)
- [k8s/base/service.yaml](k8s/base/service.yaml)
- [k8s/base/poddisruptionbudget.yaml](k8s/base/poddisruptionbudget.yaml)
- [k8s/overlays/staging/kustomization.yaml](k8s/overlays/staging/kustomization.yaml)
- [k8s/overlays/production/kustomization.yaml](k8s/overlays/production/kustomization.yaml)
- [k8s/overlays/production/networkpolicy.yaml](k8s/overlays/production/networkpolicy.yaml)
- [terraform/main.tf](terraform/main.tf)
- [terraform/modules/workhub-platform/locals.tf](terraform/modules/workhub-platform/locals.tf)
- [terraform/modules/workhub-platform/deployment.tf](terraform/modules/workhub-platform/deployment.tf)
- [terraform/modules/workhub-platform/variables.tf](terraform/modules/workhub-platform/variables.tf)
- [.github/workflows/ci.yml](.github/workflows/ci.yml)
- [.github/CI.md](.github/CI.md)
- [DOCKER.md](DOCKER.md)
- [OBSERVABILITY.md](OBSERVABILITY.md)
