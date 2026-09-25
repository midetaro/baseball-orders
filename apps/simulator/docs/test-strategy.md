# シミュレーター テスト戦略

`apps/simulator` 専用のテスト戦略。`AGENTS.md` / `apps/simulator/AGENTS.md` のルール
（子クラス全数テスト、日本語 `@DisplayName`、`// given // when // then`、`assertAll`、
真偽値の両分岐）を前提に、その上で「何をどの層で担保するか」を定義する。

---

## 0. 前提：現在のコードが持つ継ぎ目（seam）

戦略はテスト容易性の事実から決まる。先に事実を固定する。

### 0.1 乱数の入口は 1 箇所だけ

`RandomGenerator.nextFloat()`（`static`、中身は `Math.random()`）が唯一の乱数源。
`AGENTS.md` によりシード化は独立した feature specification が必要なので、
**決定論を得る唯一の手段は `mockStatic(RandomGenerator.class)` によるスクリプト化**である。

乱数を引く本番コードは以下がすべて。

| クラス | 引く回数 | 条件 |
| --- | --- | --- |
| `ShortDistanceHittingStrategy` / `MiddleDistanceHittingStrategy` / `LongDistanceHittingStrategy` | 1 | 常に |
| `StandardBuntStrategy` | 1 | `NO_OUT` のときだけ |
| `EagerBuntStrategy` | 1 | `NO_OUT` / `ONE_OUT` のときだけ |
| `NowayBuntStrategy` | 0 | — |
| `StandardStealStrategy` / `EagerStealStrategy` | 1 | 常に |
| `NowayStealStrategy` | 0 | — |
| `AbstractBasesState#battingOut` | 1 | `NO_OUT` / `ONE_OUT` かつ先頭走者が居るときだけ |

### 0.2 統計が流れる経路

```
BatterEntity#swing/bunt/stealToX
  -> PlayResultObserver (= GameStatisticsRecorder)   … 1試合の GameStatistics
  -> GameBattingContext#reflectCompletedInning (9回終了)
  -> GameCompletionObserver (= ScoreAccumulator)     … 複数試合の ScoreStatistics
```

ここから導かれる**最重要の制約**：

> `BatterEntity` をモックに置き換えると、統計は一切記録されない。
> したがって「統計を検証するシナリオテスト」では `BatterEntity` は必ず本物を使い、
> 制御は `RandomGenerator` のスクリプト化で行う。

また `GameBattingContext` はコンストラクタで `batter.observedBy(recorder)` により
打者を**コピー**する。テストから渡したインスタンスと打順上のインスタンスは同一ではない。
走者の同一性 (`assertSame`) を見たい場合は `GameStateTestFixture` 経由で
State を直接操作する経路を使う。

### 0.3 差し替え可能／不可能なもの

| 対象 | 可否 | 備考 |
| --- | --- | --- |
| `RandomGenerator` | ◯ `mockStatic` | 確率制御の本命 |
| `BaseStateFactory` | ◯ コンストラクタ注入 | `GameBattingContext` / `SimulateGameUseCase` の両方に公開コンストラクタがある（**本番変更不要**） |
| `BasesState` | ◯ 非 sealed interface | フェイク実装を置ける |
| `GameCompletionObserver` / `PlayResultObserver` | ◯ | 観測用に注入可能 |
| `BatterEntity` | △ mock 可だが統計が死ぬ | フロー検証専用 |
| `HittingStrategy` / `BuntStrategy` / `StealStrategy` / `Stealable` / `Buntable` | ✕ **sealed のため Mockito でモック不可** | 本物＋乱数スクリプトで制御する |
| `InningStateContext` の状態操作 API | △ package-private | 同一パッケージ（`...domain.game`）のテスト／testFixtures からのみ可 |

---

## 1. テスト 4 層モデル

| 層 | 名前 | 対象 | 制御手段 | 主な検証 |
| --- | --- | --- | --- | --- |
| **L1** | 確率仕様テスト | 各 Strategy（子クラス） | `ScriptedRandom` の単一値 | 乱数区間 → 結果の写像（＝確率の仕様書） |
| **L2** | 状態遷移テスト | 8 つの `BasesState` と capability | 乱数なし／単一値 | 走者配置・得点・アウトの遷移 |
| **L3** | イニングシナリオテスト | `GameBattingContext` 1 イニング | `ScriptedRandom` の**列** | 得点・打順・イニング完了・**1試合統計** |
| **L4** | 試合シナリオテスト | `GameBattingContext` / `SimulateGameUseCase` | `ScriptedBaseStateFactory`（イニング情報をフェイク）＋ `ScriptedRandom` | 9 イニング進行・試合終了・**集計統計** |

