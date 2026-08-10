# #16 saga-orchestrator

**Proves:** a synchronous saga can survive process interruption, retry idempotently, persist every transition, and expose uncompensated work instead of reporting false success.

**Measured baseline:** `consistency_rate = 1.0000`; `uncompensated_sagas = 0.0000` after recovery, across 3 repetitions of a 9-scenario PostgreSQL failure matrix.

## What runs

This repository coordinates three separately committed business operations:

1. reserve inventory;
2. authorize payment;
3. create shipment.

Each operation and compensation writes a real PostgreSQL row with a stable idempotency key. Saga state and transition events are durable. A crash after a side effect but before the saga checkpoint replays the same operation without creating a duplicate.

This is one Spring Boot process and one PostgreSQL instance with separate logical resources. It demonstrates saga semantics and recovery windows; it does **not** claim network-distributed microservices, two-phase commit, or broker delivery.

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.1, JVM 21 |
| Runtime | Spring Boot 3.4, Spring MVC |
| Persistence | Spring JDBC, PostgreSQL 17.6, Flyway |
| Verification | JUnit 5, Testcontainers |
| Reproducibility | Gradle wrapper, dependency locks, Docker Compose |

## Run locally

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. No paid service or secret is required.

```bash
curl -X POST http://localhost:8080/api/sagas \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-1001","failStep":"CREATE_SHIPMENT"}'
```

Inspect a saga with `GET /api/sagas/{sagaId}`. Resume one with `POST /api/sagas/{sagaId}/recover`.

## Reproduce the benchmark

```bash
docker compose --profile tools run --rm --build benchmark
```

The container writes [saga-reliability-v2.json](benchmarks/results/saga-reliability-v2.json) on the host. The workload executes success, failure at every forward step, failure at both reachable compensations, and a crash after each committed step.

| Metric | Result | Samples | Direction |
|---|---:|---|---|
| `consistency_rate` | 1.0000 | 3 | higher is better |
| `uncompensated_sagas` | 0.0000 | 3 | lower is better |
| `recovered_sagas` | 5.0000/run | 3 | evidence count |

`FAILED` is observable immediately when compensation fails. The benchmark then simulates restart with the failure removed; only sagas ending `COMPLETED` or `COMPENSATED` count as consistent.

## Architecture

```text
HTTP / benchmark adapter
        |
        v
SagaOrchestrator (framework-free Kotlin state machine)
   | SagaStore port          | SagaStep ports
   v                         v
JdbcSagaStore          inventory / payment / shipment JDBC adapters
   |                         |
   +----------- PostgreSQL --+
```

- Domain imports no Spring, JDBC, broker, or transport type.
- A database transaction protects each state transition, not the entire saga.
- Resource effects and saga checkpoints are intentionally separate transactions; this creates the crash window that idempotent replay closes.
- Orchestration is synchronous. Kafka or RabbitMQ would add delivery semantics unrelated to this repository's claim.
- Transition rows follow [commerce-event-v1.schema.json](contracts/commerce-event-v1.schema.json). No broker publishes them in this project.

## Test

```bash
./gradlew clean test --no-daemon
powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate-project.ps1 -SkipDocker
```

Integration tests use PostgreSQL through Testcontainers. For a build container without Docker socket access, set `TEST_DATABASE_URL` to an isolated PostgreSQL database.

## Failure semantics

| Failure window | Persisted state | Recovery behavior |
|---|---|---|
| Forward step throws before commit | `COMPENSATING` | compensates previously committed steps in reverse order |
| Process stops after resource commit | `RUNNING` with old step index | replays the step using the same idempotency key |
| Compensation throws | `FAILED` with compensation index | a later recovery retries that exact compensation |
| Recovery succeeds | `COMPLETED` or `COMPENSATED` | terminal; further starts for the same order are no-ops |

Design decisions and limits are recorded under `sdd/` and `openspec/changes/durable-postgres-saga/`.
