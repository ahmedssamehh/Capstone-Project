# Async Workflow - RabbitMQ Report Processing

## Objective

Phase 2 introduces asynchronous report generation with reliable, idempotent processing for distributed systems conditions (retries, duplicates, partial failures).

## End-to-End Flow

1. Client calls:
   - `POST /api/projects/{id}/generate-report`
2. Service validates tenant ownership for project.
3. Service creates `Job` in `PENDING`.
4. Service builds `ReportGenerationEvent`:
   - `eventId`, `jobId`, `tenantId`, `projectId`, `timestamp`, `correlationId`
5. Event published to direct exchange.
6. Rabbit listener receives event and delegates to consumer service.
7. Idempotency service reserves or rejects event by unique `eventId`.
8. Consumer transitions job:
   - `PENDING` or retryable `FAILED` -> `PROCESSING` -> `COMPLETED`
   - on error -> `FAILED`
9. Processed message ledger updated:
   - `PROCESSING` -> `COMPLETED` or `FAILED`

## Reliability and Idempotency

- `ProcessedMessage` table enforces unique `eventId`.
- Duplicate deliveries:
  - already completed event -> ignored
  - already processing event -> ignored
- Retry on prior failure:
  - status can move from `FAILED` back to `PROCESSING`.
- Business effect exactly-once:
  - only one successful completion path is allowed for an event ID.

## Components

- Producer orchestration:
  - `ReportGenerationServiceImpl`
- Producer transport:
  - `ReportGenerationProducerServiceImpl`
- Listener transport:
  - `ReportGenerationEventListener`
- Consumer business logic:
  - `ReportGenerationConsumerServiceImpl`
- Dedup/reliability ledger:
  - `MessageIdempotencyServiceImpl`

## Expected API Responses

- Successful enqueue:
  - `202 Accepted`
  - body contains job metadata and `PENDING` status
- Missing/invalid auth:
  - `401`
- Insufficient role:
  - `403`
- Cross-tenant or unknown project:
  - `404`

## Failure Handling

- Producer publish failure:
  - converted to service-level publish exception
  - safe API response via global exception handler
- Consumer processing failure:
  - job marked `FAILED` with truncated reason in independent transaction
  - processed message marked `FAILED` in independent transaction
  - failure evidence persists even if surrounding processing path fails

## Transaction Semantics (Hardening)

- Consumer orchestration is non-transactional.
- State transitions use explicit `REQUIRES_NEW` boundaries:
  - idempotency reservation
  - mark completed
  - mark failed
  - job state transitions
- This design prevents rollback from erasing failure evidence.

## Operational Notes

- Keep queue durable and exchange durable.
- Monitor queue depth and consumer lag.
- Alert on `FAILED` job ratio and repeated retry patterns.
- Correlation ID must be present in producer and consumer logs for every event.