原則（要件 3）：

- **L1 / L2 を厚く、L3 / L4 は本数を絞る。** L3・L4 は「代表シナリオの回帰」であって網羅の場ではない。
- L3・L4 は決定論的シナリオである。**確率的な期待値検証（大量試行して平均を見る等）は禁止**。フレーキーになる。
- Spring / SQS を domain・application のテストに持ち込まない。それは infrastructure と `integration-test` の担当。

---

## 2. 要件 1：子クラスは全てテストクラスを作る

### 2.1 現状

`check-simulator-conventions.sh` が `src/main/java` のみを走査し、
抽象クラス／インターフェースの具象サブタイプごとに `ClassNameTest.java` を要求する。
現時点で対象はすべて充足している。

| 具象クラス | テスト |
| --- | --- |
| `ShortDistanceHittingStrategy` / `MiddleDistanceHittingStrategy` / `LongDistanceHittingStrategy` | 各 `*Test` |
| `StandardBuntStrategy` / `EagerBuntStrategy` / `NowayBuntStrategy` | 各 `*Test` |
| `StandardStealStrategy` / `EagerStealStrategy` / `NowayStealStrategy` | 各 `*Test` |
| `NoBasesState` … `FullBasesState`（8 種） | 各 `*Test` |
| `GameStatisticsRecorder` / `ScoreAccumulator` | 各 `*Test` |
| `BatterEntity` | `BatterEntityTest` |

testFixtures は走査対象外なので、後述のフィクスチャ追加で規約違反にはならない。

### 2.2 運用ルール

1. **`ClassNameTest` はそのクラス固有の差分だけを書く。**
   共通契約は `BaseStateComponentTest` / `BasesStateTransitionTest` / `StealBehaviorTest` /
   `ExpandedBattingResultTest` のような共有パラメタライズドテストへ寄せる。
   「名前だけのテスト」は規約違反（スクリプトは中身の意味までは見ないので、レビューで担保する）。
2. **capability の `default` メソッドは規約チェックの対象外**（interface の default 実装のため）。
   そのため実装側の `*StateTest` で必ず 1 回は通す。特に以下は分岐を持つので必須。
   - `SqueezeBuntable#buntFailure` の `getOutCount() != OutCount.NO_OUT` 分岐
     （`ThirdBaseState` / `FirstThirdBaseState` / `DoubleThirdBaseState` / `FullBasesState`）
   - `AdvancingBuntable#buntSuccess` の走者進塁（`SingleBasesState` / `DoubleBaseState` / `FirstDoubleBaseState`）
   - `StealableToDoubleBase` / `StealableToTripleBase` の `sourceBase` / `targetBase` / `runner`
3. **新しい具象クラスを足したら、同じコミットでテストを足す。** 完了条件に
   `check-simulator-conventions.sh` を含める。
4. domain の `jacocoTestCoverageVerification` は `strategy.*` / `*BasesState` /
   `BaseStateFactory` / `InningState` に **LINE / BRANCH 100%** を課している。
   L1・L2 がこの 100% を支える層であり、L3・L4 はカバレッジ目的で書かない。

---

## 3. 要件 2：確率に基づく振る舞いを「テストが仕様書になる」形に整理する

### 3.1 形式

各 Strategy に対し **境界値の表**を `@ParameterizedTest` + `@MethodSource` で書く。
既存の `MiddleDistanceHittingStrategyTest` がこの形式なので、これを全戦略へ展開する。

```java
static Stream<Arguments> battingTestCases() {
    return Stream.of(
            arguments("5%未満なら四球になる", 0.04f, BattingResult.WALK),
            arguments("単打配分なら単打になる", 0.30f, BattingResult.HIT_SINGLE),
            ...);
}
```

**1 区間 1 ケースでは不十分**。各区間について
「下端（含む）」と「直前（排他）」の 2 点を必ず置く。そうしないと閾値の移動を検出できない。

