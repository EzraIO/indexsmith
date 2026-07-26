# IndexSmith for Elasticsearch

> Type-safe Java repositories and code generation for Elasticsearch 7/8

IndexSmith turns Elasticsearch mappings into typed Java models, query
requests, and repositories. Its Java 8 runtime detects `GET /` server
metadata, selects an installed ES7 or ES8 adapter through Java SPI, and keeps
repository APIs independent of client-library changes. It never downloads a
client at runtime.

## Release status

> [!IMPORTANT]
> IndexSmith is pre-release and has not been published to Maven Central.
> The `0.1.0` coordinates below describe the planned first release and only
> resolve after building this repository with `./mvnw clean install`. Use the
> source quickstart for evaluation; do not add these coordinates to production
> builds until a GitHub release and Maven Central link are published here.

## 60-second quickstart

With Docker running, start a real Elasticsearch 8.17 node and the Java 8 demo:

```shell
docker compose -f examples/quickstart/compose.yaml up --build
```

Then query through the typed repository:

```shell
curl --noproxy "*" "http://127.0.0.1:18080/products?name=city%20bike"
```

The response shows the detected server version, selected `es8` adapter,
generated JSON DSL, and mapped Java objects. See the
[`quickstart` guide](examples/quickstart/README.md) for cleanup and port options.

## Planned 0.1 support line

- ES7 adapter: Elasticsearch 7.11–7.17, using the stable REST protocol.
- ES8 adapter: Elasticsearch 8.17+, using the official Java API Client.
- Java 8 bytecode; build and release tooling uses Maven Wrapper.

Import the BOM, then install only the adapter your application supports:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.indexsmith</groupId>
      <artifactId>indexsmith-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependency>
  <groupId>io.github.indexsmith</groupId>
  <artifactId>indexsmith-client-es8</artifactId>
</dependency>
```

Gradle uses the same Maven coordinates:

```kotlin
implementation(platform("io.github.indexsmith:indexsmith-bom:0.1.0"))
implementation("io.github.indexsmith:indexsmith-api")
implementation("io.github.indexsmith:indexsmith-client-es8")
```

## Generated repositories and runtime clients

For each configured Elasticsearch index, the generator creates a typed model,
query request, field metadata, and repository. The generated repository extends
`BaseElasticsearchRepository<T>` from `indexsmith-api`; it does not duplicate
the common Elasticsearch operations. The base repository provides single and
bulk indexing, deletion by ID, search execution, response mapping, and client
lifecycle handling. Generated code supplies the index name and builds typed
query JSON from the mapping-specific request model.

At runtime, the repository delegates requests to the version-neutral
`IndexSmithClient` contract from `indexsmith-client-core`. Client discovery then
selects the installed ES7 or ES8 adapter through Java SPI:

```text
Generated repository
    -> api.repository.BaseElasticsearchRepository<T>
        -> IndexSmithClient (indexsmith-client-core)
            -> ES7 or ES8 adapter
```

The public API is grouped by responsibility under `io.github.indexsmith.api`:
`repository` contains repository and hit types, `query` contains the query DSL,
`aggregation` contains aggregation requests and results, and `result` contains
operation responses. Supporting contracts live in `policy`, `exception`, and
`json`.

The client API follows the same boundary: stable entry points such as
`IndexSmithClient`, `IndexSmithClientFactory`, and `ClientConfiguration` remain
under `io.github.indexsmith.client`; request and response models live in
`request` and `response`, policies in `policy`, lifecycle hooks in
`observation`, adapter extension points in `spi`, and reusable HTTP transport
code in `transport`.

`insert` creates a document when no ID is supplied. With an ID, it uses
`PUT /{index}/_doc/{id}`, which creates or replaces the complete document.
This is not a partial Elasticsearch `_update`; callers that need partial-update
semantics should use an explicit client request.

Basic Auth, API Key authentication, custom `SSLContext`, fail-closed query
policies, redacted lifecycle listeners, and idempotency-aware failover are
available through `ClientConfiguration`. Runnable Java 8, Spring Boot 2.7,
and Gradle consumers live under [`examples/`](examples/); both runnable Java
examples call representative generated repositories.

The Maven plugin (`indexsmith-maven-plugin:generate`) reads
`indexsmith-generator.xml`, discovers mappings, and writes typed
repositories to `target/generated-sources/indexsmith`. Generated
queries use the same version-aware client factory. See
[`docs/client-adapters.md`](docs/client-adapters.md) and
[`docs/generator.md`](docs/generator.md) for configuration and query examples.

## Development

```shell
./mvnw clean verify
./mvnw verify -Pintegration-tests       # Docker/Testcontainers
./mvnw -Prelease -Dgpg.skip=true clean verify  # local release dry-run
```

`indexsmith-utils` and `indexsmith-sql` are experimental in 0.1.x and are not part
of the Central publication bundle. See [CONTRIBUTING.md](CONTRIBUTING.md),
[SECURITY.md](SECURITY.md), [RELEASING.md](RELEASING.md), and the
[SQL module boundary](docs/sql.md).

## License

Apache-2.0. Elasticsearch and Elastic are trademarks of Elasticsearch B.V.;
this project is independent and carries no Elastic endorsement.
