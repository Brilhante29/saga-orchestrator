# Technical Decision

## Status

Accepted

## Decision Type

stack, framework, library, runtime

## Context

Project: `saga-orchestrator`
Problem: demostrar transacoes distribuidas com compensacao via saga pattern
Portfolio program: `backend-reliability-platform`
Public signal: Java/Spring distributed transaction knowledge
Benchmark: `consistency_rate`

## Selected Option

Selected: `Spring Boot 3.4 + Java 21 + Gradle 8.10 + Jackson 2.18`

Reason:

Spring Boot provides REST API and dependency injection scaffolding with minimal boilerplate. Java 21 records cleanly model `LogEntry` and result types. Gradle with version catalog provides clean dependency management. Jackson handles JSON benchmark output without additional libraries.

## Decision Brain Fields

- Stack profile: `spring-kotlin-backend`
- API style: `rest-http`
- Messaging: `none`
- Cloud mode: `none`
- Database/runtime: `docker`
- Library policy: minimal dependencies — spring-boot-starter-web, jackson-databind, junit-jupiter

## Engineering Principles

Coupling boundary:

Domain/use cases must not depend on framework, DB, broker, cloud SDK, transport, or UI.

SOLID application:

- SRP: each domain class has one responsibility (Saga = aggregate, SagaOrchestrator = execution, SagaLog = audit)
- OCP: new step types implement SagaStep without modifying orchestrator
- LSP: SagaStep implementations are substitutable (ReserveInventoryStep, ProcessPaymentStep, ShipOrderStep)
- ISP: SagaLog is a 4-method interface; SagaStep is a 3-method interface
- DIP: SagaOrchestrator depends on SagaStep and SagaLog abstractions, not concrete implementations

Simplicity:

- KISS: synchronous loop with try/catch for compensation — simplest proof of the pattern
- YAGNI: no database, no message broker, no async, no Kubernetes
- DRY: step pattern expressed via shared SagaStep interface; no repeated compensation logic

Testability evidence:

- `SagaOrchestratorTest` — pure Java test, no Spring, verifies execution order and compensation
- `BenchmarkRunnerTest` — deterministic seed ensures reproducible metrics

## Rejected Options

| Option | Why rejected |
|---|---|
| Kotlin | Java 21 records suffice; Kotlin adds compile-time overhead |
| Kafka/RabbitMQ | Unnecessary for single-node synchronous saga |
| PostgreSQL for saga log | Adds Docker Compose dependency; in-memory log is sufficient for benchmark |
| JPA / Hibernate | No database needed for in-memory benchmark |

## API Contract

Contract artifact:

`POST /api/saga/order` — creates an order saga with optional step failure flags
`GET /api/saga/health` — health check

## Cloud Local-First

Local provider: `docker`

Real provider target: `none`

Config switch:
```
none
```

## Benchmark Impact

Expected impact: consistency_rate = 1.0 (all sagas either complete or compensate cleanly when compensation is no-op)

Validation command:
```
docker run --rm saga-orchestrator
```

## Operational Cost

- Docker services added: none
- Local demo complexity: low
- Failure case required: yes (step failure triggers compensation)

## Follow-up

If benchmark shows consistency_rate < 1.0, investigate SagaOrchestrator compensation logic for unhandled exceptions.
