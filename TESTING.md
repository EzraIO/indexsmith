# Testing Guide

The default build is deterministic and does not require a running Elasticsearch cluster. It includes unit tests and local loopback HTTP contract tests for both ES7 and ES8 adapters.

## Test Levels

- Unit tests cover value objects, version rules, neutral CRUD/search responses, JSON DSL, naming, mapping parsing, SQL utilities, and AST construction.
- Generator tests compile all emitted Java files, load the generated repository, and execute its term, match-phrase, nested, pagination, and sorting logic.
- HTTP contract tests verify authentication, generic REST/NDJSON requests, failover, response parsing, mapping discovery, and SPI adapter selection.
- Integration tests use Testcontainers against Elasticsearch 7.11.0, 7.17.22 and 8.17.3 in CI. In addition to adapter checks, they read a real mapping, generate and compile repositories, then execute nested, range, highlight, aggregation, and `search_after` queries through the generated code. They are opt-in because they require Docker.

## Commands

Run the complete default quality gate:

```shell
./mvnw clean verify
```

Run one module and its reactor dependencies:

```shell
./mvnw -pl indexsmith-generator -am test
```

Run real-cluster compatibility tests:

```shell
./mvnw verify -Pintegration-tests
```

Override an image for a focused compatibility run:

```shell
./mvnw -pl indexsmith-client-es7 -am verify -Pintegration-tests \
  -Dindexsmith.test.es7.image=docker.elastic.co/elasticsearch/elasticsearch:7.11.0
```

After `./mvnw install`, compile isolated Maven consumers under `examples/`,
including `generated-consumer`, and run
`gradle -p examples/gradle-consumer compileJava`.

Before publishing source, run `./scripts/verify-public-source.sh`. The check
rejects tracked paths or content containing the retired project identity or
restricted company markers.

The runnable product demo is verified separately with Docker Compose:

```shell
docker compose -f examples/quickstart/compose.yaml up --detach --build
curl --noproxy "*" "http://127.0.0.1:18080/products?name=city%20bike"
docker compose -f examples/quickstart/compose.yaml down --volumes
```

The smoke response must report adapter `es8`, one matching document, and the
generated `match_phrase` query. CI performs the same end-to-end check.
For releases after 0.1.0, run `./mvnw verify -Papi-compatibility
-Dapi.baseline.version=<previous-version>`.

JaCoCo HTML reports are written to `<module>/target/site/jacoco/index.html`. Every module must maintain at least 60% line coverage; generated ANTLR parser sources are excluded. Treat this as a floor, not a substitute for behavior-focused assertions.

## Contribution Rules

Name unit tests `*Test` and Docker-backed tests `*IT`. Tests must not depend on developer-specific paths, credentials, execution order, or external clusters. Bug fixes require a regression test. New adapters must test their supported version range, SPI registration, authentication, request serialization, error handling, and at least one real compatible server version.
