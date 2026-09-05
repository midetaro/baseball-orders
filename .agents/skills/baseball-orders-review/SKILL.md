# baseball-orders-review

## Purpose

`baseball-orders` の1機能実装完了時に実行するレビューSkill。

このSkillは新しい機能を実装するためのものではない。

主な目的は以下。

* Feature仕様への適合確認
* module dependency保護
* 決定的に検証可能な要件の自動検証
* Unit / Integration Testの妥当性確認
* Integration Testの疎通範囲の明確化
* Architecture boundary確認
* 不要な変更・過剰実装の検出

決定的に検証可能な事項を、モデルの推測だけでPASS判定してはならない。

---

# 1. Review Scope

原則として今回のFeatureで変更されたdiffのみをレビューする。

最初に以下を確認する。

```bash
git status --short
git diff --stat
git diff
```

可能であればFeature開始地点との差分を対象にする。

既存コード全体を無条件に再レビューしない。

Feature仕様書に関係しない既存問題は、今回の変更によって悪化していない限りBlocking Findingにしない。

---

# 2. Verification Order

レビューは必ず以下の順序で行う。

```text
1. Feature specification確認
2. Deterministic checks
3. Automated tests
4. Diff inspection
5. Architecture/design review
6. Findings整理
```

コードを目視レビューする前に、決定的に検証できるものを可能な限り検証する。

---

# 3. Feature Specification

最初に対象Featureの仕様書を読む。

最低限以下を抽出する。

```text
Goal
Functional Requirements
Architecture Constraints
Deterministic Verification
Testing Requirements
Out of Scope
Acceptance Criteria
```

仕様に記載されていない機能を勝手に要求しない。

Out of Scopeの機能が追加されている場合は指摘する。

---

# 4. Deterministic Verification

## 4.1 Build / Tests

対象moduleで利用可能なGradle testを実行する。

例:

```bash
./gradlew test
```

または対象moduleのfocused test。

実行可能なtestを「コードを見る限り通りそう」という理由でPASSにしてはならない。

---

## 4.2 Module Dependency Protection

module間依存関係の変更は禁止。

以下のdiffを確認する。

```bash
git diff -- \
  '*.gradle' \
  '*.gradle.kts' \
  'settings.gradle' \
  'settings.gradle.kts'
```

以下を特に確認する。

```text
project(...)
include(...)
includeBuild(...)
```

既存module dependencyの:

* 追加
* 削除
* 方向変更

が存在した場合はBlocking Findingとする。

外部OSS/library dependencyについてはFeature仕様の範囲内であれば許容する。

---

## 4.3 Required API / Annotation

仕様で特定annotationやentry pointが要求されている場合、検索またはtestで存在を決定的に確認する。

例:

```text
@SqsListener("simulation-request")
```

存在を推測しない。

---

## 4.4 Correlation / Contract

Request / Result correlationなどassert可能な要件はIntegration Testでassertされていることを確認する。

例:

```text
request.simulationId == result.simulationId
```

「実装上同じ値を渡しているから問題ない」だけでは十分な検証としない。

---

# 5. Integration Test Review

Integration Testには日本語で以下の4項目が明記されていなければならない。

```text
実物
モック
担保する疎通
担保しないもの
```

例:

```java
/**
 * Integration Test
 *
 * 実物:
 * - Floci SQS
 * - simulation-request Queue
 * - @SqsListener
 * - result publisher
 * - simulation-result Queue
 *
 * モック:
 * - SimulationEngine
 *
 * 担保する疎通:
 * simulation-request
 *   -> @SqsListener
 *   -> application
 *   -> publisher
 *   -> simulation-result
 *
 * 担保しないもの:
 * - SimulationEngineの計算結果の正当性
 */
```

---

## 5.1 Mock Completeness

使用されている以下を確認する。

```text
@Mock
@MockBean
@MockitoBean
mock(...)
spy(...)
fake implementation
stub implementation
test configurationによる置換
```

Integration Testコメントの「モック」に漏れがないか確認する。

コメントに:

```text
モック:
- なし
```

と書かれている場合は、本当にmock/replaceされていないことを確認する。

---

## 5.2 Communication Boundary

Integration Testが担保すると主張している疎通経路そのものがmockされていないことを確認する。

例えば:

