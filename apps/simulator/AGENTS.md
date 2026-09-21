# Simulator Development Guide

Inherit the repository rules from `../../AGENTS.md`.

## Boundaries

- Deserialize `SimulationRequestMessage` and serialize `SimulationResultMessage` from `:messaging-contract` only in infrastructure adapters.
- Keep simulation results used by the use case independent from the SQS result message.
- Preserve delete-after-send semantics: delete a request only after every result message is sent successfully.
- Keep queue names configurable through `SIMULATION_REQUEST_QUEUE_NAME` and `SIMULATION_RESULT_QUEUE_NAME`.
- Do not depend on backend classes. The `domain` module is pure simulation logic;
  it must not import Spring, SQS, HTTP, Thymeleaf, or database technologies.
- `application` coordinates simulation use cases and internal contracts;
  `infrastructure` owns Spring configuration, SQS polling, serialization, and
  message-to-domain mapping. Infrastructure depends inward and adapters do not
  leak transport types into domain behavior.
- Keep supplied randomness explicit. For any feature with a seed, the same seed
  and input must yield the same result; do not add time-, thread-, or global-state
  entropy. The current unseeded `Math.random()` path is not a reproducibility
  guarantee and must only be redesigned under an explicit feature specification.

## Graph ownership

- `domain-worker` owns assigned files in `domain` and, when explicitly stated,
  simulator `application`; it does not edit `infrastructure` or backend files.
- Simulator SQS and Spring adapter changes are a separately assigned integration
  scope. Shared wire-contract changes are an upstream node owned explicitly by
  the parent, not an excuse to copy DTOs into this application.

## Tests and coverage

- Every Java test method has a Japanese `@DisplayName` and `// given`, `// when`, `// then` sections.
- Wrap assertions in `assertAll`, including a single assertion.
- Put `assertThrows` in the `// when` section and inspect the exception in `// then`.
- Exercise both outcomes of each changed boolean condition; do not weaken or suppress coverage checks.
- Run `../../.agents/skills/baseball-orders-development/scripts/verify.sh simulator` before completion.
- Focused commands may use `./gradlew :domain:test`, `./gradlew
  :application:test`, or `./gradlew :infrastructure:test` from `apps/simulator`.

ElasticMQ integration tests run only when `ELASTICMQ_ENDPOINT_URL` is set.
