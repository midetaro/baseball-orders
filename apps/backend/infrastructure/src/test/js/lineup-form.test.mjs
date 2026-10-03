import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const lineupFormJs = readFileSync(new URL('../../main/resources/static/js/lineup-form.js', import.meta.url), 'utf8');
const pages = ['simulation', 'single-game'].map(name => ({
  name,
  html: readFileSync(new URL(`../../main/resources/templates/${name}.html`, import.meta.url), 'utf8'),
  js: readFileSync(new URL(`../../main/resources/static/js/${name}.js`, import.meta.url), 'utf8')
}));

// --- 打順入力フォームの共通化（issue #153） ---
const sharedFunctions = ['valid', 'validLineup', 'toggle', 'formatPercentage', 'fieldWrapper', 'personalitySelect', 'render', 'update', 'showView', 'startLineupForm'];
for (const name of sharedFunctions) {
  assert.ok(lineupFormJs.includes(`function ${name}(`), `共通の打順入力フォームに${name}を定義する`);
}
for (const {name, html, js} of pages) {
  assert.ok(html.includes(`<script src="/js/lineup-form.js"></script>\n<script src="/js/${name}.js"></script>`), `${name}は共通の打順入力フォームを画面固有スクリプトより先に読み込む`);
  for (const shared of sharedFunctions) {
    assert.ok(!js.includes(`function ${shared}(`), `${name}.jsに共通関数${shared}を重複定義しない`);
  }
  for (const declaration of ['const lineup = [', 'const ranges =', 'const fields =', 'const personalityLabels =', 'let inFlight', 'let hasResults']) {
    assert.ok(!js.includes(declaration), `${name}.jsに共通の宣言「${declaration}」を重複定義しない`);
  }
  for (const listener of ["tabInput.addEventListener(", "toggleAllBunt.addEventListener(", "toggleAllSteal.addEventListener(", "resetAllPersonalities.addEventListener(", "submit.addEventListener("]) {
    assert.ok(!js.includes(listener), `${name}.jsに共通のイベント登録「${listener}」を重複定義しない`);
  }
  assert.equal((js.match(/startLineupForm\(\{/g) ?? []).length, 1, `${name}.jsは共通の打順入力フォームを1回だけ開始する`);
}

function extractFunction(name) {
  const start = lineupFormJs.indexOf(`function ${name}(`);
  assert.ok(start >= 0, `${name} を定義する`);
  return lineupFormJs.slice(start, lineupFormJs.indexOf('\n', start));
}
const ranges = lineupFormJs.match(/const ranges = \{[^}]*\};/)[0];
const valid = new Function(`${ranges}\n${extractFunction('valid')}\nreturn valid;`)();
const player = (hitAverage, sluggish) => ({hitAverage, sluggish, buntEnabled:true, stealEnabled:false, personality:'DEFAULT'});
assert.equal(valid(player('0.35', '0.40')), true, '範囲内の打率・長打率は有効にする');
assert.equal(valid(player('0.61', '0.40')), false, '打率が上限を超えたら無効にする');
assert.equal(valid(player('0.35', '0.09')), false, '長打率が下限を下回ったら無効にする');
assert.equal(valid(player('', '0.40')), false, '未入力の項目があれば無効にする');
const formatPercentage = new Function(`${extractFunction('formatPercentage')}\nreturn formatPercentage;`)();
assert.equal(formatPercentage('0.3'), '0.30', '入力値を小数第2位に整形する');
assert.equal(formatPercentage(''), '', '未入力はそのまま残す');

assert.ok(lineupFormJs.includes("key:'hitAverage',label:'打率',min:0.01,max:0.6"), '打率のラベルと既存の範囲を使用する');
assert.ok(lineupFormJs.includes('打率は0.01〜0.6、長打率は0.1〜0.6の範囲'), '入力エラーを打率として案内する');
assert.ok(!lineupFormJs.includes("label:'出塁率'"), '入力ラベルに旧名称を残さない');
assert.ok(!lineupFormJs.includes('出塁率は'), '入力エラーに旧名称を残さない');

// --- 性格の選択肢（issue #145） ---
const personalityLabels = new Function(`${lineupFormJs.match(/const personalityLabels = \{[^}]*\};/)[0]}\nreturn personalityLabels;`)();
assert.deepEqual(
  Object.entries(personalityLabels),
  [['DEFAULT', '単打マン'], ['MIDDLE_DISTANCE', '中距離砲'], ['EAGER_SLUGGISH', '長距離砲'], ['HIGH_ON_BASE', '高出塁率'], ['EAGER_STEAL', '盗塁重視'], ['EAGER_BUNT', 'バント職人']],
  '性格は単打マン・中距離砲・長距離砲・高出塁率・盗塁重視・バント職人の順に選択できる'
);
assert.ok(!Object.values(personalityLabels).includes('標準'), '標準の性格を選択肢に残さない');
assert.ok(!Object.values(personalityLabels).includes('バント重視'), 'バント重視の旧ラベルを残さない');

console.log('PASS: 打順入力フォームの共通化');