```text
SQS -> @SqsListener -> Publisher -> SQS
```

を担保すると書いているにもかかわらず、

```text
SqsClient
Publisher
Listener
```

のいずれかがmockされていた場合はBlocking Findingとする。

ただし、疎通経路外の純粋な計算ロジックをmockすることは許容する。

---

## 5.3 Deterministic Test Behavior

Integration Testで可能な限り以下を避ける。

```java
Thread.sleep(5000);
```

固定時間待機だけに依存している場合、条件待ち・future・poll-until等、より決定的な手段へ変更できないか確認する。

Flakyになり得る非決定的テストは指摘する。

---

# 6. Architecture Review

決定的な検証後に、以下をモデルレビューする。

## Responsibilities

* Controller / Listenerにdomain logicが流出していないか
* infrastructure detailがdomainへ侵入していないか
* application layerの責務が過剰でないか

## Coupling

* backendとsimulatorが直接結合していないか
* messaging contract以外を不用意に共有していないか

## Maintainability

* Featureのために不必要な抽象化を追加していないか
* 1回しか使わないinterface/factory等を過剰に導入していないか
* 仕様外のrefactorが混入していないか

## Failure Handling

* exceptionを握りつぶしていないか
* timeout / duplicate / late messageなど仕様にあるケースを破壊していないか

---

# 7. Out-of-Scope Detection

Feature仕様の `Out of Scope` とdiffを比較する。

例えば仕様に以下がある場合:

```text
- DLQ
- Terraform
- Dockerization
- unrelated refactoring
```

今回のdiffで追加されていれば、理由がない限り指摘する。

「将来必要になるから」という理由だけでVersion 2相当を先行実装しない。

---

# 8. Finding Severity

Findingは以下に分類する。

## BLOCKING

Feature完了不可。

例:

* test failure
* compile failure
* module dependency変更
* Acceptance Criteria未達
* Integration Testの主要疎通箇所がmock
* Result correlationが検証されていない
* 仕様外の重大な機能追加

## IMPORTANT

Featureは動くが修正を強く推奨。

例:

* cleanup漏れ
* flaky test
* exception handling不備
* integration testコメントと実装の不一致
* unnecessary coupling

## MINOR

任意改善。

例:

* 命名
* 小さな可読性改善
* コメント改善

MINORのみの場合はFeature完了を妨げない。

---

# 9. Review Output Format

レビュー結果は必ず以下の順序で出力する。

```text
## Deterministic verification

- Build: PASS / FAIL
- Unit tests: PASS / FAIL
- Integration tests: PASS / FAIL
- Module dependencies unchanged: PASS / FAIL
- Required contract checks: PASS / FAIL

## Integration test boundary

実物:
- ...

モック:
- ...

担保する疎通:
A -> B -> C

担保しないもの:
- ...

## Findings

### BLOCKING
- ...

### IMPORTANT
- ...

### MINOR
- ...

## Acceptance Criteria

- [x] ...
- [ ] ...

## Verdict

PASS
```

または:

```text
FAIL
```

---

# 10. Evidence Rules

PASS判定には可能な限り証拠を使用する。

例:

```text
PASS: ./gradlew test がexit code 0
PASS: git diffでproject dependency変更なし
PASS: integration testでsimulationId一致をassert
```

以下のような表現だけでPASSにしない。

```text
問題なさそう
おそらく動く
コード上は正しい
```

検証できなかった場合は明示する。

```text
NOT VERIFIED
```

推測でPASSへ変換しない。

---

# 11. Fix Cycle

BLOCKINGまたはIMPORTANT Findingが見つかった場合:

1. 必要な修正だけ行う。
2. 無関係なrefactorをしない。
3. 修正後にaffected testを再実行する。
4. deterministic checksを再実行する。
5. 再レビューする。

Feature完了宣言前にBLOCKING Findingが0件であること。

---

# 12. Completion

Feature完了条件:

```text
Deterministic verification PASS
+
Acceptance Criteria satisfied
+
BLOCKING findings = 0
```

完了後は次のFeatureを同じCodex sessionで開始せず、新しいsessionを開始することを提案する。

If and only if:
- deterministic verification passes,
- all acceptance criteria are satisfied,
- blocking findings = 0,

update the feature specification status from `in_progress` to `done`.
