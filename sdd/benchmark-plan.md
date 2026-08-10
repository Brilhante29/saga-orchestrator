# Benchmark Plan: durable failure matrix

## Hypothesis

After injected step failures, compensation failures and process interruptions, every measured saga reaches `COMPLETED` or `COMPENSATED` after recovery, leaving zero uncompensated sagas.

## Command

```bash
docker compose --profile tools run --rm --build benchmark
```

## Workload

Each repetition runs nine scenarios: success; failure at inventory, payment and shipment; failure at inventory and payment compensation; and crash after inventory, payment and shipment commit. Compensation failures and crashes are followed by a recovery pass without injection.

- warm-up: 3 successful sagas
- repetitions: 3
- measured scenarios: 27
- concurrency: 1, because ordering/recovery rather than throughput is the claim
- database: PostgreSQL 17.6 container

## Metrics

| Metric | Formula | Target |
|---|---|---:|
| `consistency_rate` | terminal consistent sagas / measured sagas | 1.0 |
| `uncompensated_sagas` | sagas still `FAILED` after recovery | 0 |
| `recovered_sagas` | injected crashes or compensation failures recovered per repetition | 5 |

Only `COMPLETED` and `COMPENSATED` are consistent. A failed compensation is counted as failed until a later recovery actually commits the remaining compensations.

## Evidence format

`benchmarks/results/saga-reliability-v2.json` must validate against `.portfolio/contracts/benchmark-result-v2.schema.json` and include fixture/config digests, three samples per metric, execution metadata, dependency-lock digest, image identity and source commit.

## Limitations

The workload is deterministic and single-threaded. It proves correctness under defined crash windows; it does not claim throughput, network partition tolerance or broker delivery.
