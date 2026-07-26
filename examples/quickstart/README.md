# IndexSmith Quickstart

This demo runs Elasticsearch 8.17 and a Java 8/Spring Boot 2.7 application.
The application detects the server version, selects the ES8 adapter through
Java SPI, creates an isolated `indexsmith-demo-products` index, writes sample
documents, and queries them through a generated-style typed repository. Startup
never deletes an existing index.

The POM pins the REST Client to 8.17.4 and Jakarta JSON to 2.x because Spring
Boot 2.7 otherwise selects versions that are incompatible with the Elasticsearch
8 Java client.

## Run

From the repository root:

```shell
docker compose -f examples/quickstart/compose.yaml up --build
```

After the application reports `Started QuickstartApplication`, open another
terminal:

```shell
curl --noproxy "*" "http://127.0.0.1:18080/products?name=city%20bike"
```

The response includes the detected Elasticsearch version, selected adapter,
generated JSON DSL, total hits, and typed product documents. List every sample
with `curl --noproxy "*" http://127.0.0.1:18080/products`.

Stop and remove the isolated demo containers with:

```shell
docker compose -f examples/quickstart/compose.yaml down --volumes
```

Set `INDEXSMITH_DEMO_PORT` before startup when port `18080` is already in use.
The checked-in classes under `example.quickstart.generated` represent generator
output so that the demo can start without requiring a live cluster at build
time.
