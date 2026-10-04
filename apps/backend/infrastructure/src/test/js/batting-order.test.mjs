import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { readScript, toJs } from './typescript-source.mjs';

const html = readFileSync(new URL('../../main/resources/templates/batting-order.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/batting-order.css', import.meta.url), 'utf8');
const lineupFormJs = readScript('lineup-form');

// --- 打順組み替え画面（issue #146） ---
assert.ok(!html.includes('<style>'), '打順組み替え画面のCSSをインラインで保持しない');
assert.ok(!html.includes('<script>'), '打順組み替え画面のJSをインラインで保持しない');
assert.ok(
  html.includes('<link rel="stylesheet" href="/css/simulation.css">\n    <link rel="stylesheet" href="/css/batting-order.css">'),
  '共通のシミュレーション画面CSSの後に打順組み替え画面のCSSを読み込む'
);
assert.ok(
  html.includes('<script src="/js/lineup-form.js"></script>\n<script src="/js/simulation.js"></script>\n<script src="/js/site-menu.js"></script>'),
  '共通の打順フォーム・大規模実行の結果描画・左メニューの順に読み込む'
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

// --- 打順の組み替え ---
function extractFunction(name) {
  // 型引数を持つ関数（function movePlayer<T>(...)）も対象にする。
  const start = lineupFormJs.search(new RegExp(`function ${name}[(<]`));
  assert.ok(start >= 0, `${name} を定義する`);
  return lineupFormJs.slice(start, lineupFormJs.indexOf('\n', start));
}
const movePlayer = new Function(toJs(`${extractFunction('movePlayer')}\nreturn movePlayer;`))();
const players = () => ['A', 'B', 'C', 'D'];
const down = players();
assert.equal(movePlayer(down, 0, 2), true, '下の打順へ移動できる');
assert.deepEqual(down, ['B', 'C', 'A', 'D'], '移動元の打者を移動先の打順へ差し込み、間の打者を繰り上げる');
const up = players();
assert.equal(movePlayer(up, 3, 1), true, '上の打順へ移動できる');
assert.deepEqual(up, ['A', 'D', 'B', 'C'], '移動元の打者を移動先の打順へ差し込み、間の打者を繰り下げる');
for (const [from, to] of [[1, 1], [0, -1], [3, 4]]) {
  const unchanged = players();
  assert.equal(movePlayer(unchanged, from, to), false, `${from}から${to}へは移動しない`);
  assert.deepEqual(unchanged, players(), '移動しない場合は打順を変えない');
}

assert.ok(lineupFormJs.includes("const reorderMode = order.dataset.lineupMode === 'reorder';"), '打順フォームの組み替えモードを画面のHTMLで選ぶ');
assert.ok(lineupFormJs.includes('if (reorderMode) { row.append(fieldWrapper(field.label, statLabel(player[field.key]))); return; }'), '組み替えモードでは打率を入力させずラベルで表示する');
assert.ok(lineupFormJs.includes("handle.addEventListener('pointerdown'"), 'マウス・タッチ共通のポインター操作でドラッグを開始する');
assert.ok(lineupFormJs.includes("handle.addEventListener('pointerup'"), 'ドロップした打順へ打者を移動する');
assert.ok(lineupFormJs.includes('{ArrowUp:-1,ArrowDown:1}'), 'キーボードの上下キーでも打順を入れ替えられる');

// --- 平均打率・メモ・固定ラベル・チーム既定値 ---
const averageHitAverage = new Function(toJs(`${lineupFormJs.match(/const ranges = \{[^}]*\}[^;]*;/)[0]}\n${extractFunction('valid')}\n${extractFunction('averageHitAverage')}\nreturn averageHitAverage;`))();
const hit = values => values.map(hitAverage => ({hitAverage}));
assert.equal(averageHitAverage(hit(['0.300','0.300','0.300','0.300','0.300','0.300','0.300','0.300','0.255'])), '0.295', '平均打率を小数第3位で表示する');
assert.equal(averageHitAverage(hit(['0.300',''])), '—', '未入力があれば平均打率を表示しない');
assert.equal(averageHitAverage(hit(['0.300','0.9'])), '—', '範囲外があれば平均打率を表示しない');
const lineupRequest = new Function(toJs(`${extractFunction('lineupRequest')}\nreturn lineupRequest;`))();
const body = lineupRequest([{hitAverage:'0.300',buntEnabled:true,stealEnabled:false,personality:'DEFAULT',memo:'秘密',buntForced:true}]);
assert.deepEqual(body, [{hit_average:0.3,bunt_enabled:true,steal_enabled:false,personality:'DEFAULT'}], '送信内容にメモ・固定フラグを含めない');
const parseDefaultBatters = new Function(toJs(`${extractFunction('parseDefaultBatters')}\nreturn parseDefaultBatters;`))();
const parsed = parseDefaultBatters([{dataset:{hitAverage:'0.300',personality:'EAGER_STEAL',stealForced:'true',buntForced:'false'}},{dataset:{hitAverage:'0.250',personality:'DEFAULT',stealForced:'false',buntForced:'false'}}]);
assert.deepEqual(parsed[0], {hitAverage:'0.300',personality:'EAGER_STEAL',stealForced:true,buntForced:false,stealEnabled:true,buntEnabled:false,memo:''}, '盗塁固定の打者は盗塁有効で開始する');
assert.deepEqual([parsed[1].stealEnabled, parsed[1].buntEnabled], [false, false], '固定でない打者は盗塁・バントなしで開始する');
assert.ok(lineupFormJs.includes('forced-label'), '固定の盗塁・バントは切り替え不可のラベルで表示する');
assert.ok(lineupFormJs.includes("statLabel(personalityLabels[player.personality])"), '組み替え画面の性格はラベル表示');
assert.ok(lineupFormJs.includes("maxLength=20"), 'メモの最大長を制限する');

console.log('PASS: 打順組み替え画面');
