# Feature name

Status: done

## Goal
- SQSのSimulationResultMessageキューのStatisticsクラスを、GameScoreStatisticsクラスとGameContentStatisticsクラスに分割する
- テストのskillを修正。テスト対象のクラスは`sut`という一時変数名にする
- テストのskillを修正トークン節約のためtestとレビューのskillは、セッション中に変更したクラスを対象とするクラスのみを検証対象とする、

## In scope
- simulator
- backend

## Out of scope
- thymeleafの画面

## Current behavior
- SQSのSimulationResultMessageキューのStatisticsクラスの可読性が悪い

## Expected behavior
- GameScoreStatisticsとGameContentStatisticsに分けて可読性と保守性を向上させる
- 結果SQSメッセージはスキーマバージョン2として新形式へ一斉切替する。旧形式との混在は対象外とする

## Acceptance criteria
- 結果メッセージの得点5項目とプレー内容15項目が別々の型とJSONプロパティに分かれる
- simulatorから結果SQSを経由してbackendのHTTP応答まで、全統計値と相関IDが維持される
- テスト用skillはテスト対象インスタンスに`sut`を使い、test・reviewのクラス単位の対象をセッション中に変更したクラスへ限定する
