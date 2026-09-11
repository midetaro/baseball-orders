# Backend のローカル起動

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

## ログイン

ローカル起動後、[http://localhost:8080/users/new](http://localhost:8080/users/new) からアカウントを作成し、[http://localhost:8080/login](http://localhost:8080/login) でログインできます。

- ユーザー名は英数字、`_`、`-` の3〜50文字です。個人名やメールアドレスは入力しないでください。
- パスワードは8〜72文字です。保存時には BCrypt ハッシュへ変換され、平文は保存しません。
- ログイン済みセッションには内部ユーザーIDだけを保持します。ログアウトはログイン後に `POST /logout` へCSRFトークン付きで送信します。
- `GET /login`、`POST /login`、`GET /users/new`、`POST /users` は匿名で利用できます。既存のシミュレーション画面と `POST /simulations` も匿名アクセスのままです。
- H2はインメモリDBのため、アプリケーションを再起動すると作成済みアカウントは消えます。

セッションCookieはHttpOnlyかつSameSite=Strictです。本番では `prod` プロファイルを有効にしてSecure属性も設定してください。

パスワード再設定、メールアドレスなどの個人情報、およびユーザー固有データAPIは未実装です。
