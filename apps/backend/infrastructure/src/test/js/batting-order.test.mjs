import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/batting-order.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/batting-order.css', import.meta.url), 'utf8');

// --- 打順組み替え画面（issue #146） ---
assert.ok(!html.includes('<style>'), '打順組み替え画面のCSSをインラインで保持しない');
assert.ok(!html.includes('<script>'), '打順組み替え画面のJSをインラインで保持しない');
assert.ok(
  html.includes('<link rel="stylesheet" href="/css/simulation.css">\n    <link rel="stylesheet" href="/css/batting-order.css">'),
  '共通のシミュレーション画面CSSの後に打順組み替え画面のCSSを読み込む'
);
assert.ok(html.includes('<h2 id="order-heading">打順組み替え</h2>'), '入力欄の見出しを打順組み替えにする');
assert.ok(html.includes('<div data-lineup-mode="reorder" id="order"></div>'), '打順フォームを組み替えモードで表示する');
assert.ok(html.includes('id="score-histogram"'), '大規模実行と同じ試合結果を表示する');
for (const label of ['打順', '打率', '性格', 'バント', '盗塁', 'メモ']) {
  assert.ok(html.includes(`<span>${label}</span>`), `列見出しに${label}を表示する`);
}

assert.ok(!html.includes('長打率'), '打順組み替え画面に長打率を表示しない');
assert.match(css, /grid-template-columns: 38px 28px 60px 118px 96px 96px 140px;/, '打順・ドラッグ・打率・性格・バント・盗塁・メモの7列にする');
assert.ok(html.includes('<span>打順</span><span aria-hidden="true"></span><span>打率</span><span>性格</span><span>バント</span><span>盗塁</span><span>メモ</span>'), '列見出しは打順・ドラッグ・打率・性格・バント・盗塁・メモの順');
assert.ok(!html.includes('reset-all-personalities'), '組み替え画面に性格初期化を置かない');
assert.ok(html.includes('id="team-select"') && html.includes('id="average-hit-average"'), 'チーム選択と平均打率を表示する');
assert.ok(css.includes('touch-action: none'), 'ドラッグハンドルの操作で画面をスクロールさせない');

// 打順の組み替え・平均打率・メモ・固定ラベル・チーム既定値の振る舞いは scripts/lineup/*.test.mjs で検査する。

console.log('PASS: 打順組み替え画面');
