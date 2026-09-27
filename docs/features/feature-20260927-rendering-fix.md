# Feature name

Status: in_progress

## Goal
- トップ画面は、1試合実行ではなく、大規模実行の方に変更
- 1試合実行のアニメーションのアウトカウントは赤色
- 画面のブンブン丸を廃止して、長距離砲に変更

## In scope
- backend

## Out of scope
- simulator
- messaging-contract

## Acceptance criteria
- `/` は大規模実行画面を表示し、`/single-game` から1試合実行を利用できる。
- 1試合実行アニメーションのアウト表示が赤色になる。
- 入力画面とガイドの性格表示を「長距離砲」に統一する。

## Verification (2026-09-27)
- 対象テスト: `SimulationPageControllerTest` 3件、`SimulationPageIntegrationTest` 6件成功。
- `node --test apps/backend/infrastructure/src/test/js/*.test.mjs`: 3件成功。
- `./.agents/skills/baseball-orders-development/scripts/verify.sh backend`: 終了コード0。
- backend のアーキテクチャテスト成功。Gradle モジュール依存の変更なし。
- HTTP疎通は実物の認証・コントローラー・Thymeleafを使用し、SqsTemplateはモック。実SQS・simulatorを含む疎通は今回未実行。
- 独立したコードレビューで今回の実装に不具合の指摘なし。
- 未検証: PC 1280×800・スマートフォン390×844での実画面のスクロール・見切れ確認。ブラウザ接続先がなく実行できず、画面レビューと最終完了判定は保留。
- rendering-idea の作業は、この画面レビュー完了後に開始する。
