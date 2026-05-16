# Phase 2 Enterprise Test Plan Matrix

This matrix maps Phase 2 requirements to executable integration tests and expected evidence.

| Requirement | Test Class | Test Method | Expected Behavior | Evidence |
|---|---|---|---|---|
| Tenant isolation: cross-tenant read blocked | `TenantIsolationSecurityIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `crossTenantReadBlocked` | Tenant B cannot read Tenant A project (`404`) | API response `ApiError`, logs |
| Tenant isolation: cross-tenant update blocked | `TenantIsolationSecurityIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `crossTenantUpdateBlocked` | Tenant B cannot update Tenant A task (`404`) | API response `ApiError`, DB unchanged |
| Tenant isolation: list isolation | `TenantIsolationSecurityIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `crossTenantListIsolation` | Tenant B list excludes Tenant A records | JSON response assertions |
| RBAC: missing token -> 401 | `RbacIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `missingTokenReturns401` | Unauthenticated request denied | `401`, unified `ApiError` |
| RBAC: invalid token -> 401 | `RbacIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `invalidTokenReturns401` | Malformed/invalid token denied | `401`, unified `ApiError` |
| RBAC: insufficient role -> 403 | `RbacIntegrationTest` / `Phase2EnterpriseIntegrationTest` | `insufficientRoleReturns403` | TENANT_USER blocked from admin mutation | `403`, unified `ApiError` |
| RBAC: admin allowed | `RbacIntegrationTest` | `adminRoleSucceeds` | TENANT_ADMIN can mutate protected resource | `201 Created` |
| Async workflow: report request creates job | `Phase2EnterpriseIntegrationTest` | `reportRequestCreatesAsyncJob` | Report request enqueues PENDING job | `202 Accepted`, job persisted |
| Messaging: producer publishes event | `Phase2EnterpriseIntegrationTest` | `rabbitMqEventPublished` | Event reaches consumer path and ledger entry appears | `processed_messages` row exists |
| Messaging: consumer processes event | `Phase2EnterpriseIntegrationTest` | `consumerProcessesEvent` | Job transitions to COMPLETED | DB status assertion |
| Idempotency: duplicate event ignored | `Phase2EnterpriseIntegrationTest` | `duplicateEventIgnored` | Duplicate delivery does not reapply business effect | unchanged attempt/state semantics |
| Failure safety: failed processing durable | `Phase2EnterpriseIntegrationTest` | `failedProcessingHandledSafely` | FAILED state persisted for job + ledger | DB `FAILED` + error evidence |
| Retry safety: retry after failure | `Phase2EnterpriseIntegrationTest` | `retryAfterFailureProcessesSafely` | Retry transitions FAILED -> COMPLETED safely | attempt count increments, final COMPLETED |
| Observability: actuator endpoints available | `Phase2EnterpriseIntegrationTest` | `actuatorEndpointsAccessible` | health/readiness/liveness available; metrics with auth | endpoint status assertions |
| Observability security: metrics protected | `Phase2EnterpriseIntegrationTest` | `actuatorMetricsProtectedWithoutToken` | metrics endpoint not publicly accessible | `401` |
| Correlation tracing: log correlation presence | `Phase2EnterpriseIntegrationTest` | `correlationIdsPresentInLogs` | request correlation appears in logs | captured output contains correlation ID |
| Transaction rollback integrity | `TransactionRollbackIntegrationTest` | `rollbackShouldLeaveNoPartialWrites` | Multi-step write rollback leaves no partial data | DB verification |
| Security edge hardening | `SecurityEdgeCaseIntegrationTest` | all methods | malformed/expired/tampered/missing claims rejected | `401`, consistent error contract |

## CI Execution Policy

- Full suite runs in CI through `.github/workflows/ci.yml`.
- Build fails if any tests are skipped.
- Testcontainers-based tests are expected to run on Docker-enabled runners.

## Operational Notes

- Local machines without Docker can execute non-container tests.
- Phase 2 Docker-dependent integration tests are still required for release acceptance and grading evidence.
