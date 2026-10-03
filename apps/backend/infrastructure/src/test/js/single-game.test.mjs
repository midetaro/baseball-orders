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

assert.ok(
  html.includes('<div class="section-actions">'),
  '打順入力の実行ボタンとトグル操作群を分離してPCで折り返せるようにする'
);
assert.match(
  css,
  /@media \(min-width: 761px\)[\s\S]*\.section-actions\s*\{\s*display:\s*flex;\s*flex-basis:\s*100%;/,
  'PC幅では操作トグル群を見出し行の下に折り返して配置する'
);

// --- / と機能的に共通する表示をそろえる（issue #139） ---
const simulationHtml = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const simulationCss = readFileSync(new URL('../../main/resources/static/css/simulation.css', import.meta.url), 'utf8');
function cssRule(source, selector) {
  const start = source.indexOf(`\n        ${selector} {`);
  assert.ok(start >= 0, `${selector} のスタイルが存在する`);
  return source.slice(start, source.indexOf('}', start) + 1);
}
assert.ok(!css.includes('header::before') && !css.includes('BASEBALL ORDER LAB'), 'ヘッダーに / にない装飾ラベルを表示しない');
for (const selector of ['main', 'header', 'h1', 'header .status', '.view-tabs', '.view-tab', '.view-tab-step', '.view-tab[aria-selected="true"]', '.view-tab[aria-selected="true"] .view-tab-step', 'section[hidden]', '.section-head', '.order', '.columns, .slot', 'input, select', '.bunt-toggle', '#feedback', '.result-head .hint', '.result-actions']) {
  assert.equal(cssRule(css, selector), cssRule(simulationCss, selector), `${selector} の表示を / とそろえる`);
}
assert.ok(
  html.includes('<nav aria-label="画面切り替え" class="view-tabs" role="tablist">'),
  '/ と同じく打順入力と試合結果を切り替えるタブを用意する'
);
assert.ok(
  html.includes('<button aria-controls="input-view" aria-selected="true" class="view-tab" id="tab-input" role="tab" type="button"><span class="view-tab-step">1</span>打順入力</button>'),
  '初期表示では打順入力タブを選択する'
);
assert.ok(
  html.includes('<button aria-controls="results" aria-selected="false" class="view-tab" disabled id="tab-results" role="tab" type="button"><span class="view-tab-step">2</span>試合結果</button>'),
  '結果が得られるまで試合結果タブを選択できない'
);
assert.ok(html.includes('<section aria-labelledby="order-heading" id="input-view" role="tabpanel">'), '打順入力をタブパネルにする');
assert.ok(
  html.includes('<section aria-labelledby="results-heading" hidden id="results" role="tabpanel">'),
  '初期表示では試合結果タブパネルを隠す'
);
assert.ok(html.includes('<h2 id="results-heading">試合結果</h2>'), '結果パネルの見出しを / と同じ「試合結果」にする');
assert.ok(html.includes('id="result-feedback" role="status"'), '結果パネルの見出し下に実行結果の状態を表示する');
assert.ok(html.includes('<button class="all-toggle" id="edit-lineup" type="button">打順を編集する</button>'), '結果パネルから打順入力へ戻れる');
assert.ok(!html.includes('id="toggle-lineup"') && !html.includes('入力欄を閉じる'), '「入力欄を閉じる」ボタンを表示しない');
assert.ok(!html.includes('id="lineup-body"'), '打順入力欄を折りたたみ領域にしない');
assert.ok(!js.includes('setLineupCollapsed') && !js.includes('lineupBody'), '打順入力欄の折りたたみ処理を持たない');
assert.ok(js.includes('function showView(resultsVisible)'), '/ と同じくタブの表示を切り替える関数を用意する');
assert.ok(
  js.includes("tabInput.addEventListener('click',()=>showView(false));") &&
    js.includes("tabResults.addEventListener('click',()=>showView(true));") &&
    js.includes("editLineup.addEventListener('click',()=>showView(false));"),
  'タブと「打順を編集する」で表示を切り替える'
);
assert.ok(
  js.includes('renderGame(data.transitions);hasResults=true;showView(true);'),
  '1試合実行が成功したら試合結果タブへ切り替える'
);
assert.ok(
  js.includes('submit.disabled=inFlight || !complete;') &&
    js.includes('tabInput.disabled=inFlight; tabResults.disabled=inFlight || !hasResults; editLineup.disabled=inFlight;'),
  '実行中はタブと「打順を編集する」を無効にし、結果が得られるまで試合結果タブを無効にする'
);
assert.ok(
  js.includes("resultFeedback.className='hint success';resultFeedback.textContent='試合が終了しました。';"),
  '実行成功の状態を試合結果パネルの見出し下に表示する'
);
assert.ok(simulationHtml.includes('<nav aria-label="画面切り替え" class="view-tabs" role="tablist">'), '/ も同じタブ構成である');

// --- 打順入力欄の横スクロール解消（数値入力の余白削減とスマホ表示の一行化） ---
assert.match(css, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.ok(css.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.ok(js.includes('function fieldWrapper('), '各入力欄をキャプション付きのフィールドとして構成する');
assert.match(css, /\.field-caption\s*\{\s*display:\s*none;\s*\}/, '通常幅では列見出しと入力キャプションを重複表示しない');
const mobileCss = css.slice(css.indexOf('@media (max-width: 760px)'));
assert.ok(!/\.field-caption\s*\{\s*display:\s*block;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく入力キャプションを重複表示しない');
assert.ok(!/\.columns\s*\{\s*display:\s*none;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく列見出し行を表示する');
assert.match(mobileCss, /\.order-scroll\s*\{\s*overflow-x:\s*visible;\s*\}/, 'スマホ幅では打順入力欄の横スクロールを不要にする');
assert.match(mobileCss, /\.columns, \.slot\s*\{\s*grid-template-columns:\s*24px\s+repeat\(2,\s*minmax\(0,\s*\.8fr\)\)\s+minmax\(0,\s*1\.4fr\)\s+repeat\(2,\s*minmax\(0,\s*\.8fr\)\);/, 'スマホ幅でもPC幅と同じく打者ごとに一行で入力欄を横に並べる');
assert.ok(js.includes("caption.className='toggle-label'"), 'バント・盗塁トグルの項目名を別要素にして幅に応じて省略できるようにする');
assert.ok(js.includes("button.setAttribute('aria-label',`${label}: ${enabled?'する':'しない'}`)"), '項目名を省略しても読み上げではトグルの項目名と状態を伝える');
assert.ok(js.includes("state.className='toggle-state'"), 'トグルの状態を項目名と別要素にして語の途中で折り返さないようにする');
assert.match(css, /\.toggle-label, \.toggle-state\s*\{\s*white-space:\s*nowrap;\s*\}/, 'トグルの項目名と状態はそれぞれ語の途中で折り返さない');
assert.match(css, /\.bunt-toggle\s*\{[^}]*column-gap:\s*\.3em;/, 'トグルの項目名と状態の間に余白を空ける');
assert.match(mobileCss, /\.toggle-label\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では列見出しと重複するトグルの項目名を省略して1行で収める');

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
