# Agent Handoff

Project: `16 - saga-orchestrator`

## Current state

- Branch: `codex/backend-reliability-close`
- Runtime: Kotlin/JVM 21 + Spring Boot + PostgreSQL 17.6
- Architecture: hexagonal synchronous saga
- Default command: `docker compose up --build`
- Benchmark: `docker compose --profile tools run --rm --build benchmark`
- Result: `benchmarks/results/saga-reliability-v2.json`

## Observable decisions

| Area | Decision | Evidence |
|---|---|---|
| Domain | `SagaOrchestrator` is framework-free | `src/main/kotlin/com/portfolio/saga/domain/` |
| Durability | saga state, transitions and resource effects persist in PostgreSQL | Flyway V1 migration and JDBC adapters |
| Failure semantics | failed compensation persists `FAILED`; recovery resumes its index | domain and PostgreSQL integration tests |
| Idempotency | `order_id` and operation keys are unique; replay uses `ON CONFLICT` | migration and `JdbcOrderSteps.kt` |
| Messaging | none; synchronous orchestration is the measured problem | architecture decision |
| Contract | transition envelope is `commerce-event-v1` | `contracts/commerce-event-v1.schema.json` |

## Verification performed

- Domain tests cover every forward-step failure, compensation failure and crash replay.
- PostgreSQL tests cover durable restart, one-row idempotency and compensation recovery with a new orchestrator instance.
- CI must run tests, validator, image build, Compose benchmark and schema validation.

## Continuation order

1. Read `AGENTS.md`, `project.yaml` and this file.
2. Run `git status --short`; do not touch `C:\tmp\saga-kotlin`.
3. Run tests and validator before changing public numbers.
4. Regenerate benchmark evidence after implementation changes.
5. Record reusable kit improvements in `sdd/reuse-improvement-review.md`.

No internal reasoning transcript is required; decisions, evidence and remaining actions above are sufficient to continue safely.
