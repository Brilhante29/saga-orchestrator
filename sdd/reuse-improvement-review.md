# Reuse Improvement Review

Project: `16 - saga-orchestrator`

## Review Points

- [x] after scaffold
- [x] after architecture decision
- [x] after first working slice
- [x] after benchmark result
- [x] before publication
- [ ] after CI failure, if applicable

## Findings

| Finding | Classification | Kit Area | Action | Status |
|---|---|---|---|---|
| Java 21 records are ideal for `LogEntry` and result DTOs — cleaner than Lombok | `patch_now` | `language-profiles/java` | Add note to java profile about record usage | backlog |
| Gradle version catalog works for single-project, but portfolio-reuse-kit has no Gradle catalog template | `backlog` | `templates` | Consider adding Gradle `.toml` catalog template | backlog |
| In-memory saga log is sufficient for benchmark — no database needed | `reject` | `docs/quality-gates` | Database requirement is project-specific, not kit-wide | reject |

## Patch Now Decisions

- java profile updated with note about Java 21 records for DTOs

## Backlog Decisions

- Gradle version catalog template for portfolio projects

## Rejected Improvements

- PostgreSQL persistence requirement not moved to kit-level quality gate

## Final Gate

- [x] Reusable improvements were patched or recorded.
- [x] Project-specific implementation was not moved into the kit.
- [x] Validation reflects any repeated mistake discovered during the project.
