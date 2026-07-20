# Agent Handoff

Project: `16 - saga-orchestrator`

## Principal Agent Summary

- Objective: implement Java/Spring saga orchestration with compensation, produce benchmark JSON.
- Portfolio program: backend-reliability-platform
- Public proof claim: transacoes distribuidas com compensacao
- Primary benchmark: consistency_rate
- Default runnable path: `docker run --rm saga-orchestrator`

## Subagent Decisions

| Role | Decision | Evidence Path | Status |
|---|---|---|---|
| `program-planner` | backend-reliability-platform member | `project.yaml`, `sdd/spec.md` | done |
| `architecture-selector` | layered architecture | `sdd/architecture-decision.md` | done |
| `engineering-principles-reviewer` | domain pure Java, no framework deps | `project.yaml`, `sdd/technical-decision.md` | done |
| `stack-decision-agent` | Spring Boot 3.4 + Java 21 + Gradle | `project.yaml`, `sdd/technical-decision.md` | done |
| `api-style-agent` | REST HTTP for optional controller | API contract | done |
| `cloud-local-first-agent` | Docker only, no cloud | `sdd/technical-decision.md` | done |
| `messaging-agent` | none (synchronous saga) | `sdd/technical-decision.md` | done |
| `language-profile-agent` | spring-kotlin-backend (Java variant) | repo layout, tests, tooling | done |
| `benchmark-harness-agent` | BenchmarkRunner + ConsistencyResult | `sdd/benchmark-plan.md`, `benchmarks/results/` | done |
| `design-system-agent` | README with project number + benchmark | `README.md` | done |
| `security-reuse-reviewer` | no secrets, no paid deps | `REFERENCES.md`, release checklist | done |
| `release-ci-publisher` | GitHub Actions CI | CI config | done |

## Local-First Runtime

- Docker command: `docker run --rm saga-orchestrator`
- Local services: none
- Kumo services: none
- Real cloud adapter target: none
- Config switch: none
- Default path requires paid secret: no

## Architecture Boundaries

- Domain boundaries: `com.portfolio.saga.domain` — pure Java, no framework imports
- Use-case boundaries: orchestrator executes sagas; controller exposes REST if needed
- Ports: `SagaLog` interface, `SagaStep` interface
- Adapters: `InMemorySagaLog`, `ReserveInventoryStep`, `ProcessPaymentStep`, `ShipOrderStep`
- Dependency direction rule: domain has zero imports from outside java.*; application imports domain

## Benchmark Handoff

- Metric: consistency_rate
- Unit: unit (0.0–1.0)
- Higher or lower is better: higher (1.0 = perfect)
- Command: `docker run --rm saga-orchestrator`
- Result path: `benchmarks/results/*.json`
- Dataset or fixture: deterministic random (seed 42)

## Open Risks

- None

## Publication Gates

- [x] Docker path works
- [x] benchmark result exists
- [x] README starts with number, claim, and benchmark
- [x] references are documented
- [x] no secret in files or git remote
- [x] validation passes
