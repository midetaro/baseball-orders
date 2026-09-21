# Codex Graph engineering guide

## Purpose

This repository uses a dependency graph for features that cross application or
module boundaries. The graph keeps investigation, implementation, integration,
review, and final verification distinct so that parallel work does not violate the
existing architecture or overwrite another worker's files.

The graph is coordination guidance, not permission to redesign modules. Existing
Gradle builds and source code are authoritative when this document and an older
design document differ.

## Repository facts used by the graph

- The root Gradle build is a composite. `settings.gradle` includes the independent
  `apps/backend` and `apps/simulator` builds and the root `integration-test`
  project.
- Both applications are Gradle multi-project builds with `domain`, `application`,
  and `infrastructure`. Both include the same physical
  `libs/messaging-contract` directory as `:messaging-contract`.
- Backend dependencies are `application -> domain` and `infrastructure ->
  application, domain, messaging-contract`.
- Simulator dependencies are `application -> domain, messaging-contract` and
  `infrastructure -> application`. Simulator infrastructure has a test-only
  dependency on domain.
- Backend owns synchronous HTTP, the result-waiting application flow, SQS
  publishing/listening, and Thymeleaf presentation. Templates live under
  `apps/backend/infrastructure/src/main/resources/templates`; the empty
  `apps/web` directory is not a module.
- Simulator owns the simulation use case and domain rules. SQS mapping and polling
  live in simulator infrastructure.
- `libs/messaging-contract` contains only SQS wire messages. `integration-test`
  verifies the composed applications.
- The current simulator randomness uses `Math.random()` and accepts no seed. It is
  not currently possible to prove same-seed reproducibility. A feature that adds a
  seed must make the random source explicit and test same seed + same input => same
  result; this setup task does not change application behavior.

## Agent roles

| Agent | Mode | Responsibility | Must not do |
| --- | --- | --- | --- |
| `explorer` | read-only | Trace execution paths, dependencies, tests, candidate files, ownership conflicts, and true graph dependencies. | Edit files or turn assumptions into architecture. |
| `domain-worker` | write | Implement assigned simulator domain and explicitly assigned simulator application behavior as pure inward-facing Java, with narrow tests. | Edit infrastructure/backend, use Spring/SQS/Thymeleaf/DB in domain, or change another node's files. |
| `backend-worker` | write | Implement assigned backend domain/application and non-Thymeleaf adapters within existing backend responsibilities. | Depend on simulator classes, implement simulation rules, or conflate wire DTOs and internal models. |
| `ui-worker` | write | Implement assigned backend web controllers, forms/view models, Thymeleaf templates, browser-facing behavior, and presentation tests. | Expose entities/domain objects directly to templates or put business rules in controllers/templates. |
| `integrator` | write | Start after required workers, connect their outputs, own explicitly assigned integration wiring/tests, and classify failures. | Invent business rules or silently fix an unassigned area. |
| `reviewer` | read-only, fresh context | Review specification, final diff, and test evidence for correctness, architecture, regression, and missing tests. | Modify implementation or rely on worker chat history. |

All six roles are retained because each has a non-overlapping responsibility in
the requested cross-module workflow. For a small change, unused workers are not
spawned.

## Standard execution graph

```text
explorer
   |
   v
parent: acceptance criteria + dependency graph + exclusive file ownership
   |
   +--> optional shared-contract node (single owner, completed first)
   |          |
   |          v
   +--> domain-worker --------+
   +--> backend-worker -------+--> integrator --> reviewer (fresh context)
   +--> ui-worker ------------+                       |
                                                     v
                                      parent fixes blocking findings
                                                     |
                                                     v
                                           full verification
```

Arrows represent real output dependencies, not an arbitrary sequence. When, for
example, a UI change consumes a new backend application result shape, the UI node
waits for that shape or both changes stay in one node. When their inputs and files
are stable and disjoint, domain, backend, and UI workers can run concurrently.

The parent must wait for every spawned agent. `blocked`, `failed`, or incomplete
is not equivalent to completion. Integrator starts only after all nodes it needs
have completed. Reviewer starts only after integration and receives a fresh
context containing the feature specification path, final diff, and test results.

