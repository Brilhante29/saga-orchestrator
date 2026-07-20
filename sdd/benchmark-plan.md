# Benchmark Plan: saga-orchestrator

## Hypothesis

transacoes distribuidas com compensacao, measured by consistency_rate.

## Command

```bash
docker run --rm saga-orchestrator
```

## Environment

- OS: Alpine Linux (Docker container)
- CPU: host-dependent
- RAM: host-dependent
- GPU: N/A
- Docker version: 24+
- Date: recorded in result JSON

## Inputs

- fixture: deterministic pseudo-random (java.util.Random)
- dataset size: 100 iterations
- repetitions: 1 run per execution
- warmup: none

## Metrics

| Metric | Unit | Source | Why it matters |
|---|---:|---|---|
| consistency_rate | unit | BenchmarkRunner.run() | proves the repo claim — all sagas reach a consistent state |

## Result schema

Output must be JSON and include project, metric, value, unit, timestamp, environment, and command.

```json
{
  "project": "saga-orchestrator",
  "metric": "consistency_rate",
  "value": 1.0,
  "unit": "unit",
  "timestamp": "2026-07-20T17:30:00Z",
  "environment": {
    "java_version": "21",
    "os": "Linux",
    "available_processors": "N"
  },
  "command": "java -jar saga-orchestrator.jar benchmark",
  "details": {
    "iterations": 100,
    "failure_probability": 0.2,
    "seed": 42,
    "completed": 80,
    "compensated": 20,
    "failed": 0
  }
}
```

## Post angle

#16 saga-orchestrator: consistency_rate as a reproducible portfolio benchmark.
