# RabbitMQ Setup Guide - WorkHub Phase 2

## Purpose

This guide provides deployment and verification steps for RabbitMQ integration in WorkHub.

## Local Setup with Docker Compose

Start services:

```bash
docker compose up -d --build
```

Expected containers:

- `workhub-app`
- `workhub-postgres`
- `workhub-rabbitmq`
- `workhub-pgadmin`

## Ports

- AMQP: `5672`
- RabbitMQ Management UI: `15672`

## Default Credentials

- username: `workhub`
- password: `workhub`

## Management UI

Open:

- [http://localhost:15672](http://localhost:15672)

Verify:

- direct exchange exists
- durable queue exists
- binding exists for routing key

## Application Configuration

Environment variables used by app:

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `WORKHUB_RABBIT_EXCHANGE`
- `WORKHUB_RABBIT_QUEUE`
- `WORKHUB_RABBIT_ROUTING_KEY`

## Runtime Verification

1. Login and get token.
2. Trigger report:
   - `POST /api/projects/{id}/generate-report`
3. Verify:
   - API returns `202 Accepted` with `PENDING` job.
   - Consumer logs show event received and workflow executed.
   - job transitions to `COMPLETED` or `FAILED`.

## Expected Log Indicators

- Producer publish:
  - `Published ReportGenerationEvent ...`
- Listener receive:
  - `Received ReportGenerationEvent ...`
- Workflow status:
  - `Job transitioned to PROCESSING`
  - `Job transitioned to COMPLETED` or `FAILED`
- Correlation continuity:
  - same correlation ID appears across producer and consumer logs.

## Enterprise Operations Notes

- Enable dead-letter strategy for poison messages in production environments.
- Configure queue policies for HA where required.
- Monitor consumer lag and failed-event rates.
- Rotate credentials and avoid defaults outside local development.