### 3.2 確率表（`obp=0.400 / slg=0.550` の基準打者、`MiddleDistanceHittingStrategy`）

```
extraBase = slg - obp = 0.150
double = triple = homer = 0.025, single = 0.325, walk = min(0.05, obp) = 0.050
```

| 乱数区間 | 結果 |
| --- | --- |
| `[0.000, 0.050)` | `WALK` |
| `[0.050, 0.334375)` | `HIT_SINGLE` |
| `[0.334375, 0.35625)` | `HIT_DOUBLE` |
| `[0.35625, 0.378125)` | `HIT_TRIPLE` |
| `[0.378125, 0.400)` | `HIT_HOMER` |
| `[0.400, 0.550)` | `STRIKEOUT` |
| `[0.550, 1.000]` | `BATTED_OUT` |

このような表を **`HittingStrategy` 3 種 × 代表成績、`BuntStrategy` 3 種 × `OutCount` 4 値、
`StealStrategy` 3 種 × 二塁/三塁** について作り、テストの `@DisplayName` に閾値を書く。
テスト一覧 (`./gradlew :domain:test` の HTML レポート) を読めば確率仕様が読み取れる状態を目標にする。

### 3.3 テストで明示すべき既存仕様の“角”

以下は現在の実装がそうなっている挙動。**テストで固定して可視化する**（変更は feature spec 扱い）。

- `StandardStealStrategy` / `EagerStealStrategy` は
  `if (r < NOT_TRY) ... else if (NOT_TRY < r && r < successProbability) ... else FAILURE`
  という構造のため、**`r` がちょうど `NOT_TRY` と等しいときは `FAILURE`** になる。
- `BattingResultSelector` は最終の正重みで `cumulative = onBasePercentage` を使うため、
  **`r == onBasePercentage` は安打にならず `STRIKEOUT` 側へ落ちる**。
- `ShortDistanceHittingStrategy` は triple / homer の重みが 0 なので、
  **`HIT_TRIPLE` / `HIT_HOMER` は到達不能**。これを「到達しないこと」のテストとして書く。
- 盗塁の成功率は**走者本人**の `stealSuccessRate` と戦略を使う（打者ではない）。
  打順が不均一なシナリオでは走者ごとに閾値が変わる。
- `FirstDoubleBaseState` は `StealableToTripleBase`。一二塁では**二塁走者だけ**が三塁を狙う。

### 3.4 ガイドとの同期

確率仕様を変更したら `simulator-guide-sync` スキルで HTML ガイドを同期する。
確率表テストとガイドの記述は同じ数値を指していること。

---

## 4. 共通フィクスチャ設計

`domain/src/testFixtures` に置く（`application` は既に `testImplementation testFixtures(project(':domain'))` 済みなので再利用できる）。

```
domain/src/testFixtures/java/com/example/baseballorders/simulator/domain/
  player/strategy/ScriptedRandom.java        乱数列のスクリプト化と消費検証
  player/strategy/Draws.java                 名前付き乱数定数（基準打者前提）
  player/BatterTestData.java                 基準打者・打順ビルダ
  game/GameStateTestFixture.java             既存（走者配置の作成）
  game/ScriptedBaseStateFactory.java         イニング情報のフェイク（要件5）
  game/InningScript.java                     イニング単位の脚本
  statistics/StatisticsAssertions.java       統計の恒等式アサーション
```

> ビルド変更：`ScriptedRandom` が Mockito を使うため
> `testFixturesApi 'org.mockito:mockito-core:5.21.0'` を `domain/build.gradle` に追加する。
> 外部ライブラリの追加であり、モジュール間依存の変更ではない（`AGENTS.md` の保護対象外）。

### 4.1 `ScriptedRandom`

```java
try (ScriptedRandom random = ScriptedRandom.of(Draws.SINGLE, Draws.BUNT_SUCCESS, Draws.HOMER)) {
    ...
    random.assertFullyConsumed();   // 余りがあればシナリオの想定違い
}
```

- `mockStatic(RandomGenerator.class)` をラップし、`thenAnswer` で列を先頭から返す。
- **不足したら即失敗**（`"乱数スクリプトが尽きた: N回目の要求"`）。
- **余ったら `assertFullyConsumed()` で失敗**。
  これがあるおかげで、本番の乱数消費回数が変わった時にシナリオが静かに壊れず、必ず赤くなる。

