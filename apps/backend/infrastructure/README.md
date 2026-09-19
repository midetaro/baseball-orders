# Backend のローカル起動

## DockerでBackendだけを起動する

Docker Desktop / Docker EngineとDocker Composeを起動し、`apps/backend` で実行します。
JavaとGradleはDocker内で用意されるため、ホストへのインストールは不要です。

```sh
docker compose -f infrastructure/compose-backend.yaml up -d --build --wait --wait-timeout 180
```

http://127.0.0.1:8080/ を開いてください。8080が使用中の場合は、`BACKEND_PORT=18080` を
コマンドの前に付け、http://127.0.0.1:18080/ を開きます。

この構成はBackendコンテナだけを起動し、`local` プロファイルによりSQSリスナーを停止します。
打者一覧と打順設定を確認できますが、SimulatorとSQSを起動しないため、シミュレーション実行はできません。
シミュレーションを含めて動かす場合は、[ローカルDocker環境](../../../infra/docker/README.md)を使用してください。

停止するには、次を実行します。

```sh
docker compose -f infrastructure/compose-backend.yaml down
```

Backend・Simulator・FlociをまとめてDockerで動かす場合は、
[ローカルDocker環境](../../../infra/docker/README.md)を参照してください。

AWS 接続なしで打者一覧と打順設定画面を確認する場合、`apps/backend` で実行します。

```sh
./gradlew :infrastructure:bootRun
```

http://localhost:8080/ を開いてください。

プロファイル未指定（IDE からの `BackendApplication.main` 実行を含む）では `local` が使われ、SQS リスナーの自動起動を停止します。
シミュレーションの実行には SQS と simulator が必要です。
AWS を使う場合は `SPRING_PROFILES_ACTIVE=aws` を明示し、AWS 認証情報とキューを設定してください。
キュー名は `SIMULATION_REQUEST_QUEUE_NAME` と `SIMULATION_RESULT_QUEUE_NAME` で指定できます。
