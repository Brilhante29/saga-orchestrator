# #16 saga-orchestrator

**Status:** benchmarked

**Proves:** transacoes distribuidas com compensacao.

**Stack:** java21, spring-boot, postgresql, docker.

## Run

```bash
docker build -t saga-orchestrator .
docker run --rm saga-orchestrator
```

## Benchmark

consistency_rate — percentage of sagas that complete successfully or compensate cleanly.

```bash
docker run --rm saga-orchestrator --benchmark
```

| Metric | Value | Unit |
|---:|---:|---:|
| consistency_rate | 1.0 | unit |

100 iterations with 20% failure probability — all sagas either completed all steps or compensated cleanly. Deterministic seed (42) ensures reproducible results.

## Architecture

Three-step order saga executed by `SagaOrchestrator`:

```
reserve-inventory -> process-payment -> ship-order
       |                    |                  |
  (comp no-op)      (comp no-op)        (comp no-op)
```

On step failure, prior steps compensate in reverse order. Domain layer has zero framework dependency.

## References

See REFERENCES.md.

## Benchmark result schema

```json
{
  "project": "saga-orchestrator",
  "metric": "consistency_rate",
  "value": 1.0,
  "unit": "unit",
  "details": { "completed": N, "compensated": N, "failed": 0 }
}
```