### 4.2 `Draws`：名前付き乱数定数

基準打者（`obp=0.400, slg=0.550, bunt=0.700, steal=0.800`、
`MiddleDistance` / `StandardBunt` / `StandardSteal`）に対する値。

| 定数 | 値 | 意味 |
| --- | --- | --- |
| `WALK` | `0.04f` | 四球 |
| `SINGLE` | `0.30f` | 単打 |
| `DOUBLE` | `0.34f` | 二塁打 |
| `TRIPLE` | `0.36f` | 三塁打 |
| `HOMER` | `0.39f` | 本塁打 |
| `STRIKEOUT` | `0.45f` | 三振 |
| `BATTED_OUT` | `0.60f` | 凡退 |
| `ADVANCE` | `0.05f` | 凡退時に先頭走者が進む（1・2 塁 20% / 3 塁 10% すべて満たす） |
| `NO_ADVANCE` | `0.50f` | 凡退時に進まない |
| `BUNT_SUCCESS` | `0.10f` | バント成功（成功率 0.700 未満） |
| `BUNT_FAILURE` | `0.90f` | バント失敗 |
| `STEAL_TO_SECOND_NOT_TRY` | `0.50f` | 二盗を試みない（`< 0.80`） |
| `STEAL_TO_SECOND_SUCCESS` | `0.90f` | 二盗成功（`0.80 < r < 0.96`） |
| `STEAL_TO_SECOND_FAILURE` | `0.98f` | 二盗失敗 |
| `STEAL_TO_THIRD_NOT_TRY` | `0.50f` | 三盗を試みない（`< 0.95`） |
| `STEAL_TO_THIRD_SUCCESS` | `0.97f` | 三盗成功（`0.95 < r < 0.99`） |
| `STEAL_TO_THIRD_FAILURE` | `0.995f` | 三盗失敗 |

> 同じ `0.90f` が「二盗成功」と「バント失敗」を同時に意味することからも分かるとおり、
> **値の意味は文脈（どの Strategy がその乱数を引いたか）に依存する**。
> だから生の float ではなく名前付き定数でシナリオを書く。

**`DrawsSelfTest`（必須）**：`Draws` の各定数を基準打者の各 Strategy に食わせ、
表どおりの結果になることを検証する。確率仕様が変わった瞬間にここが赤くなり、
L3 / L4 のシナリオが「意味が変わったのに緑のまま」になる事故を防ぐ。

### 4.3 1 打席あたりの乱数消費順（シナリオ記述の要）

`AtBatProcessor#process` の順序。**これを破るとシナリオが全滅するので、変更時は本ドキュメントも更新する。**

```
1. 盗塁フェーズ  : 現 State が Stealable かつ走者の戦略が乱数を引く場合に 1 個消費
                   （NowayStealStrategy は 0 個）
                   → 盗塁でイニングが変わる／試合終了 なら打席は未完了(false)で終了し、以降は消費しない
2. バントフェーズ: 現 State が Buntable かつ戦略が試行条件を満たす場合に 1 個消費
                   （Noway は 0 個。Standard は NO_OUT のみ、Eager は NO_OUT/ONE_OUT のみ）
                   → SUCCESS / FAILURE なら打席完了(true)で終了し、打撃は行わない
3. 打撃フェーズ  : 必ず 1 個消費
4. 凡退進塁判定  : BATTED_OUT かつ NO_OUT/ONE_OUT かつ先頭走者あり のときだけ 1 個消費
```

State ごとの capability（乱数の有無を決める）：

| State | Stealable | Buntable |
| --- | --- | --- |
| `NoBasesState` | — | — |
| `SingleBasesState` | 二盗 | 進塁バント |
| `DoubleBaseState` | 三盗 | 進塁バント |
| `FirstDoubleBaseState` | 三盗（二塁走者） | 進塁バント |
| `ThirdBaseState` | — | スクイズ |
| `FirstThirdBaseState` | 二盗 | スクイズ |
| `DoubleThirdBaseState` | — | スクイズ |
| `FullBasesState` | — | スクイズ |

---

## 5. 要件 4：1 イニングのシナリオテスト

### 5.1 方針

