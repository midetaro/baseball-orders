# Simulator

`simulator` は、SQS で受け取った打順を使って野球の試合を複数回シミュレーションし、得点・プレー統計を SQS に返す非同期ワーカーです。

このREADMEでは、実装を読むときの共通言語としてデザインパターンの用語を使います。ここでの名称はコードの意図を説明するためのものであり、すべてを厳密な GoF パターンとして扱うものではありません。

## 全体像

```text
SQS request
  -> SqsSimulationScheduler（Inbound Adapter）
  -> LineUpMapper（Mapper / Anti-corruption boundary）
  -> SimulateGameUseCase（Application Service）
  -> GameBattingContext と Domain Model
  -> SimulationResult
  -> SqsSimulationScheduler（Outbound Adapter）
  -> SQS result
```

パッケージは `application`、`domain`、`infrastructure` に分かれています。これはレイヤード・アーキテクチャであり、外部 I/O を `infrastructure` に寄せ、試合のルールを `domain` に閉じ込める構成です。`SimulationRequestMessage` の復元と `SimulationResultMessage` の直列化は infrastructure adapter の責務です。この点では Hexagonal Architecture（Ports and Adapters）の考え方も採用しています。

## 採用しているパターン

| パターン | 主な要素 | このモジュールでの役割 |
| --- | --- | --- |
| Adapter | `SqsSimulationScheduler` | AWS SQS の受信・送信を、アプリケーションの呼び出しに変換する。 |
| Application Service | `SimulateGameUseCase` | ユースケースの流れ（指定回数の試合実行と集計）を調整し、個別の野球ルールは保持しない。 |
| Mapper | `LineUpMapper` | shared contract の `SimulationPlayerMessage` をドメインの `BatterEntity`/`LineUpEntity` に変換する。プレイヤー転送データの事情をドメインから隔離する。 |
| Strategy | `AtBatBehavior`、`StealStrategy`、`BuntStrategy` と各実装 | 打撃・盗塁・バントの判定アルゴリズムを交換可能にする。`BatterEntity` はインターフェースへ依存する。 |
| State | `BasesState` と各塁配置クラス | 塁上の走者配置をオブジェクトで表し、打撃・盗塁・犠打後の次状態を返す。状態遷移を明示的な値として扱う。 |
| Context | `GameBattingContext` | イニング、アウト、得点、現在の `BasesState`、打順を保持し、一打席ずつ試合を進める。State パターンの文脈では Context に当たる。 |
| Value Object | `BaseTransition`、`GameStatistics`、`ScoreStatistics` | プレー結果や統計値を値として受け渡す。不変な結果を返し、状態更新と得点加算を明示する。 |
| Accumulator | `ScoreAccumulator`、`GameStatisticsRecorder` | 多数の試合得点、または一試合中のプレー統計を逐次集計してスナップショットを生成する。 |

## Strategy: 選手の行動を差し替える

`BatterEntity` は、具体的な確率計算を知らずに各戦略へ委譲します。

```text
BatterEntity
  ├─ AtBatBehavior   -> Middle/Long/ShortDistanceBattingBehavior
  ├─ StealStrategy   -> Eager/Middle/NowayStealBehavior
  └─ BuntStrategy    -> Standard/Eager/NowayBuntStrategy
```

たとえば「盗塁をしない」選手は `NowayStealBehavior` を持ちます。呼び出し側で `stealEnabled` の分岐を繰り返す代わりに、戦略オブジェクトへ判断を委譲できます。新しい行動を追加するときは、既存の条件分岐を増やすより、対応する Strategy インターフェースの実装を追加し、`LineUpMapper` または設定から選択するのが基本です。

## State: 塁配置と遷移を表す

`BasesState` は走者なし、1・2塁、満塁などの塁配置を表します。`hitSingle` や `sacrificeBunt` などは、状態を破壊的に変更せず `BaseTransition` を返します。

```text
現在の BasesState
  -> プレーを適用
  -> BaseTransition(nextState, scoredRuns)
  -> GameBattingContext が得点を加算し、nextState に置換
```

これは GoF の State を厳密に実装するというより、State パターンの「状態と遷移を第一級にする」考え方を用いたモデルです。塁状態の遷移規則は `BasesState` 側、試合全体の可変状態（アウト、回、得点、打順）は `GameBattingContext` 側にあります。塁配置を追加・変更する場合は、遷移の不変性と `BaseTransition` の得点を一緒に検証します。

## Adapter: メッセージング境界

`SqsSimulationScheduler` は inbound と outbound の両方の adapter です。

1. request queue から `SimulationRequestMessage` を取得する。
2. `LineUpMapper` と `SimulateGameUseCase` を呼ぶ。
3. `SimulationResultMessage` を result queue へ送る。
4. 結果の送信が成功した後にだけ request message を削除する。

この delete-after-send の順序は少なくとも 1 回処理されうる SQS の再配送に備えるための重要な契約です。メッセージの JSON 変換やキュー名解決も adapter の責務であり、domain には置きません。

## 変更時の目安

- 野球の確率・プレー選択を変える: Strategy 実装を変更・追加する。
- 走者の進塁や得点を変える: `BasesState` と `BaseTransition` を変更する。
- 試合の進行順・試合回数を変える: `GameBattingContext` または `SimulateGameUseCase` を確認する。
- SQS の形式・配送処理を変える: messaging contract と `SqsSimulationScheduler` を確認する。wire type は application/domain に漏らさない。
- 集計項目を増やす: `GameStatisticsRecorder` または `ScoreAccumulator` に一回の走査で加算する。

## テストと検証

通常の変更では、まず対象クラスのテストを追加または更新し、その後に次を実行します。

```sh
../../.agents/skills/baseball-orders-development/scripts/verify.sh simulator
```

`ELASTICMQ_ENDPOINT_URL` を設定した場合は、ElasticMQ を使う SQS integration test も実行されます。