## Deciding whether nodes are independent

Two nodes may run concurrently only when all of the following are true:

- neither consumes a type, schema, or behavior that the other is changing;
- their writable file sets do not overlap;
- neither test mutates shared generated output or a shared service in a conflicting
  way;
- both can validate meaningful acceptance criteria without waiting for the other.

Parallelization is useful when it reduces elapsed time or isolates noisy context.
Prefer read-heavy exploration, test execution, and review. Do not parallelize just
because concurrency is available; tightly coupled code changes create integration
cost and file conflicts.

Every writing-agent prompt must list exclusive writable paths. Do not assign the
same file to two live agents. A shared contract change has one owner and is an
upstream node; dependent application workers wait for it. The repository has no
separate contract worker because contract changes are infrequent and must be
planned centrally across both applications.

## File ownership

| Area | Normal owner | Paths |
| --- | --- | --- |
| Exploration | `explorer` (read-only) | Entire repository |
| Simulator domain | `domain-worker` | `apps/simulator/domain/**` |
| Simulator use case, when assigned | `domain-worker` | `apps/simulator/application/**` |
| Backend domain/application/API/SQS | `backend-worker` | `apps/backend/domain/**`, `apps/backend/application/**`, assigned non-web files below `apps/backend/infrastructure/**` |
| Backend presentation | `ui-worker` | `apps/backend/infrastructure/**/web/**`, `apps/backend/infrastructure/src/main/resources/templates/**`, explicitly assigned form/view-model and tests |
| Shared wire contract | parent-assigned single upstream owner | `libs/messaging-contract/**` |
| Cross-application integration | `integrator` | `integration-test/**` and explicitly assigned wiring/test files |
| Terraform/container support | separately assigned specialist scope | `infra/**` |

The table is a default. The parent narrows it to concrete files for each feature.
An agent stops instead of editing outside its allocation.

## Module responsibilities and forbidden dependencies

### Simulator

- `domain`: game state, players, strategies, play results, and statistics. It may
  use its declared general-purpose libraries but cannot reference Spring, SQS,
  Thymeleaf, HTTP, DB, backend, or messaging-contract types.
- `application`: simulation use-case coordination and internal response contracts.
  It depends inward on domain and currently sees the shared messaging contract;
  do not expand transport coupling into domain.
- `infrastructure`: Spring bootstrapping, SQS client/polling, serialization, and
  wire-to-internal mapping. It depends on application and must not place business
  rules in listeners or mappers.
- Determinism: features that accept a seed pass one explicit random source through
  the simulation path. The same seed and identical ordered input must have a
  deterministic test asserting an identical result. Do not use wall-clock time,
  thread identity, unordered iteration, or an additional global RNG as entropy.

### Backend

- `domain`: framework-independent player and result data. It must not reference
  Spring, HTTP, SQS, Thymeleaf, persistence, or simulator code.
- `application`: request coordination, result correlation/waiting, application
  DTOs, exceptions, and outbound ports. It depends on backend domain, not adapters.
- `infrastructure/api`: HTTP request/response and error translation.
- `infrastructure/web` plus templates: server-rendered presentation.
- `infrastructure/messaging`: mapping and SQS adapters. Shared messages remain wire
  DTOs and are mapped to backend internal models.
- `InfrastructureConfiguration`: composition root only.
- Backend never directly depends on simulator classes and never implements
  simulation algorithms. Controllers and listeners delegate rather than applying
  business rules.

## Worker loop and report

Each writing worker runs at most five iterations:

1. implement the smallest assigned behavior;
2. run the owning module's focused test;
3. identify the failure cause;
4. fix the single most fundamental cause within scope;
5. rerun the test.

Stop when tests and acceptance criteria pass, iteration five is reached, a change
outside the assigned scope is required, or ambiguity requires a material product
or architecture decision. Report exactly this shape:

