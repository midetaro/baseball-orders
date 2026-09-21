# Baseball Orders Development Guide

## Repository map

- `apps/backend`: synchronous HTTP API, persistence, and SQS producer/consumer.
- `apps/simulator`: asynchronous simulation worker and SQS producer/consumer.
- `libs/messaging-contract`: the single source of truth for messages exchanged through SQS.
- `infra/aws-terraform`: native HCL for AWS messaging resources.
- `.agents/skills/baseball-orders-development`: task workflow and verification commands.

Before a broad implementation search, use `docs/architecture.md` to select the
smallest relevant production path and test set. Then confirm that focused path
against the current source and Gradle files; the map is navigation guidance, not
a substitute for the code.

Read the nearest nested `AGENTS.md` before changing an application. Use the
`baseball-orders-development` skill for Java, SQS-contract, cross-application, or
Terraform changes.

## Working agreement

- Inspect `git status` first and preserve unrelated user changes.
- Make the safest in-scope assumption when ambiguity does not materially alter behavior; report it.
- Do not wait on interactive commands, foreground servers, credentials, or selectors.
- Keep SQS wire types in `libs/messaging-contract`; do not create application-local copies.
- Keep domain and application models independent from transport types unless the boundary mapper itself consumes a shared message.
- Before implementation, inspect the related production path, tests, specifications,
  and build files. Protect existing code and all uncommitted user changes; never
  discard or overwrite work outside the assigned scope.

## Application and module boundaries

- `apps/backend` owns the synchronous HTTP API, server-rendered Thymeleaf UI,
  application coordination, result waiting, and SQS adapters. It must not contain
  simulator business rules or directly depend on simulator classes.
- `apps/simulator` owns simulation use cases and business rules and consumes and
  produces SQS messages through adapters. It must not directly depend on backend
  classes.
- `libs/messaging-contract` is limited to the existing SQS wire contract. Do not
  move domain models, persistence entities, forms, or view models into it.
- `integration-test` verifies the assembled backend -> SQS -> simulator -> SQS ->
  backend path. `infra` owns deployment and local-environment definitions.
- Within each application, dependencies point inward: `infrastructure ->
  application -> domain`. Existing direct dependencies declared in Gradle are the
  authority; do not introduce a reverse dependency. Framework, Thymeleaf, SQS,
  HTTP, and persistence details are forbidden in domain code.
- Controllers and listeners translate and delegate. They must not implement
  business rules. Keep transport DTOs, backend internal models, simulator internal
  models, and persistence entities separate at their boundaries.
- Preserve the current modules, composite builds, and dependency directions. Do
  not add, remove, rename, or move modules to complete a feature.

## Feature graph workflow

Use the custom agents in `.codex/agents` for a feature that crosses independent
areas. Small, single-area changes should remain in the parent agent when delegation
would cost more coordination than it saves.

1. Run `explorer` first to map the execution path, dependencies, tests, candidate
   files, and conflicts without editing.
2. The parent agent turns that evidence into a dependency graph and assigns each
   writable file to exactly one node.
3. Run only independent worker nodes concurrently. A node that consumes another
   node's contract or output must wait for that upstream node.
4. Wait for every worker to finish. A blocked, failed, or incomplete worker is not
   a successful node.
5. Run `integrator` only after all required workers are complete.
6. Run `reviewer` in a fresh context using the specification, final diff, and test
   evidence rather than worker conversation history.
7. Fix blocking findings, rerun affected checks, then run the full verification.

Prefer parallel read-heavy exploration, tests, and review. Parallelization being
possible does not make it worthwhile: do not parallelize tightly coupled edits or
small changes. Never assign the same file to multiple agents at the same time.
Give every writing agent an explicit writable file or directory scope. The parent
agent must wait for all spawned agents before integration and final reporting.

Each worker may perform at most five implementation loops: implement, run its
owning-module tests, identify the failure, fix the single most fundamental cause,
and rerun. Stop earlier when all tests and acceptance criteria pass, or stop and
report when five loops are exhausted, an out-of-scope change is required, or an
ambiguous decision would materially change behavior. Every worker reports:

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

The simulator's stochastic behavior must be reproducible for the same explicit
seed and input when a feature introduces or changes seeded simulation. Do not add
hidden entropy or replace a supplied seed. The current code still uses
`Math.random()` and has no seed input; changing that public behavior requires an
explicit feature specification rather than an incidental refactor.

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
- Generate Value Objects with Jilt's STAGED Builder using
  `@Builder(style = BuilderStyle.STAGED)`; do not hand-write builder classes.
- Do not use Lombok to hide validation, state transitions, or other domain
  behavior, and do not add a Lombok dependency to a module solely to follow this
  preference.
- Implement branching on enum values with a `switch` expression rather than an
  `if` chain or statement-style `switch` when the branch produces a value.
- List every enum constant explicitly and omit `default` for closed internal
  enums so adding a constant causes a compile-time failure at every affected
  branch.
- Use sealed interface or sealed class when it can be.
- Do not use `default` in a `switch` expression unless that branch throws an
  exception. A `default` branch must never return a fallback value, silently do
  nothing, or handle ordinary control flow.
- use `_` instead of `ignored` as a local variable name. 

## コレクション集計におけるStream利用方針

### 目的
- 同一コレクションを複数回走査する集計処理を避け、不要なCPU消費とコードの重複を抑える。
### 禁止事項
- 同一のコレクションに対して、集計項目ごとに終端操作付きのStreamを繰り返し実行してはならない。
```
gameStatistics.stream().mapToInt(GameStatistics::homeRunCount).sum();
gameStatistics.stream().mapToInt(GameStatistics::buntCount).sum();
gameStatistics.stream().mapToInt(GameStatistics::stealCount).sum();
```
特に、同じメソッド内で次のようなコードを複数記述してはならない。
```
collection.stream().mapToInt(...).sum();
collection.stream().filter(...).count();
collection.stream().mapToLong(...).sum();
```
Streamは終端操作ごとにコレクションを走査するため、集計項目が増えるほど同じ要素を繰り返し処理することになる。

### 推奨実装

複数の値を同時に集計する場合は、原則として1回のループで集計する。
```
int homeRunCount = 0;
int buntCount = 0;
int stealCount = 0;

for (GameStatistics statistics : gameStatistics) {
homeRunCount += statistics.homeRunCount();
buntCount += statistics.buntCount();
stealCount += statistics.stealCount();
}
```

集計項目が多い場合、または集計処理を再利用する場合は、可変な集計用クラスを作成して処理を分離する。

```
ScoreAccumulator accumulator = new ScoreAccumulator();
for (GameStatistics statistics : gameStatistics) {
accumulator.add(statistics);
}

return accumulator.toScoreStatistics(
averageScore,
medianScore,
maximumScore,
gameCount,
scoreDistribution
);
```

### 許容されるStream利用
- 次の場合はStreamの利用を許容する。
  - コレクションを1回だけ走査する単純な変換・抽出
  - 同じコレクションを複数回走査しても、各処理が独立しており、データ量が十分に小さいことが明確な場合
  - 1回のStream処理に集計ロジックを集約したカスタムCollectorを使用する場合
  - 可読性上の理由から複数回走査が適切であり、性能上の影響を計測・確認済みの場合
- ただし、許容事項に該当する場合でも、複数回走査の理由をコメントまたは設計書に残す。

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
