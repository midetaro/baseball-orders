# Feature name

Status: done

## Goal
- 試合の詳細な挙動を把握する機能を作成する
- 画面には、既存の大規模実行と1試合実行の二つに分ける
- 初期のページは詳細実行のページとする
- 1試合実行か、既存の大規模実行機能かを見分けるために、SQSのキューに新規のEnumを追加する
- 1試合実行のとき、resultのSQSは一試合の状況推移のリストを返却する。全ての推移にはイニング、打者 or 走者の振る舞い結果、アウトカウント、累積得点、走者状況をもつこと。
- simulatorのobserverに、1試合実行用のconcreteクラスを追加する。
- backend側で、返却された試合の推移を画面にテキストで描画する。
- なお、画面での描画の方法は後ほど検討するので、一旦文字列を表示すればよい。

## In scope
- simulator
- backend
- libs/message-contract

## Out of scope

## Acceptance criteria
