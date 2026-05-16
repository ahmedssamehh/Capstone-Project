# Troubleshooting Guide - Phase 2 Enterprise Features

## 1) RabbitMQ Connection Failures

Symptoms:

- app startup logs show broker connection refused
- async jobs stay in `PENDING`

Checks:

- `docker compose ps` shows `workhub-rabbitmq` running and healthy
- app env vars match broker host/port/credentials
- port `5672` reachable

Fix:

- restart stack: `docker compose down && docker compose up -d --build`
- verify broker credentials and vhost

## 2) Jobs Never Complete

Symptoms:

- job remains `PENDING` or `PROCESSING`

Checks:

- listener log entries for received events
- queue depth in RabbitMQ management UI
- consumer errors in application logs

Fix:

- verify queue/exchange/routing key match configuration
- inspect `ProcessedMessage` entries for `FAILED` and `lastError`
- replay by creating a new job request after correcting root cause

## 3) Duplicate Event Side Effects

Symptoms:

- concern about repeated processing after retries

Checks:

- `processed_messages` has unique `event_id`
- duplicate event logs show "already processing" or "already completed"

Fix:

- confirm idempotency service is active in consumer path
- ensure event IDs are not regenerated on retry publish

## 4) Unexpected 401/403 Responses

Checks:

- token validity and expiration
- token role claim
- endpoint role requirement

Expected behavior:

- missing/invalid token -> `401`
- wrong role -> `403`

## 5) Cross-Tenant Access Appears Broken

Checks:

- resource truly belongs to same tenant as token
- service path uses tenant-scoped repository methods

Expected behavior:

- cross-tenant access should return `404` and not leak existence details

## 6) Actuator Endpoints Unavailable

Checks:

- dependencies include actuator
- management exposure configured in `application.yml`
- security allows `/actuator/health/**` and `/actuator/metrics/**`

## 7) Correlation IDs Missing in Logs

Checks:

- request carries `X-Correlation-Id` or filter generates one
- log pattern includes MDC key `correlationId`
- async producer/consumer paths propagate correlation correctly

## Operational Escalation Checklist

- Capture:
  - correlation ID
  - timestamp
  - endpoint/event ID
  - tenant ID and job ID
- Attach:
  - relevant app logs
  - RabbitMQ queue/exchange snapshot
  - actuator health/readiness output
- Classify:
  - security, data isolation, async reliability, or observability incident
