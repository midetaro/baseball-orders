# Backend Development Guide

Inherit the repository rules from `../../AGENTS.md`.

## Boundaries

- Keep HTTP and SQS adapters in `infrastructure`, coordination in `application`, and business data in `domain`.
- Publish and consume only the types from `:messaging-contract` at the SQS boundary.
- Preserve the synchronous HTTP-to-asynchronous-SQS correlation through `WaitingResultRegistry`.
- Keep queue names configurable through `SIMULATION_REQUEST_QUEUE_NAME` and `SIMULATION_RESULT_QUEUE_NAME`.

## Tests

- Every Java test method has a Japanese `@DisplayName` and `// given`, `// when`, `// then` sections.
- Wrap assertions in `assertAll`, including a single assertion.
- Put `assertThrows` in the `// when` section and inspect the exception in `// then`.
- Comment what a mock represents when its role is not obvious from its name.
- Run `../../.agents/skills/baseball-orders-development/scripts/verify.sh backend` before completion.

ElasticMQ integration tests run only when `SQS_ENDPOINT` is set.
