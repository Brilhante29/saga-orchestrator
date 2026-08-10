# Architecture Decision: hexagonal synchronous saga

## Status

Accepted.

## Context

The previous in-memory layered demo lost all saga state on restart, used no-op compensations and calculated consistency as a consequence of its control flow. The repository must prove a real failure-recovery property without adding infrastructure unrelated to that property.

## Decision

Use a hexagonal architecture with a framework-free Kotlin state machine. `SagaOrchestrator` depends on two outbound port types:

- `SagaStore` persists saga checkpoints and transition events;
- `SagaStep` performs an idempotent resource operation and its idempotent compensation.

PostgreSQL adapters implement all ports. Inventory, payment, shipment and saga checkpoints use separate transactions. There is no transaction wrapping the complete saga. A resource can therefore commit before the saga checkpoint, which is the recovery window under test.

## Messaging decision

No Kafka, RabbitMQ or outbox is used. The orchestration is synchronous, and this repository measures state durability and compensation. A broker would introduce delivery, ordering and consumer semantics that belong to repositories #20 and #28. Transition events are persisted using `commerce-event-v1`, ready for a future publisher adapter.

## Dependency rule

```text
application adapters -> domain ports/state machine <- PostgreSQL adapters
```

The domain has no Spring, JDBC, HTTP, database, broker or cloud import. Controllers contain no business transition logic.

## Consequences

Positive: restart recovery is real, effects are inspectable, adapters are substitutable, and compensation failure cannot be hidden.

Tradeoff: all logical resources share one PostgreSQL server in the local demo. This does not reproduce network partitions or independent service databases and is documented as such.
