# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17@sha256:f58d59b6273e785ac0a4477f6e9b5ba1d7731c75b906c0f7b34076f1851318cc AS build

WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress \
    -pl indexsmith-bom,indexsmith-client-es8,indexsmith-api -am install -DskipTests
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress -f examples/quickstart/pom.xml package

FROM eclipse-temurin:8-jre-jammy@sha256:2dd44832214038e70ecda102295e1099012a2a665fe5c81167c3eb7782085fed

RUN groupadd --system indexsmith \
    && useradd --system --gid indexsmith --uid 10001 indexsmith
WORKDIR /app
COPY --from=build --chown=indexsmith:indexsmith \
    /workspace/examples/quickstart/target/indexsmith-quickstart.jar app.jar

USER indexsmith:indexsmith
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
