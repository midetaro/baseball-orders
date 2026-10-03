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
Use `baseball-orders-test` for deterministic test and static checks before
`baseball-orders-review` assesses the completed feature diff. When changing a
repository skill, validate each changed skill with the official
`skill-creator/scripts/quick_validate.py`. Use the existing
`./.agents/skills/baseball-orders-development/scripts/validate-skill.sh` wrapper
for `baseball-orders-development`.

In the simulator, every concrete class that implements an interface or extends
an abstract class must have a dedicated test class named after that concrete
class. Shared behavior tests may supplement those tests. Keep tunable numeric
values in named properties where practical, with explicit local, dev, and prod
profile values; retain numeric literals that express fixed rules or indexes in
code. Run the checks provided by `baseball-orders-test` for these rules.

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
- use Staged Builders over `new`: Always use the Staged Builder pattern 
  instead of direct instantiation (`new`) for value object creation 
  to enforce compile-time safety and prevent missing required fields.


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

## 数値定数の構成ファイル化方針

### 目的
- 挙動を調整するための数値をコードのリテラルに埋め込むと、環境ごとの調整にコード変更とビルドが必要になり、変更の影響範囲も追跡しにくくなる。

### 方針
- しきい値・重み・確率・時間・上限など、チューニング対象になり得る数値定数は、原則としてコード中のリテラルではなく構成ファイル（`application.yml`/`application-{profile}.yml` 等）のプロパティとして定義する。
- 各設定値は既存の local・dev・prod プロファイルごとに明示的な値を持たせる。
- 配列やコレクションの添字、業務ルールそのものを表す不変の数値（例: 3ストライク、9イニングなど、ゲーム定義上変更され得ない値）はコード内のリテラルのままでよい。
- 設定値はコンストラクタ注入でフィールドとして保持し、既存メソッド（`@Bean` メソッドや static ユーティリティメソッドを含む）のシグネチャに引数を追加して受け渡すことは禁止する。static ユーティリティに設定が必要になった場合はインスタンス化する。

### 対象外
- 単体テストのみで使うテスト用の数値。
- 恒久的な設定として管理する必要のない、一度きりのスクリプトやマイグレーションの数値。

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
4. Run the baseball-orders-test skill, then the baseball-orders-review skill
   against the current feature diff.
5. Fix all blocking findings.
6. Re-run affected tests.
7. Open a pull request linked to the tracking GitHub Issue, per "Issue-driven
   development" above.
8. Confirm the pull request's CI succeeds, per "Pull request CI verification"
   below.
9. Report completion.
10. Recommend starting a new Codex session before beginning another feature.

## Issue-driven development

- Development tasks are tracked as GitHub Issues in this repository (`gh issue
  list`, `gh issue view <number>`), not only as documents under
  `docs/features`.
- Before starting implementation, identify the GitHub Issue this session is
  working on. If the user does not name one, ask for the issue number, or
  create the issue first with `gh issue create` before writing code.
- Do not open a pull request for tracked development work without an
  associated issue. A small, incidental fix explicitly scoped by the user in
  the same session is the only exception.
- When implementation is complete, deterministic verification has passed, and
  `baseball-orders-test` then `baseball-orders-review` have run against the
  diff per "Feature completion" below, open the pull request with `gh pr
  create` and link it to the issue by including `Closes #<issue-number>` (or
  `Refs #<issue-number>` when the PR does not fully close the issue) in the PR
  body.
- Record the tokens consumed by the working session in both the pull request
  title and description. Immediately before `gh pr create`, run
  `./.agents/skills/baseball-orders-development/scripts/session-token-usage.sh codex`
  (Claude Code uses `claude` instead of `codex`). Append
  `(tokens: <total_tokens>)` to the title, for example
  `feat: add scoreboard (tokens: 1045K)`, and add a `## Token usage` section
  to the description containing the script's full output. The figure includes
  the session's subagents. When the same session later updates the pull
  request (review or CI fixes), rerun the script and update both the title and
  the section with `gh pr edit`, so the recorded value is the session's final
  consumption. If the script cannot find the session, write `tokens: unknown`
  and state the reason in the section rather than estimating a value.
- If a `docs/features` specification document is also used for a feature that
  has a tracking issue, keep the two in sync: mark the specification
  `status: done` and close the issue together, in the same session that merges
  the PR.

## Pull request CI verification

