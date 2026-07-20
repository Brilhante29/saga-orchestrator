# Architecture Decision

## Status

Accepted

## Context

Project: `saga-orchestrator`
Claim: `transacoes distribuidas com compensacao`
Benchmark: `consistency_rate`

Problem forces:

- Domain complexity: `medium` — saga orchestration with compensation requires ordered execution and rollback
- Integration pressure: `low` — single JVM process, no external services
- UI state complexity: `none` — CLI/benchmark only
- Data/ML reproducibility: `low` — no ML, data is in-memory
- Auditability/event history: `medium` — saga log tracks every transition
- Throughput/async pressure: `low` — synchronous single-threaded demo
- Independent deployability need: `low` — single Docker container

## Decision

Chosen architecture: `layered`

Reason:

Domain layer (`Saga`, `SagaOrchestrator`, `SagaStep`, `SagaLog`) has zero framework imports — pure Java POJOs. Application layer (`InMemorySagaLog`, `SagaController`) depends on domain and adds Spring/Jackson. This lets the orchestrator be tested without Spring, meeting the "domain does not depend on framework" principle.

Dependency rule:

`domain -> (no deps)` — pure Java. `application -> domain + Spring Web`. `steps -> domain`. `benchmark -> domain + Jackson`.

## Rejected Alternatives

| Alternative | Why rejected |
|---|---|
| Event-driven (message broker) | Adds Kafka/RabbitMQ without improving the compensation proof |
| Hexagonal / Clean Architecture | Overengineering for a single-benchmark project; layers are sufficient |
| Kotlin | Java 21 records and interfaces express the pattern just as well |

## Folder Layout

```
src/main/java/com/portfolio/saga/
  SagaApplication.java
  domain/       (pure Java, no framework)
  application/  (Spring REST + in-memory infra)
  steps/        (step implementations)
  benchmark/    (runner + result JSON)
src/test/java/com/portfolio/saga/
  domain/
  benchmark/
```

## Testing Strategy

- Unit tests: saga orchestrator, saga log, order saga, compensation — all without Spring
- Benchmark: `BenchmarkRunnerTest` verifies deterministic results, zero-failure edge case, full-failure edge case

## Consequences

Positive:

- Domain is testable in isolation, no Spring context needed
- Simple layered structure is easy to understand and benchmark
- Compensation pattern is clearly visible in orchestrator

Tradeoffs:

- In-memory only (no PostgreSQL persistence for saga log)
- Synchronous execution (no parallel step execution)

Migration path:

- Replace `InMemorySagaLog` with JDBC/Spring Data PostgreSQL adapter
- Add async step execution with `CompletableFuture` if throughput becomes relevant
