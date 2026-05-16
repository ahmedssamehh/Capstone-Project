# WorkHub Docker Infrastructure

Enterprise container architecture, deployment flow, and troubleshooting for the WorkHub Spring Boot SaaS backend.

---

## Container architecture

### Topology

```
┌─────────────────────────────────────────────────────────────────┐
│  Host (developer machine / VM)                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Docker network: workhub-net (bridge)                      │  │
│  │                                                            │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │   postgres   │  │   rabbitmq   │  │     app      │   │  │
│  │  │  :5432       │  │  :5672       │  │  :8080       │   │  │
│  │  │              │  │  :15672 mgmt │  │  Spring Boot │   │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │  │
│  │         │                 │                 │            │  │
│  │         └─────────────────┴─────────────────┘            │  │
│  │              DNS: postgres, rabbitmq, app                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│       ▲ published ports (local): 5432, 5672, 15672, 8080       │
└─────────────────────────────────────────────────────────────────┘
```

### Image build (multi-stage)

| Stage | Base image | Output |
|-------|------------|--------|
| `deps` | `maven:3.9-eclipse-temurin-17-alpine` | Cached Maven dependencies |
| `builder` | `deps` | `workhub-0.0.1-SNAPSHOT.jar` |
| `extractor` | `builder` | Exploded Spring Boot layers |
| `runtime` | `eclipse-temurin:17-jre-alpine` | ~375 MB image, non-root `spring` user |

The runtime image contains **only** the JRE, application layers, and `curl` (health checks). No source code, Maven, or JDK.

### Data persistence

| Volume | Mount | Purpose |
|--------|-------|---------|
| `workhub-postgres-data` | `/var/lib/postgresql/data` | Database files |
| `workhub-rabbitmq-data` | `/var/lib/rabbitmq` | Queues, definitions |

Volumes survive `docker compose down`. Use `docker compose down -v` only when you intend to wipe data.

### Security controls

| Control | Implementation |
|---------|----------------|
| Non-root app process | UID/GID 1000 (`spring` user) |
| No secrets in image | Profile and credentials from runtime env |
| Secrets externalized | Required `.env` (local) or secret manager (prod) |
| `no-new-privileges` | All services |
| `init: true` on app | Proper signal handling for JVM |
| Health endpoints public | `/actuator/health/**` only; metrics require auth |
| `.dockerignore` | Excludes `.env`, tests, docs from build context |

---

## Deployment flow

### Local development (one command)

```bash
cp .env.example .env
# Edit .env — set POSTGRES_PASSWORD, RABBITMQ_PASSWORD, JWT_SECRET (32+ chars)

docker compose up --build -d
```

**Startup sequence:**

1. **postgres** — `pg_isready` until accepting connections (`start_period: 20s`).
2. **rabbitmq** — `ping` + port `5672` listener check (`start_period: 40s`).
3. **app** — starts only when both are `healthy` (`depends_on: condition: service_healthy`).
4. **app health** — liveness at `/actuator/health/liveness` after JVM + schema init (~60–120s).

Verify:

```bash
docker compose ps
# Windows
.\scripts\docker-stack-verify.ps1
# Linux/macOS
./scripts/docker-stack-verify.sh
```

### Production-style overlay

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

| Change | Reason |
|--------|--------|
| `SPRING_PROFILES_ACTIVE=prod` | `ddl-auto: validate`, production logging |
| Postgres/RabbitMQ ports not published | Broker and DB only on internal network |
| Higher memory limits | Production workload headroom |

**Before prod overlay:** apply database migrations / ensure schema exists (`application-prod.yml` uses `ddl-auto: validate`).

### Cloud / Kubernetes (typical)

1. Build and push image: `docker build -t registry/workhub:tag .`
2. Inject secrets as env vars or mounted secrets (not `.env` files).
3. Deploy postgres and rabbitmq as managed services **or** operators.
4. Set env from `application-prod.yml`: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `RABBITMQ_*`, `JWT_SECRET`.
5. Configure probes:
   - **Liveness:** `GET /actuator/health/liveness`
   - **Readiness:** `GET /actuator/health/readiness`
6. Do not expose actuator metrics publicly; restrict via network policy or auth.

---

