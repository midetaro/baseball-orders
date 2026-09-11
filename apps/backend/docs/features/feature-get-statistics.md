# Feature: SQS経由でシミュレーション要求を受信し結果を返却する

status: done

## 1. Goal

`backend` simulation resultキューに存在する統計情報を、backendが受け取り、画面に表示する。

---

## 2. Behavior

実装案
- sqsのキューから受け取る
- キューに対してバリデーションを行う
- バリデーションが問題なければ統計情報を画面に表示する

## 5. Functional Requirements
- 統計情報は、平均、中央値、最大得点の三つがnullではないこと
- シミュレーション処理またはResult送信に失敗した場合、例外を無条件に握りつぶさないこと。

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

* 既存のテストのassertion項目に統計情報を追加

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
