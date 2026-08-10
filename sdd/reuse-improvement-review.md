# Reuse Improvement Review

Project: `16 - saga-orchestrator`

## Review points

- [x] after architecture selection
- [x] after PostgreSQL implementation
- [x] after failure tests
- [x] after benchmark harness
- [x] before release validation

## Findings

| Finding | Classification | Kit area | Action | Status |
|---|---|---|---|---|
| Reliability repos need a gate that rejects claims backed only by in-memory adapters | `backlog` | benchmark harness | require at least one failure integration test for durable claims | recorded |
| V2 evidence needs an explicit producer for dependency and image digests | `backlog` | evidence harness | add a reusable provenance collector script | recorded |
| Sagas require `FAILED` when compensation fails | `backlog` | architecture skills | add compensation-failure invariant to saga guidance | recorded |
| Broker selection must follow the measured problem | `patch_now` | messaging decision | document the no-broker decision and future publisher boundary locally | completed |
| JDBC step SQL is project-specific | `reject` | component packs | keep resource operation/compensation code in this repo | rejected |

## Reuse delta

The repository exposes three kit improvements but does not edit the kit from this isolated worktree. They are recorded for the macro-level reuse pass. The local validator now rejects tracked `.gradle` caches and non-V2 benchmark evidence, preventing both repeated defects here.

## Final gate

- [x] Reusable improvements were patched or recorded.
- [x] Project-specific implementation was not moved into the kit.
- [x] Validation reflects tracked Gradle caches and stale benchmark evidence.
