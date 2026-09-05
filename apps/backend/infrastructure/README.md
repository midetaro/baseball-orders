# Backend のローカル起動

AWS 接続なしで打者一覧と打順設定画面を確認する場合、`apps/backend` で実行します。

```sh

```

http://localhost:8080/ を開いてください。

`local` プロファイルでは SQS リスナーの自動起動を停止します。
シミュレーションの実行には SQS と simulator が必要です。
AWS を使う場合は `local` を指定せず、AWS 認証情報とキューを設定してください。
キュー名は `SIMULATION_REQUEST_QUEUE_NAME` と `SIMULATION_RESULT_QUEUE_NAME` で指定できます。
