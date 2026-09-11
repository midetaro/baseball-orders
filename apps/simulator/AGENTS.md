# Simulator Development Guide

Inherit the repository rules from `../../AGENTS.md`.

## Boundaries

- Deserialize `SimulationRequestMessage` and serialize `SimulationResultMessage` from `:messaging-contract` only in infrastructure adapters.
- Keep simulation results used by the use case independent from the SQS result message.
- Preserve delete-after-send semantics: delete a request only after every result message is sent successfully.
- Keep queue names configurable through `SIMULATION_REQUEST_QUEUE_NAME` and `SIMULATION_RESULT_QUEUE_NAME`.

## Tests and coverage

- Every Java test method has a Japanese `@DisplayName` and `// given`, `// when`, `// then` sections.
- Wrap assertions in `assertAll`, including a single assertion.
- Put `assertThrows` in the `// when` section and inspect the exception in `// then`.
- Exercise both outcomes of each changed boolean condition; do not weaken or suppress coverage checks.
- Run `../../.agents/skills/baseball-orders-development/scripts/verify.sh simulator` before completion.

ElasticMQ integration tests run only when `ELASTICMQ_ENDPOINT_URL` is set.
