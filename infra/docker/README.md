# ローカルアプリ環境（gRPC）

Docker Desktop / Docker EngineとDocker Composeを起動し、リポジトリルートで実行します。
JavaとGradleはDocker内で用意されるため、ホストへのインストールは不要です。
初回はイメージとGradle依存ライブラリのダウンロードが必要です。

```sh
docker compose up -d --build --wait --wait-timeout 180
```

http://127.0.0.1:8080/ を開くと、大規模実行（`/`）・1試合実行（`/single-game`）・シミュレーションの仕組み（`/simulation-guide`）の各画面を利用できます。
既存のJavaプロセスなどが8080を使っている場合は、`BACKEND_PORT=18080`をコマンドの前に付け、
http://127.0.0.1:18080/ を開いてください。

## 構成

- `simulator`: 本番の試合計算を行う独立したJavaプロセス。gRPCサーバーとして`9090`番ポートで待ち受けます（Dockerネットワーク内のみに公開）。
- `backend`: HTTP/UI。要求をgRPCの`SimulationService/Simulate`で`simulator`へ送り、応答をHTTPで返します。

HTTP -> Backend -> gRPC (unary) -> Simulator -> gRPC応答 -> Backend -> HTTPレスポンス

Dockerでは明示的に`docker`プロファイルを指定します。Backendは`SIMULATOR_GRPC_HOST=simulator`、
`SIMULATOR_GRPC_PORT`（既定9090）でSimulatorへ接続します。Simulatorの試合数は
`apps/simulator/infrastructure/src/main/resources/application.yml`の
`simulation.game-count`（既定100、環境変数`SIMULATION_GAME_COUNT`）に従います。変更後はSimulatorを再ビルド・再作成してください。
待ち受けポートは`SIMULATION_GRPC_PORT`で変更でき、Compose内の両サービスに同じ値が渡ります。

Composeが試合数を上書きしないこと、およびキューを持たないgRPC構成であることは、`python3 infra/docker/test-compose-config.py`で検証できます。

メッセージ基盤やAWS認証情報は不要で、ホストにはBackendのみを公開します。

## 疎通確認

Python 3があるホストで実行します。画面のHTTP 200と、9選手を`POST /simulations`へ送った実シミュレーション応答（相関ID、試合数、得点分布）を検証します。

```sh
python3 infra/docker/smoke-test.py
```

ポートを変えた場合は同じ`BACKEND_PORT`を指定してください。

```sh
BACKEND_PORT=18080 python3 infra/docker/smoke-test.py
```

## 状態・ログ・停止

```sh
docker compose ps -a
docker compose logs --tail=100 backend simulator
docker compose down
```

ソース変更を反映する場合は、再度`up -d --build --wait`を実行します。
初回ビルド後はDockerのGradleキャッシュを再利用します。

アプリをまとめて動かす場合は、リポジトリルートの`compose.yaml`を使用してください。

## 画面でシミュレーション呼び出しに失敗する場合

`simulation request could not be sent: {UUID}`（HTTP 502）が表示される場合は、まず画面のURLが
`http://127.0.0.1:8080/`（ポート変更時はそのポート）になっているか確認してください。
ComposeはIPv4の`127.0.0.1`に公開しています。`localhost`はIPv6の`::1`に解決されることがあり、
IDEなどで起動した別のJavaプロセスが同じポートで待ち受けていると、そちらに接続してしまいます。
疎通テストが成功するのに画面で失敗する場合も、画面を上記のIPv4アドレスで開き直してください。
