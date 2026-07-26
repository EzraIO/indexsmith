# IndexSmith for Elasticsearch

> 面向 Elasticsearch 7/8 的类型安全 Java 仓储与代码生成工具链

IndexSmith 将 Elasticsearch mapping 生成为强类型 Java 模型、查询请求与仓储。
Java 8 运行时读取 `GET /` 的 `version.number`，通过 Java SPI 在已安装的
ES7/ES8 适配器中动态选择，查询和仓储 API 不绑定具体客户端版本，也不会在
运行时下载依赖。

## 60 秒快速体验

本机启动 Docker 后，用一条命令运行 Elasticsearch 8.17 和 Java 8 Demo：

```shell
docker compose -f examples/quickstart/compose.yaml up --build
```

随后通过类型安全仓储查询：

```shell
curl --noproxy "*" "http://127.0.0.1:18080/products?name=city%20bike"
```

响应会展示探测到的 ES 版本、自动选择的 `es8` 适配器、生成的 JSON DSL 和映射后的
Java 对象。停止方式及端口配置见 [`quickstart` 文档](examples/quickstart/README.md)。

ES7 适配器支持 7.11–7.17，ES8 适配器支持 8.17+。Maven 通过 BOM 统一版本：

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
```

Gradle 使用同一 BOM：

```kotlin
implementation(platform("io.github.indexsmith:indexsmith-bom:0.1.0"))
implementation("io.github.indexsmith:indexsmith-api")
implementation("io.github.indexsmith:indexsmith-client-es8")
```

客户端支持 Basic Auth、API Key、自定义 `SSLContext`、可选查询策略、脱敏请求监听和
幂等感知故障切换。`examples/` 包含纯 Java 8、Spring Boot 2.7 和 Gradle 消费示例；
两个可运行 Java 示例均直接使用生成器风格的仓储代码。

Maven 插件 `indexsmith-maven-plugin:generate` 读取
`indexsmith-generator.xml`，发现 mapping 并将类型安全的仓储生成到
`target/generated-sources/indexsmith`。生成代码使用同一套版本选择逻辑。
详见 [`docs/client-adapters.md`](docs/client-adapters.md)。

开发命令：

```shell
./mvnw clean verify
./mvnw verify -Pintegration-tests
```

`indexsmith-utils` 与 `indexsmith-sql` 在 0.1.x 中属于实验模块，不进入 BOM 或 Central 发布包；
SQL 模块只承诺解析和 AST，不承诺 SQL 执行。
项目采用 Apache-2.0 许可证，与 Elastic 无隶属或背书关系。
