# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-noble AS build
ARG APP
COPY apps/${APP} /workspace/apps/${APP}
COPY libs /workspace/libs
WORKDIR /workspace/apps/${APP}
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    sh ./gradlew --no-daemon --console=plain :infrastructure:bootJar

FROM eclipse-temurin:25-jre-noble
ARG APP
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/apps/${APP}/infrastructure/build/libs/*-SNAPSHOT.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
