import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/batting-order.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/batting-order.css', import.meta.url), 'utf8');
const lineupFormJs = readFileSync(new URL('../../main/resources/static/js/lineup-form.js', import.meta.url), 'utf8');
const siteMenuJs = readFileSync(new URL('../../main/resources/static/js/site-menu.js', import.meta.url), 'utf8');

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
for (const label of ['打順', '打率', '長打率', '性格', 'バント', '盗塁']) {
  assert.ok(html.includes(`<span>${label}</span>`), `列見出しに${label}を表示する`);
}

// --- 左メニュー ---
assert.ok(html.includes('aria-controls="site-menu" aria-expanded="false" class="all-toggle menu-toggle" id="menu-toggle"'), 'メニューは閉じた状態で表示する');
assert.match(html, /<nav aria-label="画面メニュー" class="site-menu" hidden id="site-menu">/, '他の画面への導線は既定で表示しない左メニューに置く');
const menu = html.slice(html.indexOf('id="site-menu"'), html.indexOf('</nav>', html.indexOf('id="site-menu"')));
assert.ok(menu.includes('<a aria-current="page" href="/">打順組み替え</a>'), 'メニューで現在の画面を示す');
for (const [href, label] of [['/large-scale', '大規模実行'], ['/single-game', '1試合実行'], ['/simulation-guide', 'シミュレーションの仕組み']]) {
  assert.ok(menu.includes(`<a href="${href}">${label}</a>`), `メニューから${label}画面を選べる`);
}
const header = html.slice(html.indexOf('<header'), html.indexOf('</header>'));
for (const href of ['/large-scale', '/single-game', '/simulation-guide']) {
  assert.ok(!header.includes(`href="${href}"`), `ヘッダーに${href}への導線を直接表示しない`);
}
assert.ok(css.includes('.site-menu {') && css.includes('left: 0'), 'メニューは画面左に表示する');
assert.ok(css.includes('touch-action: none'), 'ドラッグハンドルの操作で画面をスクロールさせない');

const menuState = {hidden:true, backdropHidden:true, expanded:null, focused:null};
const listeners = {};
// hiddenのアクセサを保つため、スプレッドではなくプロパティ記述子で合成する。
const element = (name, extra = {}) => Object.defineProperties({addEventListener(type, handler) { listeners[`${name}:${type}`] = handler; }, setAttribute(key, value) { if (key === 'aria-expanded') menuState.expanded = value; }, focus() { menuState.focused = name; }}, Object.getOwnPropertyDescriptors(extra));
const siteMenu = element('menu', {get hidden() { return menuState.hidden; }, set hidden(value) { menuState.hidden = value; }, querySelector: () => element('first-link')});
const elements = {'#menu-toggle': element('toggle'), '#site-menu': siteMenu, '#menu-close': element('close'), '#menu-backdrop': element('backdrop', {set hidden(value) { menuState.backdropHidden = value; }})};
new Function('document', siteMenuJs)({querySelector: selector => elements[selector], addEventListener(type, handler) { listeners[`document:${type}`] = handler; }});
listeners['toggle:click']();
assert.deepEqual(menuState, {hidden:false, backdropHidden:false, expanded:'true', focused:'first-link'}, 'メニューボタンで左メニューを開く');
listeners['document:keydown']({key:'Escape'});
assert.deepEqual(menuState, {hidden:true, backdropHidden:true, expanded:'false', focused:'toggle'}, 'Escapeで左メニューを閉じる');
listeners['toggle:click']();
listeners['backdrop:click']();
assert.equal(menuState.hidden, true, 'メニュー外をクリックすると左メニューを閉じる');
listeners['toggle:click']();
listeners['close:click']();
assert.equal(menuState.hidden, true, '閉じるボタンで左メニューを閉じる');

// --- 打順の組み替え ---
function extractFunction(name) {
  const start = lineupFormJs.indexOf(`function ${name}(`);
  assert.ok(start >= 0, `${name} を定義する`);
  return lineupFormJs.slice(start, lineupFormJs.indexOf('\n', start));
}
const movePlayer = new Function(`${extractFunction('movePlayer')}\nreturn movePlayer;`)();
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
assert.ok(lineupFormJs.includes('if (reorderMode) { row.append(fieldWrapper(field.label, statLabel(player[field.key]))); return; }'), '組み替えモードでは打率・長打率を入力させずラベルで表示する');
assert.ok(lineupFormJs.includes("handle.addEventListener('pointerdown'"), 'マウス・タッチ共通のポインター操作でドラッグを開始する');
assert.ok(lineupFormJs.includes("handle.addEventListener('pointerup'"), 'ドロップした打順へ打者を移動する');
assert.ok(lineupFormJs.includes('{ArrowUp:-1,ArrowDown:1}'), 'キーボードの上下キーでも打順を入れ替えられる');

console.log('PASS: 打順組み替え画面');
