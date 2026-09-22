---
name: baseball-orders-test
description: Verify baseball-orders features with focused and owning-module tests, and check simulator subtype test coverage and numeric profile configuration. Use during implementation and before feature review.
---

# Baseball Orders Test

Use this skill to establish executable evidence for a feature. Read the requested specification and the nearest `AGENTS.md`; select focused tests from `docs/architecture.md`, then confirm the current source and Gradle files.

## Red, green, and owning build

1. Add or update the narrowest behavioral test. Run it before the implementation and confirm it fails for the expected behavior. A build, environment, or permission error is not a red test.
2. Implement the change and rerun the focused test. Run the owning application through `./.agents/skills/baseball-orders-development/scripts/verify.sh <backend|simulator|all|terraform>` after the focused tests pass. Use `all` for shared SQS contracts.
3. Report exact commands, exit status, conditional integration tests skipped because their endpoint was absent, and any checks not run. Never infer a PASS from source inspection.

For simulator changes, run `scripts/check-simulator-conventions.sh [repo-root]`. It requires a mirrored `ClassNameTest.java` with a JUnit test for each concrete subtype of a repository interface or abstract class. For a named member subtype such as `Outer.Inner`, use `OuterInnerTest.java` in the package of `Outer`; the test class must have the same name. Anonymous and method-local classes are outside this file-naming rule. Cover behavior particular to each subtype; a class-named test file alone is insufficient for meaningful coverage. It also checks that `simulation.game-count`, `simulation.sqs.max-messages-per-poll`, and `simulation.sqs.long-poll-seconds` have bindings and explicit numeric values in `application-local.yml`, `application-dev.yml`, and `application-prod.yml`. Domain rule literals such as outs, innings, and base masks remain domain decisions and are outside this configuration check.

If deterministic checks fail, report the exact failure. Record integration-test environmental skips explicitly.
