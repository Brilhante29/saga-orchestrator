# Technical Decision

## Selected stack

Kotlin 2.1, JVM 21, Spring Boot 3.4 MVC, Spring JDBC, PostgreSQL 17.6, Flyway, Testcontainers, Gradle 8.10.2 and Docker Compose.

## Why Kotlin

The repository owns a state machine. Kotlin enums, non-null types and exhaustive `when` expressions make missing states visible at compile time. This is a functional improvement over the Java-only simulator, not a cosmetic migration. Java and Kotlin compile to JVM 21; no Kotlin-only type crosses a published Java API.

## Why JDBC

Explicit SQL keeps these proof obligations visible:

- unique `order_id` and idempotency keys;
- one transition sequence per saga;
- partial indexes for recoverable states;
- transaction boundaries around individual mutations;
- `ON CONFLICT DO NOTHING` for replay.

JPA was rejected because entity lifecycle behavior would obscure the SQL central to the benchmark. WebFlux was rejected because PostgreSQL access is blocking.

## SOLID and simplicity

- SRP: orchestrator chooses transitions; store persists them; each step owns one resource.
- OCP/LSP: new resource adapters implement `SagaStep` and obey the same execute/compensate idempotency contract.
- ISP: ports contain only operations required by orchestration.
- DIP: domain code depends on ports, never PostgreSQL or Spring.
- KISS/YAGNI: synchronous calls, one process, one database and no broker.
- DRY: failure and recovery policy lives once in `SagaOrchestrator`; SQL resource behavior is shared by the JDBC step base class.

## Local-first and cloud

`docker compose up --build` is the default. `DB_URL`, `DB_USER` and `DB_PASSWORD` are the only runtime switch needed for a managed PostgreSQL adapter target. Kumo is not used because no AWS API is part of this problem.

## Security and operations

The default credentials are local-only. No secret is committed for a real provider. Actuator exposes health only. Dependency versions are locked, Flyway controls schema changes and the runtime image contains only the application jar plus the lockfile used for benchmark provenance.