- After opening or updating a pull request, a task is not complete until that
  pull request's CI has succeeded. Do not report completion, mark a feature
  specification `status: done`, or close the tracking issue while CI is still
  running, unknown, or failing.
- Check CI status with `gh pr checks <pr-number>` (add `--watch` to block until
  checks finish, since this is monitoring your own PR's automated checks, not
  an unrelated interactive command). Re-check after pushing new commits.
- If CI fails, continue the same task: inspect the failure with `gh run view
  --log-failed` (or the equivalent check output), fix the root cause, push a
  new commit to the same branch, and re-check CI. Do not amend or force-push to
  hide a failed run unless the user explicitly asks. Do not consider the task
  complete until CI succeeds.
- Only report completion, per "Feature completion" above, once `gh pr checks`
  shows every required check passing.

## Git branch workflow

Do not create or use additional Git worktrees. Work in the existing repository
checkout and isolate each GitHub Issue's changes on its own branch.

- Only start implementation for an issue that is currently open. Confirm with
  `gh issue view <number> --json state` (or `gh issue list --state open`)
  before creating its branch; never branch off a closed issue.
- One issue maps to exactly one branch. Do not reuse an issue's branch for a
  second, unrelated issue or mix changes from multiple issues on one branch.
- Create every new branch from the latest `develop`. First switch to
  `develop` and bring it up to date with the remote, then branch from it in
  the existing checkout:
  ```
  git switch develop
  git pull --ff-only origin develop
  git switch -c feature/<date>[-<n>] develop
  ```
  (see `git branch` for this repository's existing `feature/YYYYMMDD[-n]`
  naming). Never create a new branch from another feature branch or from a
  stale local `develop`. If `git pull --ff-only` fails because local
  `develop` has diverged, report the blocker instead of resetting it.
- Work on only one issue at a time in this checkout. Finish, review, and merge
  its pull request before starting another issue from the updated `develop`.
  Independent worker nodes within the same issue may still run concurrently
  under the feature graph workflow and exclusive file-ownership rules.
- Before switching branches, inspect `git status` and preserve all uncommitted
  and unpushed work. Do not discard changes or carry unrelated changes into
  another issue's branch. If existing work prevents a safe switch, report the
  blocker rather than creating a worktree as a workaround.
- After an issue's pull request is merged, switch back to `develop` and delete
  the now-merged local branch with `git branch -d <branch>`.
- Do not remove pre-existing worktrees as part of this workflow change. Any
  cleanup must preserve uncommitted and unpushed work and be explicitly scoped.

## Commit granularity

Split an issue's work into small, reviewable commits on its branch instead of
one commit for the whole issue.

- Create one commit each time an implementation unit and its tests pass:
  after a focused change is implemented and its focused tests (and any
  affected module verification) are green, commit that unit before starting
  the next one. Do not commit code whose tests are known to fail.
- Put documentation changes in their own dedicated commits. Do not mix
  documentation (`*.md`, `docs/**`, `AGENTS.md`, `CLAUDE.md`, skill
  `SKILL.md`/reference files, feature specifications, README files) with
  production code or test changes in the same commit. Javadoc and code
  comments that accompany a code change belong to that code commit.
- Review findings and CI fixes are additional commits on the same branch,
  following the same split between code and documentation.
- Stage only the files that belong to the commit (`git add <path>`); do not
  use `git add -A` to sweep unrelated changes into it.
- Write each commit message as a single purpose, using this repository's
  existing `feat:` / `fix:` / `refactor:` / `test:` / `chore:` / `docs:`
  prefix style. Documentation-only commits use `docs:`.

## Screen screenshots in pull requests

- When a change alters what a backend Thymeleaf screen (`apps/backend/infrastructure/src/main/resources/templates/*.html`)
  looks like — template, inline style, or a presentation view-model change —
  capture a screenshot of every changed screen, per the representative PC
  (1280×800) and smartphone (390×844) viewports used by the
  `baseball-orders-screen-review` skill, and paste them into the pull request
  description before opening or updating it.
- Capture screenshots against the real rendered screen (through its actual
  route and controller), not a static file opened directly.
- A change that only affects non-visual behavior (backend logic, SQS
  messaging, Terraform, tests) with no template/CSS/view-model diff does not
  require screenshots.
- When updating an already-open pull request for a screen change made after
  the PR was created, add the new screenshots to that PR's description rather
  than leaving them undocumented.

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
