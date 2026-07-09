# #16 saga-orchestrator

**Status:** scaffold

**Proves:** transacoes distribuidas com compensacao.

**Benchmark target:** consistency_rate.

**Stack:** java21, spring-boot, postgresql, docker.

## Next milestone

Implement the smallest Docker-runnable version and produce the first JSON benchmark under enchmarks/results/.

## Run

`ash
docker build -t saga-orchestrator .
docker run --rm saga-orchestrator
`

## Benchmark

`ash
docker run --rm saga-orchestrator benchmark
`

| Metric | Value | Unit |
|---|---:|---|
| consistency_rate | pending | pending |

## Architecture

Defined in sdd/spec.md before implementation.

## References

See REFERENCES.md.