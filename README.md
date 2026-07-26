# IndexSmith for Elasticsearch

> Type-safe Java repositories and code generation for Elasticsearch 7/8

IndexSmith turns Elasticsearch mappings into typed Java models, query
requests, and repositories. Its Java 8 runtime detects `GET /` server
metadata, selects an installed ES7 or ES8 adapter through Java SPI, and keeps
repository APIs independent of client-library changes. It never downloads a
client at runtime.

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

## Supported line

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
