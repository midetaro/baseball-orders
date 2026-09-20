done

総評は「モジュール分割は良好、パッケージ境界は再整理が必要」です。評価は 6/10 程度です。実行時の依存方向は概ね健全ですが、パッケージ移動後の残骸とフレームワーク依存が目立ちます。

## 主な指摘

1. 高: JaCoCoの対象パッケージが古く、カバレッジ検証が実質的に無効です。

domain/build.gradle:35 は以下を対象にしています。

- domain.model.behavior.*
- domain.model.state.*

現在の実装は domain.entity.behavior.* と domain.model.base.* です。対象クラスが存在しないため、先ほどの検証成功だけではこれらの100%カバレッジを保証できません。これは機械的に確認済
みで、最優先の修正候補です。

2. 中: domainがSpringに依存しています。

domain/src/main/java/com/example/baseballorders/simulator/domain/model/base/BaseStateFactory.java:4 や各戦略クラスが @Component を持ち、domain/build.gradle:10 もSpring Contextへ依
存しています。

ドメインロジック自体はSpringを必要としていないため、Bean登録をinfrastructure側の設定へ移すと、domainを純粋なJavaモデルとして維持できます。

3. 中: applicationにインフラ設定が置かれています。

application/src/main/java/com/example/baseballorders/simulator/application/config/SimulationApplicationConfiguration.java:3 が ObjectMapper、AWS SqsClient、スケジューリングを設定
しています。これらはすべてSQSアダプターの構成要素なので、infrastructure.config が自然です。

同様に、application/src/main/java/com/example/baseballorders/simulator/application/mapper/LineUpMapper.java:3 はSQS共有メッセージを直接入力にしています。境界マッパーとしては規約内
ですが、責務上は infrastructure.messaging に置く方がapplicationをトランスポート非依存にできます。

## 良い点

- Gradleモジュールの依存方向は infrastructure → application → domain で、逆方向依存はありません。
- SQSのデシリアライズとシリアライズはinfrastructureに閉じています。
- domainはmessaging-contractへ依存していません。
- model.base.capability はバント・盗塁能力を明確に表現しており、凝集度が高いです。
- applicationの結果型とSQS結果メッセージは分離されています。