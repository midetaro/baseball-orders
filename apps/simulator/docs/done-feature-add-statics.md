# Feature: SQS経由でシミュレーション要求を受信し結果を返却する

status: done

## 1. Goal

`simulator` が シミュレーションを指定の全試合分実行した後、試合ごとの得点の統計情報を計算し、その結果をsimulation resultキューに追加する。それをbackendが受け取り画面に表示する。

---

## 2. Behavior

実装案
- domainに統計情報エンティティと統計情報計算機を追加する
- application/usecaseが統計情報計算機を呼び出して統計情報を取得
- sqsのキューに設定

## 4. Output
- 計算した統計情報をキューに含む

## 5. Functional Requirements
- 統計情報は、平均、中央値、最大得点の三つをもつ
- シミュレーション処理またはResult送信に失敗した場合、例外を無条件に握りつぶさないこと。

失敗したmessageを正常処理済みとして扱わないこと。

---

## 6. Architecture Constraints

以下を禁止する。

* `backend` moduleへの依存追加
* 既存module間依存関係の変更
* backend domain classの直接利用
* HTTP Controllerの追加
* H2/JPAの追加
* RabbitMQの追加
* Kafkaの追加
* simulator内部への永続DB追加

外部OSS/libraryの追加は、既存のmodule依存関係を変更しない範囲で必要な場合のみ許可する。

---

## 7. Deterministic Verification

決定的に検証可能な要件は、コードレビューだけで判断せず自動的に検証する。

### DV-1 Build

```bash
./gradlew test
```

が成功すること。

## 8. Testing Requirements

### Unit Test

以下をUnit Testで担保する。

* シミュレーションを含まない、統計情報専用の計算ロジック
* message/domain変換
* application処理

## 10. Out of Scope

以下はこのFeatureでは実装しない。

* DLQ
* custom retry policy
* strict idempotency
* autoscaling
* Docker image作成
* ECS
* Terraform
* backend側のResult受信
* Thymeleaf
* Polling
* SSE
* WebSocket
* unrelated refactoring

---
