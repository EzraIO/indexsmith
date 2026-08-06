# IndexSmith for Elasticsearch

[English](README.md) | [简体中文](README_zh-CN.md)

> 从 Elasticsearch mapping 生成类型安全的 Java 仓储，同时让业务代码摆脱
> Elasticsearch 7/8 Java 客户端差异。

IndexSmith 消除 mapping 与 Java 业务代码之间重复、易错的胶水层：构建时生成
Java 模型、搜索请求和仓储；运行时通过一套稳定 API 检测服务端版本，并利用
Java SPI 选择已安装的 ES7 或 ES8 适配器。它不会在运行时下载客户端。

```text
Elasticsearch mapping
        │ 生成
        ▼
Java 模型 + 类型安全查询请求 + 仓储
        │ 一套与版本无关的 API
        ▼
Elasticsearch 7.11–7.17 或 Elasticsearch 8.17+
```

业务代码直接使用 mapping 字段，不再手写 JSON：

```java
ProductSearchRequest request = new ProductSearchRequest();
request.setName("city bike");
request.setCategory("bikes");

PageSearchResponse<BaseHit<Product>> result = products.query(request);
```

对于每个配置的索引，生成器都会输出普通 Java 源码，不依赖运行时代理，也不要求
业务项目使用 Lombok：

| 生成类型 | 提供的能力 |
| --- | --- |
| Model | 从 mapping 派生的 JavaBean 字段 |
| 字段枚举 | 用于查询组合的编译期字段名 |
| 搜索请求 | 类型安全的过滤、分页、排序、高亮与聚合参数 |
| Repository | 建立在公共 CRUD 与响应映射之上的索引专属查询 |
| 客户端门面 | 与 ES 客户端版本无关的创建与生命周期管理 |

## 为什么用 IndexSmith？

| 不使用 IndexSmith | 使用 IndexSmith |
| --- | --- |
| 手工同步 mapping 与 Java 模型 | 从 mapping 生成 JavaBean |
| 为每个索引重复 CRUD 和响应转换 | 复用经过测试的仓储操作 |
| 业务代码绑定某一代 ES 客户端 | 面向稳定的 `IndexSmithClient` 编程 |
| ES7 升级 ES8 时重写客户端代码 | 启动时自动选择兼容适配器 |
| 生成代码被意外覆盖 | 输出变化时默认拒绝覆盖 |

它适合维护 mapping 驱动仓储、仍需支持 Java 8，或正在逐步从 ES7 迁移到 ES8
的团队。它不是 ORM，也不试图替代 Elasticsearch 完整 Query DSL；高级操作仍可
通过显式客户端请求完成。

## 一条命令快速体验

本机启动 Docker 后，用一条命令运行 Elasticsearch 8.17 和 Java 8 Demo：

```shell
docker compose -f examples/quickstart/compose.yaml up --build
```

随后通过类型安全仓储查询：

```shell
curl --noproxy "*" "http://127.0.0.1:18080/products?name=city%20bike"
```

精简后的响应如下，它把类型安全请求、最终 Elasticsearch 查询和映射结果联系在一起：

```json
{
  "adapter": "es8",
  "serverVersion": "8.17.3",
  "generatedQuery": { "query": { "bool": { "must": [
    { "match_phrase": { "name": "city bike" } }
  ] } } },
  "totalHits": 1,
  "hits": [{ "id": "1", "data": { "name": "City Bike", "category": "bikes" } }]
}
```

完整响应还包含完整的生成查询和商品字段。停止方式及端口配置见
[`quickstart` 文档](examples/quickstart/README.md)。

此 Demo 不要求本机安装 Java、Maven 或 Elasticsearch，Docker 会构建并运行全部服务。

## 项目状态

IndexSmith 目前处于预发布阶段，尚未发布到 Maven Central。上面的源码 quickstart
已经可以直接体验。下文 `0.1.0` 坐标在本地执行 `./mvnw clean install` 后可用；正式用于
生产环境前，请等待本页提供 GitHub Release 与 Maven Central 链接。

| 已可通过源码验证 | 仍待 0.1.0 完成 |
| --- | --- |
| mapping 驱动的 Java 源码生成 | 发布到 Maven Central |
| 生成代码编译测试与快照测试 | 建立稳定公共 API 兼容性基线 |
| ES7 与 ES8 适配器集成测试 | 首个 GitHub Release 与升级承诺 |
| Docker 端到端 quickstart | 生产采用指南 |

## 计划中的 0.1 支持范围

ES7 适配器支持 7.11–7.17，ES8 适配器支持 8.17+。Maven 通过 BOM 统一版本：

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
```

Gradle 使用同一 BOM：

```kotlin
implementation(platform("io.github.ezraio:indexsmith-bom:0.1.0"))
implementation("io.github.ezraio:indexsmith-api")
implementation("io.github.ezraio:indexsmith-client-es8")
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
