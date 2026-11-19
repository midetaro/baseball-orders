import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { test } from 'node:test';

const root = new URL('../../../main/typescript/', import.meta.url);
const sources = readdirSync(root, { recursive: true })
  .filter(path => path.endsWith('.ts'))
  .map(path => ({ path, text: readFileSync(new URL(path, root), 'utf8') }));

test('廃止した入力項目・名称・乱数を画面スクリプトに残さない', () => {
  for (const [word, reason] of [
    ['sluggish', '長打率の項目'],
    ['長打率', '長打率の文言'],
    ['出塁率は', '打率の旧名称'],
    ['buntSuccessRate', 'バント成功率の入力'],
    ['stealSuccessRate', '盗塁成功率の入力'],
    ['bunt_success_rate', 'バント成功率の送信'],
    ['steal_success_rate', '盗塁成功率の送信'],
    ['ブンブン丸', '長距離砲の旧名称'],
    ['標準', '単打マンの旧名称'],
    ['バント重視', 'バント職人の旧名称'],
    ['pitcher', '存在しない投手の設定'],
    ['Math.random', '演出の乱数（同じ結果なら同じ表示にする）'],
  ]) {
    for (const { path, text } of sources) {
      assert.ok(!text.includes(word), `${path}に${reason}（${word}）を残さない`);
    }
  }
});

test('打順入力フォームは lineup/lineup-form.ts にだけ定義し、画面はそれを使う（issue #153）', () => {
  const definitions = sources.filter(({ text }) => /export class LineupForm\b/.test(text)).map(({ path }) => path);
  assert.deepEqual(definitions, ['lineup/lineup-form.ts']);
  for (const page of ['simulation/simulation-page.ts', 'single-game/single-game-page.ts']) {
    assert.match(sources.find(({ path }) => path === page).text, /from '\.\.\/lineup\/lineup-form\.ts'/, `${page}は共通の打順入力フォームを使う`);
  }
});

test('モジュールは相対パスの.tsファイルだけをimportし、画面の入口だけが読み込み時に画面を組み立てる', () => {
  for (const { path, text } of sources) {
    for (const [, specifier] of text.matchAll(/from '([^']+)'/g)) {
      assert.match(specifier, /^\.\.?\/.+\.ts$/, `${path}の${specifier}`);
    }
    const topLevelCalls = text.split('\n').filter(line => /^[a-zA-Z]\w*\(/.test(line));
    if (!path.startsWith('pages/')) {
      assert.deepEqual(topLevelCalls, [], `${path}は読み込んだだけでは画面を操作しない`);
    }
  }
});