```text
## Result

- Status: completed | blocked | failed
- Files changed:
- Tests executed:
- Test result:
- Decisions:
- Remaining risks:
- Required follow-up:
```

## Test commands by node

Run commands from the paths stated below. These tasks are present in the current
Gradle builds.

| Node | Focused command |
| --- | --- |
| Simulator domain | `cd apps/simulator && ./gradlew :domain:test` |
| Simulator application | `cd apps/simulator && ./gradlew :application:test` |
| Simulator infrastructure | `cd apps/simulator && ./gradlew :infrastructure:test` |
| Backend domain | `cd apps/backend && ./gradlew :domain:test` |
| Backend application | `cd apps/backend && ./gradlew :application:test` |
| Backend infrastructure/UI | `cd apps/backend && ./gradlew :infrastructure:test` |
| Shared contract in backend build | `cd apps/backend && ./gradlew :messaging-contract:test` |
| Shared contract in simulator build | `cd apps/simulator && ./gradlew :messaging-contract:test` |
| Root integration tests | `./gradlew :integration-test:test` |

Owning-application completion uses the canonical commands:

```text
./.agents/skills/baseball-orders-development/scripts/verify.sh backend
./.agents/skills/baseball-orders-development/scripts/verify.sh simulator
```

For a shared contract or cross-application feature, use:

```text
./.agents/skills/baseball-orders-development/scripts/verify.sh all
./gradlew :integration-test:test
```

ElasticMQ-dependent backend and simulator tests are conditional on `SQS_ENDPOINT`
and `ELASTICMQ_ENDPOINT_URL` respectively. Report them as skipped when the matching
variable is absent. Never hide a test failure.

## Integration and review

Integrator receives completed worker summaries and the current diff. It connects
only agreed interfaces, adds or updates integration tests, and classifies failures
as `domain`, `contract`, `backend`, `presentation`, or `integration`. An issue
outside its ownership returns to the parent; it is not repaired silently.

Reviewer runs in a new context, read-only. Give it the specification, final diff,
and exact test commands/results. Findings include severity, evidence, and file
location. It distinguishes deterministic verification, test coverage, manual
review, and unverified behavior. A no-findings report still lists what was checked.
Blocking findings return to a single assigned worker or the parent, after which
affected tests and review are repeated before full verification.

## When not to use the graph

Keep work in one parent context when it is a small documentation-only change, a
single-file mechanical correction, or a localized behavior change with one test
and no cross-module dependency. Do not spawn agents whose startup, duplicated
reading, token consumption, and merge coordination exceed the expected work.

Subagents consume separate context and tokens. Give them bounded inputs and ask for
summaries, not raw logs. Parallel writes increase conflict risk, so maximize
read-only parallelism first and keep file ownership disjoint.

## Feature-specification example

Copy `docs/features/TEMPLATE.md` to a feature-specific file, set `Status: todo`,
and write deterministic acceptance criteria. Example graph:

```text
explorer: trace request, message, simulator, result, and UI paths
parent contract node: update one shared message and its tests
domain-worker: consume the agreed internal input and implement domain behavior
backend-worker: map backend request/result without simulator class dependencies
ui-worker: render the already-agreed backend view model
integrator: prove backend -> SQS -> simulator -> SQS -> backend
reviewer: check the final diff and evidence in a fresh read-only context
parent: fix findings and run full verification
```

After acceptance criteria, deterministic checks, integration, review, and full
verification pass with no blocking finding, change the feature status to `done`.

## Prompt for Codex

```text
docs/features/<feature>.mdを実装してください。

AGENTS.mdに従い、最初にexplorerで影響範囲を調査してください。
調査結果から依存Graphを作り、独立したNodeだけを並列実行してください。
各workerの完了後にintegratorを実行し、最後に新しいコンテキストのreviewerで検証してください。
同一ファイルを複数agentへ同時編集させず、すべての結果を待ってから最終報告してください。
```

The parent also preserves uncommitted changes, avoids unrelated files, does not
add/remove/move modules, does not change dependency direction or add external
libraries, and does not commit, push, or create branches unless the user makes a
separate explicit request.
