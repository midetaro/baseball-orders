# Docker疎通テスト完成確認（2026-09-05）

`baseball-orders-review`に従ったレビュー。依頼は既存の`integration-test`の疎通確認と必要な修正。
個別の仕様書は指定されていないため、既存テストのHTTP/SQS往復と相関ID検証を受け入れ条件とした。
変更範囲は共有ライブラリのビルド出力先分離と実行手順の追加。
開始時から存在する各アプリのgroup変更、ルートbuild.gradle/settings.gradleのintegration-test組み込み、テスト本体は既存構成として保持した。
前回のモジュール追加の指摘はHEADとの差分全体に対するもの。今回の変更で依存関係を追加・承認したものではない。

## Deterministic verification

- Build: PASS。SQS接続先を設定した`verify.sh all`がexit code 0。
- Unit tests: PASS。両アプリのテストXML集計はbackend 29件、simulator 132件、失敗・エラー・スキップはいずれも0件（統合テストを含む総数）。
- Integration tests: PASS。`:integration-test:test --rerun-tasks`成功。標準検証後、`:integration-test:test --tests '*BackendSimulatorFlociIntegrationTest'`でも成功。条件付きSQSテスト3件もElasticMQ 1.6.16で実行済み。
- Module dependencies unchanged: PASS（作業開始時との比較）。変更したGradle設定は`layout.buildDirectory`のみ。外部ライブラリの変更なし。
- Required contract checks: PASS。実行した疎通テストが要求・HTTP応答のsimulationId一致、選手数、得失点、待機解放をassert。
- 共有JAR: PASS。backend/simulator別の出力JARを開き、双方にSimulationRequestMessage/SimulationResultMessageクラスが存在することをスクリプトでassert。
- 整形・カバレッジ: PASS。標準検証のSpotlessおよびsimulatorのjacocoTestCoverageVerification成功。
- 差分空白チェック: PASS。`git diff --check`成功。

再現した不具合は、アプリ単独ビルド後の複合ビルドで共有JARが空になり、JUnitの探索がNoClassDefFoundErrorで失敗すること。
テスト本体の振る舞いのredではなく、ビルド成果物の不具合として扱った。
出力先分離後は同じ実行順序とキャッシュ利用で成功した。

## Integration test boundary

実物: backend HTTPサーバー、Controller、Coordinator、H2、要求Publisher、結果Listener、WaitingResultRegistry、simulator SQSアダプター、LineUpMapper、SimulateGameUseCase、Floci。

モック: AWS SQSをFlociに置換。打撃・盗塁・バント戦略を固定結果の実装に置換。

担保する疎通: HTTP -> backend -> 要求SQS -> simulator -> 結果SQS -> backend Listener -> HTTP応答。

担保しないもの: AWS実環境、乱数の統計的正当性、定期実行設定、異常系・再配信。pollはテストから直接呼ぶ。

## Findings

### BLOCKING

今回の変更範囲ではなし。

### IMPORTANT

なし。

### MINOR

なし。

手動レビュー: 疎通経路のアダプターをモックしていないこと、固定戦略の置換が日本語コメントに記載されていること、リソースをtry-with-resourcesで閉じることを確認。
AWS実環境と定期実行設定はNOT VERIFIEDであり、今回の対象外。

## Acceptance Criteria

- [x] Docker上のFlociでHTTP/SQS往復と相関IDの一致を確認。
- [x] 通常の並列・キャッシュ設定、およびアプリ単独ビルド後に疎通テスト成功。
- [x] 両アプリの標準検証成功、条件付きSQSテストのスキップなし。
- [x] 今回の変更ではモジュール関係・外部依存関係を変更しない。
- [x] 実行手順と検証範囲をREADMEに記載。

## Verdict

PASS（上記の変更範囲・受け入れ条件）。
