# Benchmark Proof

Run `docker compose --profile tools run --rm --build benchmark`.

The producer executes 3 warm-ups and 3 repetitions of 9 scenarios. It reports `consistency_rate`, `uncompensated_sagas` and `recovered_sagas` to `benchmarks/results/saga-reliability-v2.json`.
