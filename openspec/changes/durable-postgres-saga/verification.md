# Verification

Required gates:

- `./gradlew clean test --no-daemon`
- `docker build -t saga-orchestrator .`
- `docker compose --profile tools run --rm benchmark`
- validate the result against `.portfolio/contracts/benchmark-result-v2.schema.json`
- `tools/validate-project.ps1 -SkipDocker`
- `git ls-files .gradle` returns no path

Publication is blocked if a compensation failure ends as `COMPENSATED`, if a restarted step duplicates a resource row, or if CI uploads a stale committed artifact.
