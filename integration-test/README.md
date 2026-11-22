# backend / simulator 疎通テスト

Dockerやクラウドの資格情報は不要です。リポジトリルートから実行します。

```sh
./gradlew --no-daemon --console=plain :integration-test:test
```

テストを再実行する場合は`--rerun-tasks`を追加します。

## 検証範囲

`BackendSimulatorGrpcIntegrationTest` は、simulatorとbackendを実際のSpring Bootアプリケーションとして起動します。
simulatorはポート0で待ち受け、割り当てられたポートをbackendのgRPC接続先に設定します。

- 実物: backendのHTTPサーバー、Controller、SimulationCoordinator、GrpcSimulatorClient、gRPCチャネル、simulatorのGrpcServerLifecycle・GrpcSimulationService・LineUpMapper・SimulateGameUseCase、localhost上のNetty/HTTP2。
- モック: なし。
- 担保する疎通: HTTP POST /simulations および /simulations/single-game -> backend -> gRPC -> simulator -> gRPC応答 -> HTTP応答(統計、1試合実行では状況推移)。
- 担保しないもの: 実環境のネットワーク・TLS、乱数の統計的正当性、gRPCの異常系(タイムアウト・接続断)、ブラウザ描画。

## アプリ全体の検証

```sh
./.agents/skills/baseball-orders-development/scripts/verify.sh all
```

このコマンドは両アプリの標準検証を実行します。上記の`:integration-test:test`は別途実行してください。
