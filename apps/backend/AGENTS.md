# Backend Development Guide

Inherit the repository rules from `../../AGENTS.md`.

## Boundaries

- Keep HTTP and SQS adapters in `infrastructure`, coordination in `application`, and business data in `domain`.
- Publish and consume only the types from `:messaging-contract` at the SQS boundary.
- Preserve the synchronous HTTP-to-asynchronous-SQS correlation through `WaitingResultRegistry`.
- Keep queue names configurable through `SIMULATION_REQUEST_QUEUE_NAME` and `SIMULATION_RESULT_QUEUE_NAME`.
- Do not depend on `apps/simulator` classes or reproduce simulator rules here.
- Keep SQS DTOs from `:messaging-contract`, backend domain models, application
  DTOs, request/form objects, view models, and any persistence entities distinct.

## Class design

- `domain` contains framework-independent business data and result models. Do not put HTTP, SQS, JPA, or Spring types in this module.
- `application` contains use-case coordination, request-scoped or in-memory application state, application DTOs, and ports. Define ports here when a use case needs an external capability; implementations belong in `infrastructure`.
- `infrastructure` contains adapters for external I/O: `api` for HTTP APIs, `web` for server-rendered pages, `messaging` for SQS, and `persistence` for JPA. `InfrastructureConfiguration` composes application services with adapter implementations.
- An `infrastructure` adapter must not directly reference another `infrastructure` adapter's implementation class, entity, repository, or framework client. In particular, `api`, `web`, `messaging`, and `persistence` must communicate through an `application` use case or port, not through each other.
- `InfrastructureConfiguration` is the only exception to the adapter-to-adapter restriction: it may reference application types and adapter implementations solely to compose Spring beans.
- When repairing an existing direct adapter reference, migrate it to an `application` port or use case; do not introduce additional violations.
- `infrastructure/api` owns JSON/HTTP request and error translation;
  `infrastructure/web` and `src/main/resources/templates` own the Thymeleaf
  presentation. Controllers validate/translate and delegate; they do not contain
  simulation or other business rules, and templates receive presentation-shaped
  data rather than domain objects or entities.

## Graph ownership

- `backend-worker` owns backend `domain`, `application`, and non-Thymeleaf
  infrastructure files assigned by the parent.
- `ui-worker` owns `infrastructure/web`, presentation request/form/view-model
  types explicitly assigned to it, Thymeleaf templates, and their tests.
- Never edit a file assigned to the other worker concurrently. Contract changes in
  `../../libs/messaging-contract` and cross-application integration tests remain a
  separate upstream or integration node owned explicitly by the parent.

## Tests

- Every Java test method has a Japanese `@DisplayName` and `// given`, `// when`, `// then` sections.
- Wrap assertions in `assertAll`, including a single assertion.
- Put `assertThrows` in the `// when` section and inspect the exception in `// then`.
- Comment what a mock represents when its role is not obvious from its name.
- Run `../../.agents/skills/baseball-orders-development/scripts/verify.sh backend` before completion.
- Focused commands may use `./gradlew :domain:test`, `./gradlew
  :application:test`, or `./gradlew :infrastructure:test` from `apps/backend`.

ElasticMQ integration tests run only when `SQS_ENDPOINT` is set.
