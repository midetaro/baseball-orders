# Baseball Orders Development Guide

## Repository map

- `apps/backend`: synchronous HTTP API, persistence, and SQS producer/consumer.
- `apps/simulator`: asynchronous simulation worker and SQS producer/consumer.
- `libs/messaging-contract`: the single source of truth for messages exchanged through SQS.
- `infra/aws-terraform`: native HCL for AWS messaging resources.
- `.agents/skills/baseball-orders-development`: task workflow and verification commands.

Read the nearest nested `AGENTS.md` before changing an application. Use the
`baseball-orders-development` skill for Java, SQS-contract, cross-application, or
Terraform changes.

## Working agreement

- Inspect `git status` first and preserve unrelated user changes.
- Make the safest in-scope assumption when ambiguity does not materially alter behavior; report it.
- Do not wait on interactive commands, foreground servers, credentials, or selectors.
- Keep SQS wire types in `libs/messaging-contract`; do not create application-local copies.
- Keep domain and application models independent from transport types unless the boundary mapper itself consumes a shared message.

## Module dependency protection

- Never add, remove, or change Gradle project dependencies between modules.
- Never modify the module graph in settings.gradle or settings.gradle.kts.
- Existing module dependencies may be inspected and used as-is.
- Adding or changing external OSS/library dependencies is allowed when required.
- If a requested feature cannot be implemented without changing module relationships,
  stop that part of the implementation and report the architectural conflict.
- Do not work around this rule by copying production classes between modules.

## Test-driven changes

1. Add or update the narrowest test that expresses the requested behavior.
2. Run it and confirm it fails for the expected behavioral reason. Environment or permission failures do not count as a red test.
3. Implement the minimum change, rerun the focused test, then refactor.
4. Run the owning application's full verification before completion.
5. For shared contracts, run verification for both applications.

Use `./.agents/skills/baseball-orders-development/scripts/verify.sh` as the canonical verification entrypoint.
When changing a repository skill, also run its official-validator wrapper,
`./.agents/skills/baseball-orders-development/scripts/validate-skill.sh`.

## Production Java

- Add Javadoc to every public production method except methods annotated with `@Override`.
- Document purpose, observable behavior, parameters, non-void returns, and declared exceptions.
- Update Javadoc when behavior or signatures change.

## Implementation style

- Where Lombok is already available, use focused annotations such as `@Getter`,
  `@RequiredArgsConstructor`, and `@Slf4j` to remove trivial getters,
  constructors, and logger declarations.
- Do not use Lombok to hide validation, state transitions, or other domain
  behavior, and do not add a Lombok dependency to a module solely to follow this
  preference.
- Implement branching on enum values with a `switch` expression rather than an
  `if` chain or statement-style `switch` when the branch produces a value.
- List every enum constant explicitly and omit `default` for closed internal
  enums so adding a constant causes a compile-time failure at every affected
  branch.
- Do not use `default` in a `switch` expression unless that branch throws an
  exception. A `default` branch must never return a fallback value, silently do
  nothing, or handle ordinary control flow.

## Completion report

Summarize changed behavior, tests and checks run, skipped conditional tests, assumptions, dependency changes, and unresolved blockers.

## Session boundary recommendation

When the current logical task is complete, do not automatically continue to unrelated work.
At the end of the final response:
- State that the current task is complete.
- If the next work is a separate logical task, explicitly recommend starting a new Codex session.
- Briefly summarize the information that should be carried into the next session.
- Do not continue with the next task unless the user explicitly asks.
Use wording similar to:
"このタスクは完了しました。コンテキスト肥大化を避けるために`/exit && codex`で新しいCodexセッションへ分割することを推奨します。"

## Verification policy

- If a requirement can be verified deterministically, prefer deterministic verification over heuristic or review-based judgment.
- Do not rely on visual inspection, assumptions, or prose review when the same property can be checked by:
  - compilation,
  - automated tests,
  - static analysis,
  - scripts,
  - grep/diff-based checks,
  - dependency graph checks,
  - schema/contract assertions.
- Human/model review should focus on properties that cannot be fully determined automatically.
- A review finding must clearly distinguish:
  - deterministically verified,
  - test-covered,
  - manually reviewed,
  - not verified.

## Integration test documentation

Every integration test must include a Japanese comment or Javadoc that explicitly states:

1. 実物
  - Components/services used without mocking.

2. モック
  - Every mocked, stubbed, faked, or replaced dependency.
  - If nothing is mocked, write `モック: なし`.

3. 担保する疎通
  - The end-to-end path that this integration test proves.
  - Write the path in `A -> B -> C` form where practical.

4. 担保しないもの
  - Important behavior intentionally excluded from this test.

Do not call a test an Integration Test if the intended integration boundary itself is mocked.

## Feature completion

A feature is not complete until all of the following are done:

1. Acceptance criteria are satisfied.
2. Focused tests pass.
3. Owning application verification passes.
4. Run the baseball-orders-review skill against the current feature diff.
5. Fix all blocking findings.
6. Re-run affected tests.
7. Report completion.
8. Recommend starting a new Codex session before beginning another feature.

## Feature specification status

Feature specification documents have a status field:

- todo
- in_progress
- done

When selecting work:
- Ignore specifications with `status: done`.
- Prefer only specifications explicitly requested by the user.
- Do not scan completed specifications unless they are needed to understand an interface or regression.
- After implementation, deterministic verification, and review skill all pass, change the specification status to `done`.
- Do not mark a specification `done` if any blocking review finding remains.