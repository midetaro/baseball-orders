import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/single-game.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/single-game.css', import.meta.url), 'utf8');
const js = readFileSync(new URL('../../main/resources/static/js/single-game.js', import.meta.url), 'utf8');

// --- HTMLとCSSのファイル分離 ---
assert.ok(!html.includes('<style>'), '1試合実行画面のCSSをインラインで保持しない');
assert.ok(html.includes('<link rel="stylesheet" href="/css/single-game.css">'), '1試合実行画面から分離したCSSファイルを読み込む');

// --- HTMLとJSのファイル分離 ---
assert.ok(!html.includes('<script th:inline="none">'), '1試合実行画面のJSをインラインで保持しない');
assert.ok(html.includes('<script src="/js/single-game.js"></script>'), '1試合実行画面から分離したJSファイルを読み込む');

assert.match(css, /\.out-count\s*\{\s*color:\s*#b8432f;/, 'アウト数をアースカラーの赤（レンガ色）で表示する');
assert.ok(css.includes('color-scheme: light') && css.includes('--moss: #56704a'), '1試合実行画面は / と同じアースカラー配色にする');
assert.ok(!/--(cyan|lime|pink|violet)\b/.test(css), '1試合実行画面にネオン調の配色を残さない');
assert.ok(js.includes("span.className='out-count'"), 'アウト表示に専用スタイルを適用する');
for (const name of ['single-game', 'simulation', 'simulation-guide']) {
  const template = readFileSync(new URL(`../../main/resources/templates/${name}.html`, import.meta.url), 'utf8');
  const script = ['single-game', 'simulation'].includes(name)
    ? readFileSync(new URL(`../../main/resources/static/js/${name}.js`, import.meta.url), 'utf8')
    : '';
  assert.ok(template.includes('長距離砲') || script.includes('長距離砲'), `${name}の性格名は長距離砲`);
  assert.ok(!template.includes('ブンブン丸') && !script.includes('ブンブン丸'), `${name}に旧名称を残さない`);
}

// --- 1試合実行結果のアニメーションフレーム描画 ---
assert.ok(!html.includes('id="transitions"'), '推移のプレーンテキスト表示を除去する');
assert.ok(html.includes('id="frame-stage"'), 'アニメーションフレームの表示領域を用意する');
assert.match(
  html,
  /th:attr="data-frame-duration-millis=\$\{frameDurationMillis}"/,
  'サーバー設定のフレーム間隔をデータ属性で渡す'
);
assert.ok(
  js.includes("frameStage.dataset.frameDurationMillis"),
  'データ属性からフレーム間隔を読み取る'
);

assert.ok(js.includes('function annotateBattingOrder('), '状況推移から打順を推測する関数を用意する');
assert.ok(
  js.includes("actionResult.startsWith('盗塁')"),
  '盗塁のみの推移は打者の打席結果として扱わない'
);
assert.ok(
  js.includes('battingOrder === 9 ? 1 : battingOrder + 1'),
  '打順は1から9を継続して循環させる'
);

assert.ok(js.includes('function buildFrame('), '1推移から1フレームを生成する関数を用意する');
assert.ok(js.includes("'走者なし':{first:false,second:false,third:false}"), '走者なしの塁配置を定義する');
assert.ok(js.includes("'満塁':{first:true,second:true,third:true}"), '満塁の塁配置を定義する');
assert.ok(js.includes("'一・二塁':{first:true,second:true,third:false}"), '一・二塁の塁配置を定義する');
assert.ok(js.includes("class='diamond'") || js.includes('diamond.className'), 'ダイヤモンドを描画する');
assert.ok(js.includes("'●'.repeat"), 'アウトカウントを記号で表現する');

assert.ok(js.includes('function playFrames('), 'フレームを一定間隔で再生する関数を用意する');
assert.ok(js.includes('setInterval('), 'フレームをコマ送りで自動再生する');
assert.ok(js.includes('function stopFramePlayback('), '再実行時に前回の再生を止める');

assert.ok(js.includes('function buildOrderTable('), 'イニング×打順の結果表を生成する関数を用意する');
assert.ok(
  js.includes('battingOrder <= 9'),
  '打順表の行は固定の9行にする'
);
assert.ok(html.includes('id="order-table-scroll"'), '結果表を横スクロール可能な領域に置く');

assert.ok(js.includes('function renderGame('), '推移全体からフレーム再生と結果表を組み立てる関数を用意する');
assert.ok(js.includes('renderGame(data.transitions)'), '実行結果の描画をrenderGameへ切り替える');
assert.ok(!js.includes('renderTransitions'), '旧いテキスト描画関数を除去する');

// --- 打順入力と結果表示の縦並び配置 ---
assert.match(
  css,
  /\.simulation-workspace\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/,
  'スマホ幅では打順入力と結果を縦に並べる'
);

// --- PC入力欄の画面デザイン変更（issue #124: Figmaデザインに合わせたPC専用の2カラム配置） ---
assert.match(
  css,
  /@media \(min-width: 761px\)\s*\{\s*\.simulation-workspace\s*\{\s*grid-template-columns:\s*minmax\(0,\s*1fr\)\s*minmax\(0,\s*1fr\);/,
  'PC幅（761px以上）では打順入力と結果を2カラムで横並びにする'
);
assert.ok(
  html.includes('<div class="section-actions">'),
  '打順入力の実行ボタンとトグル操作群を分離してPCで折り返せるようにする'
);
assert.match(
  css,
  /@media \(min-width: 761px\)[\s\S]*\.section-actions\s*\{\s*display:\s*flex;\s*flex-basis:\s*100%;/,
  'PC幅では操作トグル群を見出し行の下に折り返して配置する'
);

// --- 実行後に打順入力欄を自動で折りたたみ、結果を見やすくする ---
assert.ok(
  html.includes('id="lineup-body"'),
  '打順入力欄を折りたたみ可能な領域にする'
);
assert.ok(
  html.includes('id="toggle-lineup"'),
  '打順入力欄の開閉トグルボタンを用意する'
);
assert.ok(
  js.includes('function setLineupCollapsed('),
  '折りたたみ状態を切り替える関数を用意する'
);
assert.ok(
  js.includes("toggleLineup.addEventListener('click',()=>setLineupCollapsed(!lineupBody.hidden))"),
  'トグルボタンで開閉を手動切り替えできる'
);
assert.ok(
  js.includes('renderGame(data.transitions);setLineupCollapsed(true);'),
  '1試合実行が成功したら打順入力欄を自動で折りたたむ'
);
assert.ok(
  js.includes('submit.disabled=inFlight || !complete || lineupBody.hidden;'),
  '打順入力欄が閉じている間は1試合実行ボタンを押せなくする'
);

// --- 打順入力欄の横スクロール解消（数値入力の余白削減とスマホ表示のカード化） ---
assert.match(css, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.match(css, /grid-template-columns:\s*38px\s+repeat\(2,\s*60px\)\s+118px\s+78px\s+78px/, '数値入力列の余白を詰めて幅を最小限にする');
assert.ok(css.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.ok(js.includes('function fieldWrapper('), '各入力欄をキャプション付きのフィールドとして構成する');
assert.match(css, /\.field-caption\s*\{\s*display:\s*none;\s*\}/, '通常幅では列見出しと入力キャプションを重複表示しない');
assert.match(css, /@media \(max-width: 760px\)[\s\S]*\.field-caption\s*\{\s*display:\s*block;\s*\}/, 'スマホ幅では列見出しの代わりに入力キャプションを表示する');
assert.match(css, /@media \(max-width: 760px\)[\s\S]*\.columns\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では横スクロールが必要な列見出し行を表示しない');
assert.match(css, /@media \(max-width: 760px\)[\s\S]*\.order-scroll\s*\{\s*overflow-x:\s*visible;\s*\}/, 'スマホ幅では打順入力欄の横スクロールを不要にする');
assert.match(css, /@media \(max-width: 760px\)[\s\S]*\.slot\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/, 'スマホ幅では打順入力行を打者ごとに縦一列のカードへ切り替える');

// --- バント成功率・盗塁成功率の入力欄を削除する（1試合実行画面） ---
assert.ok(!js.includes("key:'buntSuccessRate'"), '1試合実行画面ではバント成功率の入力欄を表示しない');
assert.ok(!js.includes("key:'stealSuccessRate'"), '1試合実行画面では盗塁成功率の入力欄を表示しない');
assert.ok(!html.includes('バント成功率'), '1試合実行画面ではバント成功率のラベル・列見出しを表示しない');
assert.ok(!html.includes('盗塁成功率'), '1試合実行画面では盗塁成功率のラベル・列見出しを表示しない');
assert.ok(!js.includes('buntSuccessRate'), 'バント成功率の値をどこにも保持しない');
assert.ok(!js.includes('stealSuccessRate'), '盗塁成功率の値をどこにも保持しない');
assert.ok(!js.includes('bunt_success_rate'), 'バント成功率をAPIへ送信しない');
assert.ok(!js.includes('steal_success_rate'), '盗塁成功率をAPIへ送信しない');
assert.ok(js.includes("toggle('バント',player.buntEnabled"), 'バントを使うかどうかの切り替えボタンは残す');
assert.ok(js.includes("toggle('盗塁',player.stealEnabled"), '盗塁を使うかどうかの切り替えボタンは残す');

console.log('PASS: 1試合実行結果のアニメーションフレームと打順成績表の描画');
