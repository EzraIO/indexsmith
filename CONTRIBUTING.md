# Contributing

## Before opening a change

Read [AGENTS.md](AGENTS.md) and [TESTING.md](TESTING.md). Keep Maven
coordinates under `io.github.ezraio` and Java packages under
`io.github.indexsmith`.
Place production code and tests in the matching module; do not commit
`target/`, generated Antlr output, IDE metadata, credentials, or local
cluster data.

## Local checks

Use the committed wrapper and Java 8-compatible source level:

```shell
./mvnw --batch-mode --no-transfer-progress clean verify
./mvnw verify -Pintegration-tests       # requires Docker
```

Changes to stable public modules must also pass the `api-compatibility`
profile once a published baseline exists. Dependency updates must satisfy the
Enforcer dependency-convergence rule and must not weaken pinned workflow or
release-signing controls.

Every behavior change needs a focused JUnit 4 regression test. Preserve the
60% per-module JaCoCo floor. For generated code, assert both generated source
shape and observable query behavior. Keep adapter tests independent: test ES7
and ES8 separately, then exercise common behavior through `indexsmith-client-core`.

## Pull requests

Use a short imperative commit subject such as
`indexsmith-client-core: reject unsupported server major`. A pull request must explain
the motivation, affected modules, compatibility implications, tests run, and
any generated files or dependency changes. Link an issue when one exists.
Security-sensitive reports must follow [SECURITY.md](SECURITY.md), not a
public issue.
