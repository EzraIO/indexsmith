# Repository generation

Import the BOM, add the public API and one runtime adapter, then configure the
Maven plugin. Copy
[`examples/indexsmith-generator.xml`](../examples/indexsmith-generator.xml)
to the consumer project root as `indexsmith-generator.xml`:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.ezraio</groupId>
      <artifactId>indexsmith-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.github.ezraio</groupId>
    <artifactId>indexsmith-api</artifactId>
  </dependency>
  <dependency>
    <groupId>io.github.ezraio</groupId>
    <artifactId>indexsmith-client-es8</artifactId>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>io.github.ezraio</groupId>
      <artifactId>indexsmith-maven-plugin</artifactId>
      <version>0.1.0</version>
      <executions>
        <execution>
          <goals><goal>generate</goal></goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

A minimal `indexsmith-generator.xml` keeps credentials in the environment and
generated files inside the Maven project:

```xml
<generatorConfiguration>
  <sourceDir>target/generated-sources/indexsmith</sourceDir>
  <writeMode>FAIL</writeMode>
  <targetPackage>
    <model>com.example.search.model</model>
    <fieldEnum>com.example.search.field</fieldEnum>
    <esQueryModel>com.example.search.request</esQueryModel>
    <client>com.example.search.client</client>
  </targetPackage>
  <clusters>
    <cluster>
      <connection>
        <hostPorts>${env.ELASTICSEARCH_HOSTS}</hostPorts>
        <apiKeyId>${env.ELASTICSEARCH_API_KEY_ID}</apiKeyId>
        <apiKeySecret>${env.ELASTICSEARCH_API_KEY_SECRET}</apiKeySecret>
      </connection>
      <indexes>
        <index><name>orders</name></index>
      </indexes>
    </cluster>
  </clusters>
</generatorConfiguration>
```

Run `./mvnw generate-sources` or any lifecycle phase that includes it. The
plugin detects the server version through the same SPI factory as generated
repositories, then requests mappings and writes model, field-enum, request,
and client sources. Install `indexsmith-client-es7` and/or
`indexsmith-client-es8` in the plugin runtime as needed; the published
plugin includes both adapters.

Use `${env.NAME}` placeholders for credentials and host lists. Configure
either `userName`/`passWord` or `apiKeyId`/`apiKeySecret`, never both. HTTPS
uses the Maven JVM trust store; configure a private CA with standard JVM
trust-store options rather than placing certificates or passwords in XML. Output paths
must be relative to the Maven project. `FAIL` is the safe default: changed
files stop the build until the author explicitly selects `SKIP` or
`OVERWRITE`. Generated output is deterministic and identical files are left
untouched.

Mapping regression coverage uses versioned JSON snapshots and compiles the
complete emitted source set. Generated models and requests use ordinary
JavaBean accessors, so consumers do not need Lombok. CI compiles
`examples/generated-consumer` against only the BOM and public modules, plus
the plain Java, Spring Boot 2.7, and Gradle consumer examples.