## Environment variables

All secrets are loaded from `.env` (local) or your platform secret store (prod). **Nothing sensitive is defaulted in `docker-compose.yml`.**

| Variable | Required | Used by | Description |
|----------|----------|---------|-------------|
| `POSTGRES_DB` | No (default `workhub`) | postgres, app JDBC path | Database name |
| `POSTGRES_USER` | No (default `postgres`) | postgres, app | DB user |
| `POSTGRES_PASSWORD` | **Yes** | postgres, app | Single source of truth for DB password |
| `POSTGRES_PORT` | No | host mapping | Host port for PostgreSQL |
| `RABBITMQ_USERNAME` | **Yes** | rabbitmq, app | Broker user |
| `RABBITMQ_PASSWORD` | **Yes** | rabbitmq, app | Broker password |
| `RABBITMQ_VHOST` | No | rabbitmq, app | Virtual host (default `/`) |
| `JWT_SECRET` | **Yes** | app | HS256 signing key (min 32 characters) |
| `SPRING_PROFILES_ACTIVE` | No | app | `docker` (local) or `prod` |
| `APP_PORT` | No | host mapping | API port (default `8080`) |
| `WORKHUB_RABBIT_*` | No | app | Exchange, queue, routing key |

`DATABASE_URL` is constructed in compose as `jdbc:postgresql://postgres:5432/${POSTGRES_DB}` so the hostname always matches the Docker service name.

---

## Troubleshooting

### `error: required variable POSTGRES_PASSWORD is missing`

Create `.env` from the template:

```bash
cp .env.example .env
```

Set `POSTGRES_PASSWORD`, `RABBITMQ_PASSWORD`, and `JWT_SECRET` (32+ characters).

### App container exits or restarts

```bash
docker compose logs app --tail 100
```

| Log signal | Likely cause | Fix |
|------------|--------------|-----|
| `Connection refused` to `postgres` | DB not ready or wrong host | Use service name `postgres`, not `localhost` |
| `Connection refused` to `rabbitmq` | Broker not healthy | `docker compose logs rabbitmq`; wait for healthcheck |
| `JWT secret` / token errors | `JWT_SECRET` too short | Use at least 32 characters |
| `Schema-validation` / `ddl-auto: validate` | Prod profile without schema | Use `docker` profile locally, or run migrations before `prod` |
| `password authentication failed` | Credential mismatch | Ensure `DATABASE_PASSWORD` equals `POSTGRES_PASSWORD` in `.env` |

### App stuck in `starting` health state

Spring Boot can take **60–120 seconds** on first start (schema update, connection pools).

```bash
docker compose ps
curl -s http://localhost:8080/actuator/health/readiness
```

Increase `start_period` in compose if running on a slow machine.

### RabbitMQ unhealthy

```bash
docker compose logs rabbitmq --tail 50
docker compose exec rabbitmq rabbitmq-diagnostics -q ping
docker compose exec rabbitmq rabbitmq-diagnostics -q check_port_listener 5672
```

If the volume is corrupt: `docker compose down` then `docker volume rm workhub-rabbitmq-data` (data loss).

### PostgreSQL unhealthy

```bash
docker compose exec postgres pg_isready -U postgres -d workhub
```

### Port already allocated

Another process uses `8080`, `5432`, or `5672`. Change ports in `.env`:

```env
APP_PORT=8081
POSTGRES_PORT=5433
```

### Wipe and reset local stack

```bash
docker compose down -v
docker compose up --build -d
```

### Build failures

```bash
docker compose build --no-cache app
```

Ensure Docker BuildKit is enabled and you have network access for Maven dependencies.

---

## Files reference

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage production image |
| `docker-compose.yml` | Local enterprise stack |
| `docker-compose.prod.yml` | Production overlay (internal-only DB/broker) |
| `.env.example` | Documented variable template |
| `.env` | Local secrets (gitignored) |
| `application-docker.yml` | Compose profile: schema update, connection timeouts |
| `application-prod.yml` | Production profile: validate schema, externalized config |
| `scripts/docker-stack-verify.ps1` | Post-start smoke test (Windows) |
| `scripts/docker-stack-verify.sh` | Post-start smoke test (Unix) |