- 対象：`GameBattingContext`（本物の `BaseStateFactory`・本物の `BatterEntity`）。
- 制御：`ScriptedRandom` のみ。**確率を明示的に固定して、プレーの並びを脚本にする。**
- 単位：3 アウトでイニングが完了するまで `nextAtBat()` を回す。
- 配置：`domain/src/test/java/.../domain/game/inning/` に
  `<シナリオ名>InningScenarioTest.java` として置き、L2 の遷移テストと混ぜない。

### 5.2 テストの書き方（雛形）

```java
@Test
@DisplayName("無死単打→二盗成功→スクイズ成功→凡退→三振で1点、1イニングが完了する")
void scoresOneRunByStealAndSqueeze() {
    // given
    var sut = new GameBattingContext(BatterTestData.uniformLineUp());
    try (ScriptedRandom random =
            ScriptedRandom.of(
                    Draws.SINGLE,                    // 1番: 走者なし → 単打
                    Draws.STEAL_TO_SECOND_SUCCESS,   // 2番打席前: 一塁走者が二盗成功
                    Draws.BUNT_SUCCESS,              // 2番: 二塁 → 進塁バント成功(1死、三塁)
                    ...)) {

        // when
        while (sut.getInning() == 1) {
            sut.nextAtBat();
        }

        // then
        GameStatistics statistics = sut.getGameStatistics();
        assertAll(
                () -> assertEquals(1, sut.getTotalScore()),
                () -> assertEquals(2, sut.getInning()),
                () -> assertEquals(expectedStatistics(), statistics),
                () -> random.assertFullyConsumed());
    }
}
```

### 5.3 検証項目（要件 4-2：統計も検証する）

1. **得点**：`getTotalScore()`。
2. **イニング完了**：`getInning()` が +1、走者・アウトがリセットされていること。
3. **打順の進行**：盗塁成功・盗塁でのイニング終了では打席は完了せず**打順が進まない**
   （`AtBatProcessor` が `false` を返す仕様）。打順を観測するため、
   打者ごとに異なる成績を持つ打順（`BatterTestData.distinctLineUp()`）を用意し、
   消費された乱数の解釈が打者ごとに変わることを利用して検証する。
4. **統計**：`GameStatistics` は `record` なので、**期待値を `GameStatisticsBuilder` で組み立てて
   `assertEquals` で丸ごと比較する**。個別フィールドを列挙しない。
   こうすると統計項目が追加された時に既存シナリオが必ず落ち、更新漏れを検出できる。
5. **恒等式**：`StatisticsAssertions.assertConsistent(statistics)` を必ず併用（§7）。

### 5.4 最低限そろえる代表シナリオ

| シナリオ | 主な担保 |
| --- | --- |
| 三者凡退（三振 3 つ） | 乱数 3 個だけでイニングが閉じる最小経路 |
| 単打 → 二盗成功 → 進塁バント成功 → スクイズ成功 | 盗塁・バント両フェーズの成功経路と `advancingBuntCount` / `squeezeBuntCount` |
| 単打 → 二盗失敗 → 併殺なしで 3 死 | `stealFailureCount` とアウト加算 |
| 満塁弾（走者 3 人 → `HIT_HOMER`） | `grandSlamCount` と 4 得点 |
| 無死満塁スクイズ失敗 | `SqueezeBuntable#buntFailure` の二死適用とイニング完了 |
| 凡退で先頭走者だけ進塁 | `battingOut` の追加乱数消費と走者進塁 |
| 打順一巡（9 打席連続で三振） | 10 打席目が 1 番打者に戻ること。**現状は §11-6 の不具合により赤になる想定** |

---

## 6. 要件 5：試合単位のシナリオテスト（イニング情報をモック化）

### 6.1 なぜフェイクが要るか

9 イニングを乱数だけで駆動すると、最低でも 27 アウト分・数十個の乱数列が必要になり、
シナリオが読めなくなる。そこで**イニングの進み方そのものをフェイク化**し、
「試合レベルの関心事」だけを残す。

### 6.2 継ぎ目：`BaseStateFactory` の注入（本番変更不要）

```java
public GameBattingContext(LineUpEntity, GameCompletionObserver, BaseStateFactory)   // 既存
public SimulateGameUseCase(int gameCount, BaseStateFactory)                          // 既存
```

`ScriptedBaseStateFactory` は `...domain.game` パッケージの testFixtures に置く。
同一パッケージなので `InningStateContext` の package-private API
（`addOut` / `addScore` / `completeInning` / `place` / `changeState` / `isGameOver`）を使える。

