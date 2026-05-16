# Design Note - Phase 1 Enterprise Hardening

## 1) Architecture

The backend uses layered architecture:

- Controller layer: request mapping, `@Valid`, DTO in/out.
- Service layer: tenant-aware business logic, transaction boundaries.
- Repository layer: JPA persistence with tenant-scoped queries.
- Security layer: JWT authn/authz + RBAC + tenant context propagation.

This keeps HTTP concerns out of business logic and keeps repositories as persistence-only components.

## 2) Tenant Isolation Strategy

Tenant isolation is enforced by design:

1. `JwtAuthenticationFilter` validates JWT and loads authenticated user.
2. Filter sets `TenantContext` from validated claims only.
3. Service layer resolves current tenant via `TenantContext.getRequiredTenantId()`.
4. Repository calls must use tenant-safe methods:
   - `findByIdAndTenantId(...)`
   - `findByTenantId(...)`
   - `existsBy...AndTenantId(...)`

Cross-tenant reads and writes are blocked by tenant-scoped service/repository access patterns.

## 3) Transaction Boundaries

- Services are `@Transactional` by default.
- Read methods are marked `@Transactional(readOnly = true)`.
- Multi-step write flows are transactional to avoid partial writes.
- `TransactionalTestService.testTransactionRollback(...)` intentionally throws after multiple writes to verify rollback behavior end-to-end.

## 4) Error Handling Contract

Global error contract is standardized via `ApiError`:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `correlationId`
- `details` (optional)

Security handlers (`401`, `403`) and controller/service exceptions use the same shape.

