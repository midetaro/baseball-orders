# Feature name

Status: done

## Goal
- テストとレビューを通過すること

## In scope
- simulator/domain
- simulator/application

## Out of scope
- backend
- simulator/infrastructure

## Current behavior
- simulatorのAbstractBasesStateがinningStateを保持しており可読性が悪い

## Expected behavior
- simulatorのAbstractBasesStateからinningStateを切り出す
- GameBattingContextはフィールドとしてInningStateContextを持つ
- 各ConcreteStateクラスはInningStateContextがフィールドとして保持する。GameBattingContextは各Stateを直接保持しない
- AtBatProcessorが更新するのはInningStateContextであり、GameBattingContextではない

## Acceptance criteria