```java
public final class ScriptedBaseStateFactory extends BaseStateFactory {
    private final Deque<PlayOutcome> script;   // 1打席ごとの「得点・アウト・次の走者配置」

    @Override
    public BasesState create(InningStateContext context, int configuration) {
        return new ScriptedBasesState(context, script);
    }
}
```

`ScriptedBasesState` は `BasesState` の全メソッドを実装し、
**どのプレーが来ても脚本の次エントリを適用する**（得点加算・アウト加算・3 死でイニング完了）。

`InningScript` は読みやすい入口を与える。

```java
InningScript.of(
        inning(1).runs(2).outs(3),
        inning(2).runs(0).outs(3),
        ...,
        inning(9).runs(1).outs(3));
```

### 6.3 担保範囲を明記する（重要）

フェイク化すると「得点ロジックは検証していない」ことになる。
`AGENTS.md` の結合テスト記述ルールに倣い、**各試合シナリオテストの Javadoc に明記する**。

```java
/**
 * 実物: GameBattingContext, InningStateContext, GameStatisticsRecorder, BatterEntity, 各 Strategy
 * モック: BaseStateFactory / BasesState（イニング進行を脚本化）, RandomGenerator（乱数列を固定）
 * 担保する疎通: 打席 -> イニング完了 -> 試合得点集計 -> GameCompletionObserver -> ScoreAccumulator
 * 担保しないもの: 走者配置ごとの進塁・得点規則（L2 の BasesState テストが担保する）
 */
```

### 6.4 検証項目（要件 5-2：試合統計も検証する）

1. **9 イニングで終了**：`isGameOver()` が true、`getInning()` が 9 で止まる、
   10 イニング目に進まない、終了後の `nextAtBat()` / `completeInning()` が無視される。
2. **得点の合算**：`getTotalScore()` が各イニング得点の総和。
   進行中イニングの得点も含まれること（`totalScore + inningStateContext.score()`）。
3. **完了通知**：`GameCompletionObserver` が**ちょうど 1 回**、最終得点と最終統計を伴って呼ばれる
   （既存 `GameBattingContextTest#notifiesGameCompletionObserverOnlyOnce` を土台に拡張）。
4. **試合統計**：`GameStatistics` を record 全体で比較。
5. **集計統計**：`SimulateGameUseCase` + `ScoreAccumulator` 経由で
   `ScoreStatistics` を決定論的に検証する。
   - `gameCount`、`scoreDistribution`（合計 = 試合数、キーの最大 = `maximumScore`）
   - `averageScore`
   - `medianScore` は **試合数が偶数のケースと奇数のケースを両方**書く
     （`ScoreAccumulator` の中央値計算が分岐するため）
   - 各カウンタが全試合の `GameStatistics` の合計であること
6. 現行の `SimulateGameUseCaseTest`（実乱数・恒等式のみ）は**スモークテストとして残す**。
   決定論シナリオは `SimulateGameUseCaseScenarioTest` として別に追加する。

---

## 7. 統計検証の共通ルール（4-2 / 5-2 共通）

`StatisticsAssertions` に恒等式をまとめ、統計を触る全テストで呼ぶ。

```
GameStatistics:
  hitCount        = singleHitCount + doubleHitCount + tripleHitCount + homeRunCount
  homeRunCount    = soloHomeRunCount + twoRunHomeRunCount + threeRunHomeRunCount + grandSlamCount
  buntCount       = advancingBuntCount + squeezeBuntCount
  buntFailureCount= advancingBuntFailureCount + squeezeBuntFailureCount
  stealCount      = stealToSecondCount + stealToThirdCount

ScoreStatistics:
  上記すべて（合算後も成立する）
  sum(scoreDistribution.values()) = gameCount
  max(scoreDistribution.keySet()) = maximumScore
  averageScore, medianScore は分布と整合
```

さらに：

- **期待統計は record 全体の `assertEquals` で比較する**（フィールド列挙禁止）。
  統計項目の追加が既存シナリオを赤にし、更新漏れを検出する唯一の仕組みになる。
- `ScoreAccumulator#toScoreStatistics` の空リスト例外は
  `assertThrows` を `// when` に置き、`// then` でメッセージを確認する（`AGENTS.md`）。

