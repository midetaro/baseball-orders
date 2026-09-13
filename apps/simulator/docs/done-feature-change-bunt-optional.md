# Feature: バントするしないを選択制にする

status: done

## 1. Goal

`simulator` キューの選手データにバントの有無を追加して、有のときだけ、バントロジックを実践する

---
## 3. Input
- キューの選手データにバントの有無を追加

## 4. Output
- 結果のキューは変わらない

## 5. Functional Requirements
- バントロジックの判定を追加
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
