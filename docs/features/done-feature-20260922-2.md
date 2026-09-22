# Feature name

Status: done

## Goal
- バント、盗塁を画面上に本塁打の統計情報と同様に色を分けてレンダリングする
- 本塁打の統計情報を参考に、バントと盗塁を詳細化。 
- 盗塁は二盗・三盗・失敗に分類 
- バントは進塁・スクイズ・進塁失敗・スクイズ失敗に分類
- 満塁で四球が発生したとき、得点を追加する

## In scope
- simulator,backend,simulation-resultのキュー
- 画面

## Out of scope

## Current behavior
- 盗塁・バントの成功・失敗しかない
- 満塁で四球が発生したとき、得点が追加されない

## Expected behavior
- バント、盗塁を画面上に本塁打の統計情報と同様に色を分けてレンダリングする
- 本塁打の統計情報を参考に、バントと盗塁を詳細化。
- 盗塁は二盗・三盗・失敗に分類
- バントは進塁・スクイズ・進塁失敗・スクイズ失敗に分類
- 満塁で四球が発生したとき、得点を追加する

## Acceptance criteria
- baseball-orders-reviewをクリアすること
