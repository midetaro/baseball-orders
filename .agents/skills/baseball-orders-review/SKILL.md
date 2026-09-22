---
name: baseball-orders-review
description: Review a completed baseball-orders feature diff against its specification, architecture boundaries, integration path, and deterministic test evidence. Use after implementation and baseball-orders-test verification.
---

# Baseball Orders Review

Review the requested feature specification and the classes changed during the current session. Use `git status --short`, `git diff --stat`, and `git diff`; compare with the feature starting point and worker reports when available to separate session edits from pre-existing user changes. Limit class-level findings and focused test review to changed classes. Read unchanged callers, contracts, and tests only where needed to verify the changed classes' behavior, integration path, or regression risk. Preserve unrelated user changes.

## Evidence before judgment

Read the command results from `baseball-orders-test`. Run missing required owning-build, static, deterministic, and shared-contract checks when feasible; this required verification may cover unchanged classes. Do not rerun unrelated class-level tests. For simulator changes, invoke the ArchUnit static check directly:

```bash
(cd apps/simulator && ./gradlew --no-daemon --console=plain :infrastructure:test --tests com.example.baseballorders.simulator.infrastructure.SimulatorArchitectureTest </dev/null)
```

The ArchUnit test checks package and dependency rules in code; inspect its actual assertions before relying on it. Check the Gradle and settings diff for added, removed, or redirected project dependencies, including `project(...)`, `include(...)`, and `includeBuild(...)`. A changed module relationship is blocking. External library dependencies are allowed when required by the feature.

Verify required annotations, entry points, and message correlation with executable assertions or direct source evidence. Do not mark a check PASS based on plausibility. Mark unrun checks `NOT VERIFIED`, with the reason.

## Diff and architecture review

Compare each changed behavior with the specification's goal, expected behavior, out-of-scope list, and acceptance criteria. Inspect layering: infrastructure delegates to application, application delegates to domain, backend and simulator communicate only through the shared SQS contract, and domain has no framework or transport concerns. Check for excessive abstractions, unrelated refactors, swallowed exceptions, and behavior changes to specified timeout, duplicate, or late-message handling.

For integration tests changed during the session, compare the Japanese `実物`, `モック`, `担保する疎通`, and `担保しないもの` comments with the implementation. List every replaced dependency. A test cannot claim a path through a mocked component. Review fixed sleeps and other flaky synchronization only when they affect the changed classes.

Do not delete production code as part of review without a demonstrated finding. Send blocking findings to the owning implementer for a focused fix and rerun affected deterministic checks. Do not mark the specification done while blocking findings remain.

## Result

Report:

- Deterministic verification: command and result for build, focused and integration tests, ArchUnit, module graph, contract checks, and any `NOT VERIFIED` item.
- Integration boundary: real components, mocked components, path in `A -> B -> C` form, and excluded behavior.
- Findings: `BLOCKING`, `IMPORTANT`, `MINOR`, each with file/line and evidence. Distinguish deterministic, test-covered, manually reviewed, and unverified conclusions.
- Acceptance criteria: each criterion checked against evidence.
- Verdict: `PASS` only when deterministic checks pass, acceptance criteria are met, and blocking findings are zero; otherwise `FAIL`.
