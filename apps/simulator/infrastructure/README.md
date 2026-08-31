# Infrastructure integration test

The integration test exercises `SqsSimulationScheduler` against ElasticMQ running in Docker.

```shell
docker compose -f infrastructure/compose.yaml up -d
ELASTICMQ_ENDPOINT_URL=http://localhost:9324 ./gradlew \
  :infrastructure:test \
  --tests '*SqsSimulationSchedulerIntegrationTest'
docker compose -f infrastructure/compose.yaml down
```

When `ELASTICMQ_ENDPOINT_URL` is not set, the integration test is skipped so the regular unit-test
suite does not require Docker.
