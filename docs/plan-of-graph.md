このリポジトリで、Codexのsubagentを利用したGraphエンジニアリングを行うための設定ファイルを作成してください。
機能開発を次のGraphとして実行できるようにします。
1. 要件と既存コードを調査する
2. 独立した作業を専門subagentへ分割する
3. 依存しない作業を並列実行する
4. worker完了後に統合する
5. 実装者とは別コンテキストのreviewerで検証する
6. 最後に全体テストを実行する
   今回はGraphの設定・指示ファイルを作成することが目的です。アプリケーション機能自体は実装しないでください。
   ファイルを変更する前に、以下を調査してください。
- リポジトリ全体のディレクトリ構成
- Gradleのルート、subproject、composite build構成
- 既存のAGENTS.md
- 既存の.codex/config.toml
- 既存の.codex/agents/*.toml
- simulatorとbackendの実際のモジュール構成
- 各モジュールの依存方向
- 利用可能なテストコマンド
- Thymeleaf画面の配置場所
- messaging-contractなどの共有モジュールの有無
- 既存の設計書や仕様書
  既存ファイルがある場合は上書きせず、内容を維持しながら必要な指示を統合してください。
  調査結果と実際の構成が以下の想定と異なる場合は、実際のリポジトリを優先してください。ただし、モジュール構成や依存方向を勝手に変更してはいけません。
- simulatorは独立したアプリケーションである　
- backendは- Thymeleaf画面の機能をもち、simulatorにSQSリクエストを送信する
- simulatorとbackendは、それぞれGradleマルチモジュール構成である
- simulatorとbackendをクラスレベルで直接依存させない
- 共有モジュールが存在する場合、共有対象は通信契約など既存責務の範囲に限定する
- domain層からSpring、Thymeleaf、SQS、DBなどの技術詳細を参照しない
- 既存のモジュール構成と依存方向を変更しない
- ListenerやControllerに業務ロジックを書かない
- 同じ乱数seedと同じ入力から、同じシミュレーション結果を再現できるようにする
- 不明点を想像で補って新しいアーキテクチャを導入しない
  実際のリポジトリ構成に合わせて、最低限以下を作成または更新してください。
```
AGENTS.md
.codex/config.toml
.codex/agents/explorer.toml
.codex/agents/domain-worker.toml
.codex/agents/backend-worker.toml
.codex/agents/ui-worker.toml
.codex/agents/integrator.toml
.codex/agents/reviewer.toml
simulator/AGENTS.md
backend/AGENTS.md
docs/features/TEMPLATE.md
docs/codex-graph.md
```
ディレクトリ名が異なる場合は、実際の構成に合わせて配置してください。
不要なagentを無理に作らないでください。ただし、省略する場合は理由を報告してください。
ルートのAGENTS.mdには以下を含めてください。
- アプリケーション間とモジュール間の責務
- 許可されている依存方向
- 禁止されている依存
- 既存構成を変更してはならないこと
- 既存コードとユーザーの変更を保護すること
- 実装前に関連コードとテストを調査すること
  複数領域にまたがる機能では、以下の順序で進めるようにしてください。
1. explorerが影響範囲と依存関係を調査する
2. 親エージェントが実行Graphを決定する
3. 依存しないworkerを並列実行する
4. 依存するworkerは上流Nodeの完了を待つ
5. 全worker完了後にintegratorを実行する
6. reviewerを新しいコンテキストで実行する
7. 指摘修正後に全体テストを実行する
   以下も明記してください。
- 同一ファイルを複数agentに同時編集させない
- 各agentに書き込み可能範囲を割り当てる
- 並列化できることと、並列化すべきことを区別する
- 小規模な変更では無理にsubagentを使用しない
- 調査、テスト、レビューなど読み取り中心の作業を優先して並列化する
- 親エージェントは全subagentの完了を待ってから統合する
- agentの失敗や未完了を成功として扱わない
  各workerは次のLoopを最大5回まで実行するようにしてください。
1. 実装
2. 担当モジュールのテスト
3. 失敗原因の特定
4. 最も根本的な原因を1つ修正
5. 再テスト
   以下の停止条件を設定してください。
- テストと受入条件をすべて満たした
- 最大5回に達した
- 担当範囲外の変更が必要になった
- 仕様が曖昧で結果が変わる重要な判断が必要になった
  すべてのworkerに、以下の形式で親エージェントへ報告させてください。
```
## Result

- Status: completed | blocked | failed
- Files changed:
- Tests executed:
- Test result:
- Decisions:
- Remaining risks:
- Required follow-up:
```
実際のモジュール名に合わせて、以下を定義してください。
- domainの担当範囲
- application/usecaseの担当範囲
- infrastructureの担当範囲
- presentation/Thymeleafの担当範囲
- 各層で禁止される依存
- 利用するテストコマンド
- シミュレーションの決定性に関するルール
  既存コードからbackendの実際の責務を特定し、以下を定義してください。
- domain、application、infrastructure、presentationの責務
- simulatorへの直接依存禁止
- 通信DTOと内部モデルを分離すること
- 利用するテストコマンド
- backendにシミュレーションロジックを書かないこと
  backendの責務がコードから判断できない場合は、推測で追加せず、確認できた事実だけを書いてください。
  既存設定を維持しながら、subagentを利用できる設定を追加してください。
  原則として以下を設定してください。
```
[agents]
enabled = true
max_concurrent_threads_per_session = 4
```
既存の[agents]設定がある場合は、重複セクションを作らず統合してください。
プロジェクト事情から別の同時実行数が適切な場合は変更できますが、理由をdocs/codex-graph.mdへ記録してください。
各.codex/agents/*.tomlには、最低限以下を設定してください。
```
name = "..."
description = "..."
developer_instructions = """
...
"""
```
必要に応じて、model_reasoning_effortやsandbox_modeも設定してください。
- 読み取り専用
- コードを変更しない
- 実行経路、依存関係、関連テストを調査する
- 並列化できる作業と、本当に依存する作業を分ける
- 変更候補ファイルと競合可能性を報告する
- simulatorのドメインロジックを担当する
- 純粋なJavaとして実装する
- Spring、SQS、Thymeleaf、DBへ依存しない
- 業務ルールと境界値を単体テストする
- 担当外のモジュールを変更しない
- backendの既存責務内だけを担当する
- simulatorへ直接依存しない
- simulatorの業務ルールを実装しない
- 通信DTO、Entity、domain modelを混同しない
- 担当テストを実行する
- backendのController、Form、ViewModel、Thymeleafを担当する
- テンプレートへdomain objectやEntityを直接公開しない
- 業務ルールをControllerやテンプレートへ実装しない
- 入力、バリデーションエラー、結果表示をテストする
- すべての必要なworkerが完了してから実行する
- workerの成果物を接続する
- 新しい業務ルールを追加しない
- 統合テストを担当する
- 失敗をdomain、contract、backend、presentation、integrationに分類する
- 担当外の問題を無断で修正せず、親エージェントへ返す
- 読み取り専用にする
- 実装を変更しない
- workerの会話履歴ではなく、仕様、diff、テスト結果を基に検証する
- 正しさ、アーキテクチャ、回帰、テスト不足を確認する
- 指摘には重要度、根拠、対象ファイルを含める
- 問題がない場合も、確認した項目を明記する
  docs/features/TEMPLATE.mdには、少なくとも以下のセクションを作成してください。
```
# Feature name

## Goal

## Background

## In scope

## Out of scope

## Current behavior

## Expected behavior

## Acceptance criteria

## Affected applications

## Constraints

## Test scenarios

## Decisions

## Open questions
```
受入条件は、可能な限りテストで成功・失敗を判定できる形式で書くよう注記してください。
docs/codex-graph.mdには以下を記載してください。
- このリポジトリにおけるGraphエンジニアリングの目的
- 各agentの役割
- 標準的な実行順序
- 並列実行できるNodeの判断方法
- 実際のディレクトリに対応したファイル所有範囲
- 各Nodeが実行するテストコマンド
- 統合のタイミング
- reviewerの使い方
- 小規模変更でGraphを使わない基準
- トークン消費とファイル競合への注意
- Feature仕様書を使った実行例
- Codexへ渡す実行プロンプトの例
  実行例には、次のような依頼文を含めてください。
```
docs/features/<feature>.mdを実装してください。

AGENTS.mdに従い、最初にexplorerで影響範囲を調査してください。
調査結果から依存Graphを作り、独立したNodeだけを並列実行してください。
各workerの完了後にintegratorを実行し、最後に新しいコンテキストのreviewerで検証してください。
同一ファイルを複数agentへ同時編集させず、すべての結果を待ってから最終報告してください。
```
- 既存の未コミット変更を破棄しない
- 関係のないファイルを変更しない
- モジュールを追加、削除、移動しない
- 依存方向を変更しない
- 外部ライブラリを追加しない
- Git commit、push、branch作成を行わない
- テスト失敗を隠さない
- 実在しないGradle taskを記載しない
- 調査で確認できなかったコマンドを確定事項として書かない
- 既存のAGENTS.mdや.codex設定を丸ごと置き換えない
  重要な判断が必要で、既存コードや設計書から決められない場合だけ質問してください。軽微な命名や配置については、既存規約に合わせて合理的に判断し、その判断を報告してください。
  ファイル作成後、以下を実施してください。
1. 作成・変更したファイルをすべて再読する
2. TOMLの構文を検証する
3. agent名に重複がないことを確認する
4. 記載したパスが実在することを確認する
5. 記載したGradle taskが実行可能か確認する
6. AGENTS.md間で指示が矛盾していないことを確認する
7. 既存のモジュール依存方向を変更していないことを確認する
8. 可能なら設定ファイルを利用した読み取り専用のsubagent起動を1回だけ確認する
   最後に、以下を簡潔に報告してください。
- 作成したファイル
- 更新したファイル
- 定義したGraph
- 各agentの担当範囲
- 実行した検証
- 検証結果
- 判断できず保留した事項
- 次にFeatureを実装するときに使用するプロンプト