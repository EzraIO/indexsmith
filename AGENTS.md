# Repository Guidelines

## Project Structure & Module Organization

This Java 8 multi-module Maven project contains:

- `bom/`: supported public-module dependency versions for Maven and Gradle.
- `api/`: shared repository APIs, request/response models, and JSON helpers.
- `generator/`: mapping parsers and Java source generators.
- `maven-plugin/`: Maven plugin that exposes the generator as the `generate` goal.
- `elasticsearch-sql/`: SQL grammar, ANTLR integration, AST nodes, and transformations.
- `utils/`: reusable JavaParser/AST utilities.
- `client-core/`: version-neutral client contracts, discovery, and server-version detection.
- `client-es7/`, `client-es8/`: version-specific client adapters loaded through Java SPI.
- `examples/`: isolated Java 8, Spring Boot 2.7, and Gradle consumers.

Production code belongs under `<module>/src/main/java`; resources go in `src/main/resources`. ANTLR grammars live in `elasticsearch-sql/src/main/antlr`; tests go in matching packages under `<module>/src/test/java`. Treat `target/`, generated ANTLR output, and IDE files as build artifacts.

## Build, Test, and Development Commands

- `./mvnw clean verify`: compile every module, run default tests, and enforce JaCoCo coverage.
- `./mvnw test`: run the reactor unit and local HTTP contract tests without packaging.
- `./mvnw verify -Pintegration-tests`: run Docker-backed adapter and generated-repository compatibility tests.
- `./mvnw -pl generator -am test`: test one module plus its required reactor dependencies.
- `./mvnw -pl maven-plugin -am package`: build the Maven plugin and dependencies.
- `./mvnw -pl elasticsearch-sql antlr4:antlr4`: regenerate parser sources from the `.g4` grammars.
- `./mvnw verify -Papi-compatibility -Dapi.baseline.version=0.1.0`: check stable APIs against a published baseline.

Run from the root with the Maven Wrapper. Production sources target Java 8; release tooling runs on Java 17+. Client compatibility depends on parent-POM versions.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, and standard Java braces. Packages are lowercase (`io.github.outakeshi.indexsmith...`); types use `UpperCamelCase`, methods/fields `lowerCamelCase`, and constants `UPPER_SNAKE_CASE`. Keep module boundaries intact and imports organized; no formatter or linter is configured.

## Testing Guidelines

JUnit 4 is the configured framework. Name unit tests `*Test`, integration tests `*IT`, and methods for observable behavior, such as `transformRejectsInvalidSql`. Add tests beside the module being changed. Never embed local paths, credentials, execution-order assumptions, or external cluster dependencies. Each module must retain at least 60% line coverage; generated ANTLR sources are excluded. Bug fixes require a regression test.

## Commit & Pull Request Guidelines

Git history is not included in this checkout. Use short, imperative subjects, for example `generator: handle nested keyword fields`. Pull requests should explain the motivation, list affected modules, include verification results, and link issues. Call out generated-code or dependency changes.
