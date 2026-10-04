# Saga Orchestrator: Durable Compensation that Survives Crashes

**`consistency_rate = 1.0000` and `uncompensated_sagas = 0.0000` after recovery**, across 3 repetitions of a 9-scenario PostgreSQL failure matrix: failures at every forward step, failures inside compensations, and a crash after each committed step. When compensation fails, the saga reports `FAILED` instead of false success.

[![CI](https://github.com/Brilhante29/saga-orchestrator/actions/workflows/ci.yml/badge.svg)](https://github.com/Brilhante29/saga-orchestrator/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)

## Why this exists

An order that reserves stock, charges a card, and books a shipment touches three resources that cannot share one transaction. When the shipment fails, the charge and the reservation must be undone, and that undo can fail too. Most saga examples handle the happy path and one failure, keep state in memory, and quietly report success when a compensation breaks. This orchestrator is designed around the uncomfortable cases:

- every operation and compensation writes a real PostgreSQL row with a stable idempotency key;
- saga state and transitions are durable, one transaction per transition, so a crash leaves a recoverable checkpoint;
- a crash after a side effect but before the checkpoint replays the same operation without duplicating it;
- a failed compensation leaves the saga visibly `FAILED`, and a later recovery retries exactly that compensation.

## Results

| Metric | Result | Samples | Direction |
|---|---:|---|---|
| `consistency_rate` | 1.0000 | 3 | higher is better |
| `uncompensated_sagas` | 0.0000 | 3 | lower is better |
| `recovered_sagas` | 5.0000/run | 3 | evidence count |

The workload runs the success path, a failure at every forward step, a failure at both reachable compensations, and a crash after each committed step. After the failure is removed and a restart is simulated, only sagas ending `COMPLETED` or `COMPENSATED` count as consistent.

## Quickstart

```bash
docker compose up --build
curl -X POST http://localhost:8080/api/sagas \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-1001","failStep":"CREATE_SHIPMENT"}'
```

Inspect a saga with `GET /api/sagas/{sagaId}`, resume one with `POST /api/sagas/{sagaId}/recover`, or resume every unfinished saga with `POST /api/sagas/recover`. No paid service or secret is required.

Reproduce the benchmark (writes [`benchmarks/results/saga-reliability-v2.json`](benchmarks/results/saga-reliability-v2.json) on the host):

```bash
docker compose --profile tools run --rm --build benchmark
```

## How it works

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

| Failure window | Persisted state | Recovery behavior |
|---|---|---|
| Forward step throws before commit | `COMPENSATING` | Compensates committed steps in reverse order |
| Process stops after resource commit | `RUNNING` with old step index | Replays the step with the same idempotency key |
| Compensation throws | `FAILED` with compensation index | A later recovery retries that exact compensation |
| Recovery succeeds | `COMPLETED` or `COMPENSATED` | Terminal; further starts for the same order are no-ops |

Resource effects and saga checkpoints are intentionally separate transactions; that creates the crash window that idempotent replay closes. Transition rows follow [`contracts/commerce-event-v1.schema.json`](contracts/commerce-event-v1.schema.json).

## Design decisions

| Decision | Why | Rejected |
|---|---|---|
| Orchestration over choreography | One place owns the state machine and its recovery rules | Event choreography for a three-step flow |
| Framework-free domain state machine | The hard logic is testable without Spring or a database | State transitions in controllers or listeners |
| One transaction per transition | Durable checkpoints without pretending the saga is atomic | One transaction around the whole saga |
| Synchronous orchestration | The claim is about compensation and recovery, not delivery | Kafka or RabbitMQ semantics unrelated to the claim |
| Single process, separate logical resources | Isolates saga semantics from network noise | Microservices before the semantics are proven |

## Testing

```bash
./gradlew clean test --no-daemon
```

Integration tests use PostgreSQL through Testcontainers. Without Docker socket access, set `TEST_DATABASE_URL` to an isolated PostgreSQL database.

## Limitations

- One Spring Boot process and one PostgreSQL instance with separate logical resources: saga semantics and recovery windows, not network-distributed microservices.
- No two-phase commit and no broker delivery.
- Recovery is triggered explicitly through the API; there is no background sweeper for stuck sagas yet.

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.1, JVM 21 |
| Runtime | Spring Boot 3.4, Spring MVC |
| Persistence | Spring JDBC, PostgreSQL 17.6, Flyway |
| Verification | JUnit 5, Testcontainers |
| Reproducibility | Gradle wrapper, dependency locks, Docker Compose |

## How this repository is built

The project follows the spec-driven workflow of [portfolio-reuse-kit](https://github.com/Brilhante29/portfolio-reuse-kit). Requirements and decisions live in [`sdd/`](sdd) and [`openspec/changes/durable-postgres-saga/`](openspec/changes/durable-postgres-saga/), and [`project.yaml`](project.yaml) records the architecture, stack, and rejected alternatives. Development is AI-assisted and human-governed: [`AGENTS.md`](AGENTS.md) and [`CLAUDE.md`](CLAUDE.md) hold the coding-agent instructions, while tests, validators, and CI decide what gets published.

## Related work

- [outbox-pattern](https://github.com/Brilhante29/outbox-pattern): reliable event publication when sagas move to asynchronous messaging.
- [spring-hexagonal-payments](https://github.com/Brilhante29/spring-hexagonal-payments): idempotent payment authorization, the kind of step a saga calls.
- [event-sourcing-orders](https://github.com/Brilhante29/event-sourcing-orders): durable order history with CQRS projections.

## Author

**Guilherme Brilhante**, software engineer working on scalable backends and production AI.
[LinkedIn](https://www.linkedin.com/in/guilhermefreirebrilhanteseveriano/) · [GitHub](https://github.com/Brilhante29) · [Publications](https://dblp.org/pid/353/6812.html)

## License

[MIT](LICENSE).
