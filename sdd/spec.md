# Spec: durable PostgreSQL order saga

## Public contract

- Project: `#16 saga-orchestrator`
- Claim: durable synchronous saga recovery with idempotent PostgreSQL operations and compensations
- Primary metric: `consistency_rate`
- Guardrail metric: `uncompensated_sagas`

## Required behavior

1. Starting the same `orderId` more than once returns the same saga and does not duplicate a resource effect.
2. Inventory, payment and shipment commit separately; successful prior steps compensate in reverse order after a later failure.
3. Every saga checkpoint and transition survives process restart.
4. A crash after a resource commit but before its checkpoint is recovered by idempotent replay.
5. Compensation failure persists `FAILED`; recovery retries the recorded compensation index and never reports `COMPENSATED` early.

## Scope boundary

In scope: one synchronous Spring process, PostgreSQL persistence, REST control plane, Docker Compose, failure injection and benchmark V2.

Out of scope: broker delivery, network partitions between microservices, two-phase commit, Kubernetes and cloud-specific SDKs.

## Definition of done

- [x] Framework-free Kotlin domain depends on `SagaStore` and `SagaStep` ports.
- [x] Flyway owns durable schema and indexes.
- [x] PostgreSQL integration tests cover idempotency, restart and failed compensation recovery.
- [x] Compose runs without paid credentials.
- [x] Failure matrix emits a schema-v2 JSON with 3 repetitions.
- [x] README states limitations and opens with the measured number.
