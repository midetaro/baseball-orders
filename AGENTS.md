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
- Change dependencies or module relationships only when the requested behavior requires it. Keep the change minimal and report it.
- Do not change dependency versions unless explicitly requested or required by the task.

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
