# Changelog

## 0.1.0 - Unreleased

- Added version-aware ES7/ES8 adapters with server discovery and SPI loading.
- Added Java 8-compatible repository/query APIs and mapping-based generation.
- Added the `indexsmith-maven-plugin:generate` goal.
- Added reproducible Maven release metadata, coverage gates, legal notices,
  and CI/release governance.
- Added the supported-module BOM, API Key and custom TLS configuration,
  idempotency-aware failover, redacted request listeners, and optional query
  policy SPI with a JSON-aware restricted policy.
- Added fail-closed validation for body-routed multi-index APIs and URL
  pagination, retained HTTP status/retry details on search failures, and
  removed query strings from listener events.
- Generated models and requests now use JavaBean accessors without Lombok;
  mapping snapshots cover date fields and consumer compilation. Generator API
  Key fields support the same environment interpolation as Basic Auth.
- Added ES 7.11/7.17/8.17 compatibility matrices, Java 8 consumer examples,
  API compatibility checks, CodeQL, OpenSSF Scorecard, and CycloneDX SBOMs.
- Release audits now cryptographically verify all JAR, POM, SBOM, and
  buildinfo signatures before and after Central upload.
- Marked `indexsmith-utils` and `indexsmith-sql` experimental; they are not included
  in the 0.1.x Central publication bundle.
- The 0.1.0 coordinates and Java packages use the new
  `io.github.indexsmith` namespace; existing consumers of
  the pre-release `com.easy.elasticsearch` layout must update imports and
  artifact coordinates.

Release notes will document compatibility changes, dependency changes, and
generated-code changes for each subsequent version.
