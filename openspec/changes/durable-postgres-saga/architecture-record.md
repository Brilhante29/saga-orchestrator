# Architecture Record

Use hexagonal architecture. A framework-free `SagaOrchestrator` depends on `SagaStore` and `SagaStep`; JDBC adapters persist state and execute inventory, payment and shipment effects.

Operations commit separately. No broker or two-phase commit is present. This preserves the crash window needed to prove idempotent recovery.
