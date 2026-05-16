# OBSERVABILITY - PHASE 2 ENTERPRISE GUIDE

## Scope

This guide documents production-ready observability for WorkHub:

- Spring Boot Actuator health and probes
- Micrometer metrics
- Structured logging
- Correlation ID tracing across HTTP and async flows

## Exposed Operational Endpoints

- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/health/liveness`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Architecture

- `CorrelationIdFilter`
  - creates/propagates `X-Correlation-Id`
  - stores `correlationId` in MDC
- `RequestTracingFilter`
  - adds method/path to MDC
  - emits `request-start` and `request-end` with latency
- `ObservabilityConfig`
  - enables async MDC propagation via `TaskDecorator`
  - configures metric common tags
- `RabbitMQConfig`
  - propagates correlation ID into AMQP headers before publish

## Structured Logging Contract

Logs are emitted as key-value records with:

- timestamp
- level
- app name
- correlation ID (`corr`)
- HTTP method and path
- logger
- message

This format is SIEM- and grep-friendly for incident analysis.

## Verification Checklist

1. Health endpoint:
   - `curl http://localhost:8080/actuator/health`
2. Readiness endpoint:
   - `curl http://localhost:8080/actuator/health/readiness`
3. Liveness endpoint:
   - `curl http://localhost:8080/actuator/health/liveness`
4. Metrics endpoint:
   - `curl http://localhost:8080/actuator/metrics`
5. Correlation trace:
   - send `X-Correlation-Id: corr-demo-001`
   - verify request logs include `corr=corr-demo-001`
6. Async trace:
   - trigger report generation
   - verify producer and consumer logs carry same correlation ID

## Expected Behaviors

- Request without header:
  - server generates correlation ID and returns it in response header.
- Request with header:
  - same ID appears in response header, API error body, and logs.
- Async job execution:
  - correlation ID persists through publish and consume workflow.

## Enterprise Rationale

- Faster root-cause analysis during incidents.
- Correlated traces for user request to background processing.
- Probe endpoints for orchestration platforms (readiness/liveness).
- Metrics foundation for SLO/SLA and capacity planning.

## Operational Notes

- Keep actuator exposure minimal in internet-facing production; front with gateway and IP allow lists.
- Export metrics to Prometheus/Grafana in deployment environments.
- Enforce retention and log redaction policies (no secrets in logs).