---

## 8. 決定論とフレーキー防止のルール

1. **テストから `Math.random()` を直接使わない。** 乱数は必ず `ScriptedRandom` 経由。
2. **時刻・スレッド・実行順に依存しない。**
3. **`mockStatic` は ThreadLocal で効く。** JUnit の並列実行を有効化する場合は、
   `RandomGenerator` を使うテストに `@ResourceLock("RandomGenerator")` か
   `@Execution(SAME_THREAD)` を必須にする。現在は並列未設定なので問題ないが、
   将来の並列化で静かに壊れる箇所として記録しておく。
4. **乱数の消費数を必ず検証する**（`assertFullyConsumed`）。
5. **試行回数に依存する確率的アサーションを書かない。**
   「本塁打が 1 本以上出る」のような期待は禁止。必要なら乱数を固定する。
6. `try (MockedStatic ...)` のブロック内で `when` まで完結させ、
   アサーションはブロック外（またはブロック内で完結）に統一する。
   現状のテストで書き方が揺れているので、新規分は「`// when` の中で実行し、
   `// then` はブロック内で `assertAll`」に揃える。

---

## 9. 実行コマンド

| 目的 | コマンド |
| --- | --- |
| domain の集中テスト | `apps/simulator` で `./gradlew :domain:test` |
| application | `./gradlew :application:test` |
| カバレッジ検証（100% 対象） | `./gradlew :domain:jacocoTestCoverageVerification` |
| 規約チェック（子クラス全数・数値設定） | `.agents/skills/baseball-orders-test/scripts/check-simulator-conventions.sh` |
| 完了前の検証 | `./.agents/skills/baseball-orders-development/scripts/verify.sh simulator` |

---

## 10. 段階導入計画

| Phase | 内容 | 完了条件 |
| --- | --- | --- |
| 1 | `Draws` + `DrawsSelfTest` + `ScriptedRandom`（既存テストは触らない） | `:domain:test` green |
| 2 | L1 確率仕様テストの境界値拡充（全 Strategy） | jacoco 100% 維持、確率表が `@DisplayName` から読める |
| 3 | L2 の capability `default` 分岐の穴埋め | `check-simulator-conventions.sh` green |
| 4 | L3 イニングシナリオ（§5.4 の 6 本） | 統計の record 全体比較が入る |
| 5 | `ScriptedBaseStateFactory` + L4 試合シナリオ | 9 イニング・通知 1 回・集計統計の決定論検証 |
| 6 | `SimulateGameUseCaseScenarioTest`（中央値の偶奇 2 本を含む） | `verify.sh simulator` green |

各 Phase は独立して価値を出すので、1 セッション 1 Phase を推奨する。

---

## 11. 着手前に決めるべき仕様確認事項

テスト化は仕様の固定を意味するため、以下は「現状を固定する／修正する」の判断が先に要る。

1. 盗塁戦略の `r == NOT_TRY` がちょうどの場合に `FAILURE` になる件（§3.3）。
2. `BattingResultSelector` の `r == onBasePercentage` が `STRIKEOUT` になる件（§3.3）。
3. `ShortDistanceHittingStrategy` で三塁打・本塁打が到達不能な件。
4. 盗塁でイニングが変わった打席は未完了扱い（`false`）となり、
   **次イニングの先頭打者が同じ打者になる**件（`AtBatProcessor` 30–32 行目）。
5. `SqueezeBuntable#buntFailure` の二死適用条件 `getOutCount() != NO_OUT`。
6. **`GameBattingContext#nextAtBat` の打順一巡（不具合の疑いが濃い）**。

   ```java
   if (numberOfNextBatter == 8) { numberOfNextBatter = 0; }
   numberOfNextBatter++;
   ```

   `0 → 1 → … → 8` と進み、index 8（9 番打者）の打席後は `0` にリセットしてから `++`
   されるため**次は index 1（2 番打者）**になる。結果として
   **1 番打者は試合の最初の 1 打席しか回ってこない**。
   打順一巡のシナリオテスト（§5.4）を書けば即座に赤くなる。
   修正は打順という公開挙動の変更にあたるため、feature specification を切って対応する。

いずれも「修正」に該当する場合は feature specification が必要（`AGENTS.md`）。
テスト戦略としては**まず現状を固定するテストを書き、変更は別タスクにする**。
