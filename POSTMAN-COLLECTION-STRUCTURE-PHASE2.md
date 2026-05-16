# Postman Collection Structure - Phase 2

## Goal

This structure organizes Postman requests by enterprise validation themes for grading, demo, and technical defense.

## Collection Name

- `WorkHub-Phase2-Enterprise`

## Recommended Variables

- `baseUrl`
- `adminEmail`
- `adminPassword`
- `userEmail`
- `userPassword`
- `adminToken`
- `userToken`
- `tenant2AdminToken`
- `projectId`
- `taskId`
- `jobId`
- `correlationId`

## Folder Structure

### 1) Auth

- Login Admin
- Login User
- Get Me

### 2) Tenant Isolation

- Cross-tenant read blocked
- Cross-tenant update blocked
- Cross-tenant list isolation

### 3) RBAC and Security

- Missing token -> 401
- Invalid token -> 401
- Insufficient role -> 403
- Admin mutation success

### 4) Async Report Workflow

- Generate report (`POST /api/projects/{id}/generate-report`)
- Get job by ID
- List jobs

### 5) Reliability and Idempotency

- Publish duplicate event scenario verification (through API trigger + repeated publish helper request if needed)
- Failed processing scenario verification
- Processed message status inspection endpoint (if later added)

### 6) Observability

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/metrics`

## Request-Level Test Scripts (Recommended)

- Assert response status and JSON shape.
- Assert `correlationId` exists in error payloads.
- Store token/jobId/environment variables for workflow chaining.
- Assert expected role behavior (`403` for forbidden mutation).

## Defense-Oriented Demo Order

1. Auth success.
2. RBAC failure then admin success.
3. Tenant isolation proof calls.
4. Async report request returning `202`.
5. Job polling to completion.
6. Observability endpoint checks.

This flow demonstrates architecture correctness, security posture, and operational readiness in a short live defense.
