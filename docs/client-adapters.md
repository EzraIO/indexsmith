# Elasticsearch Client Adapters

The client layer separates the public API from version-specific Elasticsearch dependencies:

- `indexsmith-client-core` contains configuration, version detection, requests, responses, and the SPI factory.
- `indexsmith-client-es7` supports Elasticsearch 7.11–7.17 through the stable REST protocol, avoiding an obsolete HLRC dependency.
- `indexsmith-client-es8` currently supports Elasticsearch 8.17+ through the 8.17 official Java API Client.

Select an adapter at build time. At startup, `IndexSmithClientFactory`
calls `GET /`, reads `version.number`, and verifies that an installed adapter
supports the server. When multiple endpoints are configured, detection and
both adapters fail over across unavailable nodes. It never downloads
dependencies at runtime.

## Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.outakeshi</groupId>
      <artifactId>indexsmith-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependency>
  <groupId>io.github.outakeshi</groupId>
  <artifactId>indexsmith-client-es8</artifactId>
</dependency>
```

Use `indexsmith-client-es7` instead for a 7.11–7.17 cluster.

## Gradle

```kotlin
repositories {
    mavenLocal() // after running ./mvnw install for a local snapshot
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.outakeshi:indexsmith-bom:0.1.0"))
    implementation("io.github.outakeshi:indexsmith-client-es8")
}
```

Gradle can consume the published JARs from any Maven-compatible repository. Use `mavenLocal()` only for development snapshots; use the release repository in production. The `indexsmith-maven-plugin` remains Maven-specific; Gradle builds can invoke the shared `indexsmith-generator` module directly or consume generated sources.

## Usage

```java
ClientConfiguration configuration = ClientConfiguration.builder()
        .endpoint("http://localhost:9200")
        .credentials("elastic", "password")
        .build();

try (IndexSmithClient client =
             new IndexSmithClientFactory().create(configuration)) {
    SearchResult result = client.search(new SearchRequest(
            "products", "{\"query\":{\"match_all\":{}}}"));
    System.out.println(result.getResponseBody());
}
```

Normally install only one adapter. If both are present, selection is based on the detected server major version. Set `.adapter("es8")` to require a specific adapter and fail fast on a mismatch.

## Authentication, TLS, and request controls

API Key authentication is mutually exclusive with Basic Auth. A custom
`SSLContext` changes certificate trust/key material but never disables JVM
hostname verification:

```java
ClientConfiguration configuration = ClientConfiguration.builder()
        .endpoint("https://search.example.com:9200")
        .apiKey(System.getenv("ES_API_KEY_ID"), System.getenv("ES_API_KEY_SECRET"))
        .sslContext(applicationSslContext)
        .queryPolicy(RestrictedQueryPolicy.builder()
                .allowIndex("products")
                .allowMethod("POST")
                .maxResultWindow(1000)
                .forbidPathPrefix("/_scripts")
                .build())
        .requestListener(event -> metrics.record(event.getPhase(), event.getDurationMillis()))
        .build();
```

Listeners receive adapter, method, query-string-free path, status, duration,
retry count and failure type; they never receive credentials or payloads. Listener failures
are ignored. Policies reject requests before transport and are disabled unless
configured. Automatic failover only replays safe methods or requests explicitly
marked retryable; Bulk/POST writes are not replayed implicitly.

When an index allow-list is active, global index APIs and body-routed Bulk,
MSearch, MGet, and MTermVectors requests fail closed because their target index
cannot be proven from the URL. `maxResultWindow` validates JSON and URL
`from`/`size` together and rejects MSearch, whose NDJSON windows cannot be
verified by the built-in policy. Use a custom `QueryPolicy` when an application
needs more specialized validation.

## Generated repositories

Generated clients use the same runtime selection. They accept either a configuration or an already-created client:

```java
ClientConfiguration configuration = ClientConfiguration.builder()
        .endpoint("http://localhost:9200")
        .build();

try (OrdersClient orders = new OrdersClient(configuration)) {
    OrdersReq request = new OrdersReq();
    request.setStatus("paid");
    request.setItemsSkuId(42L);
    request.setPageNo(1);
    request.setPageSize(20);
    request.setSortFieldName("createdAt");
    request.setDescending(true);
    PageSearchResponse<BaseHit<OrdersModel>> result = orders.query(request);
}
```

The generated `buildQuery` method emits version-compatible JSON DSL. Keyword, numeric, and boolean fields use `term`; text fields use `match_phrase`; fields beneath a `nested` mapping use a grouped `nested` bool query. Existing scalar request fields remain the default conditions. Advanced callers can add structured bool conditions, ranges, terms, multiple sorts, `search_after`, source filtering, highlights, and typed aggregations through the inherited `PageQuery` properties.

Generated source writes default to `FAIL`: an existing file with different content is never overwritten silently. Identical output is a no-op. Set `<writeMode>SKIP</writeMode>` to preserve differing files or `<writeMode>OVERWRITE</writeMode>` for an intentional regeneration, then restore `FAIL`.

For a Gradle application consuming generated sources:

```kotlin
dependencies {
    implementation(platform("io.github.outakeshi:indexsmith-bom:0.1.0"))
    implementation("io.github.outakeshi:indexsmith-api")
    implementation("io.github.outakeshi:indexsmith-client-es8") // or client-es7
}
```

Generated models and requests contain ordinary JavaBean accessors and require
no annotation processor. The Maven generator plugin bundles both adapters so
it can inspect mappings from either supported server generation. Generated
application code only needs the adapter(s) it intends to use.

## Tests

Run unit and local Mock HTTP tests without Docker:

```shell
./mvnw clean test -pl client-core,client-es7,client-es8 -am
```

Run the optional real-cluster compatibility tests with Docker/Testcontainers:

```shell
./mvnw verify -Pintegration-tests
```

CI verifies Elasticsearch 7.11.0, 7.17.22 and 8.17.3. The integration profile
accepts `indexsmith.test.es7.image` and
`indexsmith.test.es8.image` system properties for additional images.
