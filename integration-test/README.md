# backend / simulator 疎通テスト

Docker Engineを起動した状態で、リポジトリルートから実行します。
初回はTestcontainersがFlociとRyukのイメージを取得するため、ネットワーク接続が必要です。

```sh
docker info
./gradlew --no-daemon --console=plain :integration-test:test
```

テストを再実行する場合は`--rerun-tasks`を追加します。
Dockerを利用できない場合はスキップせず失敗します。
SQSのエンドポイント環境変数やAWS実環境の認証情報は不要です。
Flociコンテナ、キュー、HTTPポートはテスト用に用意します。

## 検証範囲

`BackendSimulatorFlociIntegrationTest` は次の2つのシナリオを持ちます。

1. 実試合シナリオ: 試合数1の実 `SimulateGameUseCase` で、HTTP要求が同じ相関IDの応答として返ることを確認します。
2. 統計保持シナリオ: 乱数を使う `SimulateGameUseCase` だけを固定統計を返すfakeに置き換え、得点統計・安打内訳・
   本塁打内訳・バント/盗塁の内訳がsimulatorの結果SQSからbackendのHTTP応答まで欠けずに届くことを確認します。

- 実物: backendのHTTPサーバー、Controller、Coordinator、要求Publisher、結果Listener、WaitingResultRegistry、simulatorの`SqsSimulationScheduler`・`LineUpMapper`・JSONシリアライザ、Floci。
- モック: AWS SQSをFlociに置換。統計保持シナリオでは`SimulateGameUseCase`を固定統計を返すfakeに置換。
- 担保する疎通: HTTP POST /simulations -> backend -> 要求SQS -> simulator -> 結果SQS -> backendの自動Listener -> HTTP応答。
- 検証する値: HTTPステータス、要求と応答の相関ID、選手数、試合数と集計統計、応答後の待機レジストリ解放。結果が届く前にHTTP応答が完了しないことも確認します。
- 担保しないもの: AWSのIAM・ネットワーク、乱数の統計的正当性、simulatorの定期実行設定、異常系・再配信、1試合実行（`POST /simulations/single-game`）の経路、ブラウザ描画。simulatorの`poll()`はテストから直接呼び出します。

## アプリ全体の検証

```sh
./.agents/skills/baseball-orders-development/scripts/verify.sh all
```

このコマンドは両アプリの標準検証を実行します。上記の`:integration-test:test`は別途実行してください。
アプリ内の条件付きSQSテストには、backendは`SQS_ENDPOINT`、simulatorは`ELASTICMQ_ENDPOINT_URL`が必要です。
