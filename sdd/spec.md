# Spec: saga-orchestrator

## Number

#16

## Claim

transacoes distribuidas com compensacao.

## Stack

java21, spring-boot, postgresql, docker

## User-visible output

- Docker command: `docker run --rm saga-orchestrator`
- README opens with: # #16 saga-orchestrator
- Benchmark table: consistency_rate

## Scope

In:

- Implementar o menor produto funcional que prove o claim.
- Rodar por Docker.
- Gerar benchmark JSON reproduzivel.

Out:

- Publicar repo antes do primeiro resultado numerico.
- Depender de segredo pago para o caminho default.

## Architecture

```
SagaApplication (Spring Boot)
  -> SagaOrchestrator (domain)
    -> SagaStep implementations (steps layer)
      -> InMemorySagaLog (application)
  -> BenchmarkRunner (CLI on startup)
    -> ConsistencyResult (JSON output)
```

## Benchmark

Primary metric:

- name: consistency_rate
- target: first reproducible baseline
- command: `docker run --rm saga-orchestrator`
- result file: `benchmarks/results/*.json`

## Dataset or fixture

- source: deterministic pseudo-random (java.util.Random)
- size: 100 iterations (configurable)
- license: Apache 2.0 (project code)
- deterministic seed: 42

## Definition of done

- [x] Docker command works from clean clone.
- [x] README starts with project number and benchmark result.
- [x] Benchmark command writes JSON result.
- [x] Tests cover core behavior.
- [x] REFERENCES.md explains reuse.
- [x] No secret or paid credential required for default demo.
