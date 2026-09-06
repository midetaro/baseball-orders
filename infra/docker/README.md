# Flociを使うローカルアプリ環境

Docker Desktop / Docker EngineとDocker Composeを起動し、リポジトリルートで実行します。
JavaとGradleはDocker内で用意されるため、ホストへのインストールは不要です。
初回はイメージとGradle依存ライブラリのダウンロードが必要です。

```sh
docker compose up -d --build --wait --wait-timeout 180
```

http://127.0.0.1:8080/ を開くと、打者一覧・打順設定・シミュレーションを利用できます。
既存のJavaプロセスなどが8080を使っている場合は、`BACKEND_PORT=18080`をコマンドの前に付け、
http://127.0.0.1:18080/ を開いてください。

## 構成

- `floci`: Floci 2.0.1。AWS SQSをローカルで代替します。
- `queues`: Flociの正常起動後に要求・結果キューを作成し、正常終了する初期化コンテナ。
- `simulator`: 本番の試合計算を行う独立したJavaプロセス。SQSを自動ポーリングします。
- `backend`: HTTP/UIとH2。要求をSQSへ送信し、結果リスナーが受信した結果をHTTPで返します。

Backend -> 要求SQS (Floci) -> Simulator -> 結果SQS (Floci) -> Backend

Dockerでは明示的に`docker`プロファイルを指定し、Backendの`local`プロファイルによる
リスナー停止を回避します。Simulatorは1要求につき1試合を計算し、ポーリング間隔は1秒です。
`SIMULATION_GAME_COUNT`と`SIMULATION_SQS_POLL_FIXED_DELAY`はCompose内で変更できます。

実AWSの認証情報は不要です。コンテナにはローカル専用の`test`認証情報を渡します。
FlociはDocker内部の`http://floci:4566`に配置し、ホストにはBackendのみを公開します。
`FLOCI_HOSTNAME`を指定して、返却されるSQS QueueUrlもコンテナ間で解決できるようにします。
[Floci公式ドキュメント](https://github.com/floci-io/floci#multi-container-docker-compose)

キュー名を変更する場合は、起動時に`SIMULATION_REQUEST_QUEUE_NAME`と
`SIMULATION_RESULT_QUEUE_NAME`を設定します。初期化と両アプリに同じ値が渡ります。

データは開発用の一時データです。BackendのH2はプロセス終了で、Flociのキューとメッセージは
Flociプロセス終了で失われます。起動時にH2の初期データとキューを再作成します。
Flociだけを再起動するとキューが失われるため、環境全体を`down`してから`up`してください。

## 疎通確認

Python 3があるホストで実行します。画面のHTTP 200と、9選手の実シミュレーション応答を検証します。
定期ポーリングも本番実装が自動で実行し、テストから`poll()`を直接呼びません。

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
docker compose logs --tail=100 backend simulator queues
docker compose down
```

`queues`の`Exited (0)`は正常です。ソース変更を反映する場合は、再度`up -d --build --wait`を実行します。
初回ビルド後はDockerのGradleキャッシュを再利用します。

既存の各アプリの`infrastructure/compose.yaml`は、条件付きElasticMQテスト用です。
アプリをまとめて動かす場合は、リポジトリルートの`compose.yaml`を使用してください。

## 画面でシミュレーション送信に失敗する場合

`simulation request could not be sent: {UUID}` が表示される場合は、まず画面のURLが
`http://127.0.0.1:8080/`（ポート変更時はそのポート）になっているか確認してください。
ComposeはIPv4の`127.0.0.1`に公開しています。`localhost`はIPv6の`::1`に解決されることがあり、
IDEなどで起動した別のJavaプロセスが同じポートで待ち受けていると、そちらに接続してしまいます。
疎通テストが成功するのに画面で失敗する場合も、画面を上記のIPv4アドレスで開き直してください。
