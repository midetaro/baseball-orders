status: done

## 変更概要
- シミュレーションのデータをbackendのテーブルに保持せず、画面から入力させる

## 変更の詳細
- ./faeture-20260913-3.mdを満たすように、simulatorロジックを修正する
  - 盗塁・バントはオプショナルとして設定する
- apps/backendとapps/simulatorを両方修正する。
- apps/simulator側でキューの値がnot nullであるバリデーションを実施する

## 実装の方針
- agents.mdに従う

## 完了条件
- baseball-orders-reviewのskillを満たす

## Deterministic verification

- Build: PASS。`verify.sh all` でbackend・simulatorのビルド成功、最終exit code 0。
- Unit tests: PASS。盗塁・バントの4通りの選択、SQS必須値のnull・欠落22ケースを検証。実装前の失敗も確認。
- Integration tests: PASS。HTTP結合テストとFlociを利用したSQS結合テストを実行。条件付きテストのスキップなし。
- Module dependencies unchanged: PASS。Gradle/settingsの差分なし。外部ライブラリの変更もなし。
- Required contract checks: PASS。選択値の送信・受信、相関ID維持、結果送信後の要求削除をテストで確認。
- simulatorのカバレッジ検証および `git diff --check`: PASS。

## Integration test boundary

実物:
- HTTPサーバー、Controller、Coordinator、WaitingResultRegistry、結果Listener、JSON変換。
- SQS側はFloci、Scheduler、ObjectMapper、LineUpMapper、打者。

モック:
- HTTP結合テスト: 要求送信ポート、無効化されたSQS用のSqsTemplate。
- SQS結合テスト: SimulateGameUseCase、固定の打撃・盗塁・バント戦略。

担保する疎通:
- HTTP JSON -> Controller -> Coordinator -> 結果Listener -> WaitingResultRegistry -> HTTP JSON応答。
- request SQS -> Scheduler -> LineUpMapper -> 打者の選択反映。
- request SQS -> Scheduler -> result SQS -> 共有結果メッセージ、および要求削除。

担保しないもの:
- 単一テストでのHTTPからsimulatorを含む全経路、AWS実環境、ブラウザ描画。
- SQS結合テスト内での試合計算の正当性（既存domainテストは別途実行）。

## Findings

### BLOCKING

- なし。

### IMPORTANT

- なし。手動レビューでレイヤー境界、共有contractの配置、変更範囲を確認。

### MINOR

- なし。

## Acceptance Criteria

- [x] 画面入力を使う既存の非永続化フローを維持し、backend・simulator両方を修正。
- [x] 盗塁・バントの実行有無を独立して選択可能。
- [x] simulatorの受信時に必須値のnull・欠落を拒否し、不正要求を削除しない。
- [x] 両アプリの検証とbaseball-orders-reviewを完了。

## Verdict

PASS

「オプショナル」は実行有無の選択と解釈し、成功率を含む入力項目は必須のままとした。
共有メッセージに `stealEnabled` を追加したため、この値を含まない旧要求は拒否される。
運用時はbackendとsimulatorの双方を更新すること。
