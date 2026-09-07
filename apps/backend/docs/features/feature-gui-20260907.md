# 画面表示を改善する

status: done

## 1. Goal

- `backend` 画面のデフォルトで打順が入力された状態にする。
- `backend` 画面のシミュレーションロジックの仕組みを説明するページを追加する。

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
