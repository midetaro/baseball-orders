import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/single-game.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/single-game.css', import.meta.url), 'utf8');

assert.ok(html.includes('<span>打率</span>'), '1試合画面に打率の列見出しを表示する');
assert.ok(!html.includes('出塁率'), '1試合画面に旧入力名を残さない');
assert.ok(!html.includes('長打率'), '1試合画面に長打率を表示しない');

// --- HTMLとCSSのファイル分離 ---
assert.ok(!html.includes('<style>'), '1試合実行画面のCSSをインラインで保持しない');
assert.ok(html.includes('<link rel="stylesheet" href="/css/single-game.css">'), '1試合実行画面から分離したCSSファイルを読み込む');

// --- HTMLとJSのファイル分離（読み込むスクリプトは scripts/pages.test.mjs で検査する） ---
assert.ok(!html.includes('<script th:inline="none">'), '1試合実行画面のJSをインラインで保持しない');

assert.match(css, /\.out-count\s*\{\s*color:\s*#b8432f;/, 'アウト数をアースカラーの赤（レンガ色）で表示する');
assert.ok(css.includes('color-scheme: light') && css.includes('--moss: #56704a'), '1試合実行画面は / と同じアースカラー配色にする');
assert.ok(!/--(cyan|lime|pink|violet)\b/.test(css), '1試合実行画面にネオン調の配色を残さない');
for (const name of ['single-game', 'simulation', 'simulation-guide']) {
  const template = readFileSync(new URL(`../../main/resources/templates/${name}.html`, import.meta.url), 'utf8');
  assert.ok(!template.includes('ブンブン丸'), `${name}に旧名称を残さない`);
}

// --- 1試合実行結果のアニメーションフレーム描画 ---
assert.ok(!html.includes('id="transitions"'), '推移のプレーンテキスト表示を除去する');
assert.ok(html.includes('id="frame-stage"'), 'アニメーションフレームの表示領域を用意する');
assert.match(
  html,
  /th:attr="data-frame-duration-millis=\$\{frameDurationMillis}[,"]/,
  'サーバー設定のフレーム間隔をデータ属性で渡す'
);




assert.ok(html.includes('id="order-table-scroll"'), '結果表を横スクロール可能な領域に置く');


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
assert.ok(simulationHtml.includes('<nav aria-label="画面切り替え" class="view-tabs" role="tablist">'), '/ も同じタブ構成である');

// --- 打順入力欄の横スクロール解消（数値入力の余白削減とスマホ表示の一行化） ---
assert.match(css, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.ok(css.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.match(css, /\.field-caption\s*\{\s*display:\s*none;\s*\}/, '通常幅では列見出しと入力キャプションを重複表示しない');
const mobileCss = css.slice(css.indexOf('@media (max-width: 760px)'));
assert.ok(!/\.field-caption\s*\{\s*display:\s*block;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく入力キャプションを重複表示しない');
assert.ok(!/\.columns\s*\{\s*display:\s*none;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく列見出し行を表示する');
assert.match(mobileCss, /\.order-scroll\s*\{\s*overflow-x:\s*visible;\s*\}/, 'スマホ幅では打順入力欄の横スクロールを不要にする');
assert.match(mobileCss, /\.columns, \.slot\s*\{\s*grid-template-columns:\s*24px\s+minmax\(0,\s*\.8fr\)\s+minmax\(0,\s*1\.4fr\)\s+repeat\(2,\s*minmax\(0,\s*\.8fr\)\);/, 'スマホ幅でもPC幅と同じく打者ごとに一行で入力欄を横に並べる');
assert.match(css, /\.toggle-label, \.toggle-state\s*\{\s*white-space:\s*nowrap;\s*\}/, 'トグルの項目名と状態はそれぞれ語の途中で折り返さない');
assert.match(css, /\.bunt-toggle\s*\{[^}]*column-gap:\s*\.3em;/, 'トグルの項目名と状態の間に余白を空ける');
assert.match(mobileCss, /\.toggle-label\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では列見出しと重複するトグルの項目名を省略して1行で収める');

// --- バント成功率・盗塁成功率の入力欄を削除する（1試合実行画面） ---
assert.ok(!html.includes('バント成功率'), '1試合実行画面ではバント成功率のラベル・列見出しを表示しない');
assert.ok(!html.includes('盗塁成功率'), '1試合実行画面では盗塁成功率のラベル・列見出しを表示しない');

// --- 本塁打・得点・安打の専用アニメーション（issue #141） ---
// 演出の選び方・プレー直後の状況の求め方は scripts/single-game/*.test.mjs で検査する。

// --- バント成功の演出（issue #156） ---
assert.match(html, /th:attr="[^"]*data-bunt-frame-duration-millis=\$\{buntFrameDurationMillis}/, 'バント成功フレームの表示時間をサーバー設定から渡す');
assert.ok(css.includes('@keyframes bunt-ball'), 'バントの打球が本塁前に転がる');
assert.match(css, /\.effect-bunt \.ball\s*\{[^}]*animation:\s*bunt-ball/, 'バント成功フレームの打球にバント用の動きを適用する');
assert.match(css, /\.effect-bunt \.base\.occupied/, 'バント成功で進塁した走者の塁を点灯させる');
assert.match(css, /\.headline-bunt\s*\{/, 'バント成功の見出しのスタイルを用意する');

assert.match(html, /th:attr="[^"]*data-hit-frame-duration-millis=\$\{hitFrameDurationMillis}/, '安打フレームの表示時間をサーバー設定から渡す');
assert.match(html, /th:attr="[^"]*data-score-frame-duration-millis=\$\{scoreFrameDurationMillis}/, '得点フレームの表示時間をサーバー設定から渡す');
assert.match(html, /th:attr="[^"]*data-home-run-frame-duration-millis=\$\{homeRunFrameDurationMillis}/, '本塁打フレームの表示時間をサーバー設定から渡す');
for (const [name, message] of [
  ['home-run-headline', '本塁打の見出しを大きく表示する'],
  ['home-run-flash', '本塁打で画面を光らせる'],
  ['home-run-ball', '本塁打の打球が場外へ飛ぶ'],
  ['firework-spark', '本塁打で花火を打ち上げる'],
  ['runner-circuit', '本塁打の打者走者がダイヤモンドを一周する'],
  ['score-burst', '得点を「+N点」で弾けるように表示する'],
  ['home-plate-glow', '得点で本塁を光らせる'],
  ['scoreboard-pulse', '得点でスコアボードの得点を強調する'],
  ['hit-headline', '安打の種類を見出しで表示する'],
  ['hit-ball', '安打の打球が外野へ飛ぶ'],
  ['runner-to-first', '単打の打者走者が一塁へ走る'],
  ['runner-to-second', '二塁打の打者走者が二塁へ走る'],
  ['runner-to-third', '三塁打の打者走者が三塁へ走る'],
]) {
  assert.ok(css.includes(`@keyframes ${name}`), message);
}
assert.match(css, /@media \(prefers-reduced-motion: reduce\)\s*\{[\s\S]*animation:\s*none/, '動きを減らす設定では演出アニメーションを止める');

// --- アニメーションの再生速度（issue #156） ---
assert.ok(
  html.includes('<div aria-label="再生速度" class="playback-speed" role="group">'),
  '試合結果に再生速度の切り替えを用意する'
);
for (const [speed, label, pressed] of [['slow', '遅い', 'false'], ['normal', '普通', 'true'], ['fast', '速い', 'false']]) {
  assert.ok(
    html.includes(`<button aria-pressed="${pressed}" class="speed-option" data-speed="${speed}" type="button">${label}</button>`),
    `再生速度「${label}」を選べ、初期値は「普通」にする`
  );
}
assert.ok(
  html.indexOf('class="playback-speed"') < html.indexOf('id="frame-stage"'),
  '再生速度の切り替えはアニメーションの直上に置く'
);
assert.match(css, /\.speed-option\[aria-pressed="true"\]\s*\{/, '選択中の速度ボタンを強調表示する');

// --- 野球のスコアボード（イニング別得点とR・H・E）（issue #149） ---
assert.ok(html.includes('id="line-score"'), 'スコアボードの表示領域を用意する');
assert.ok(
  html.indexOf('id="line-score"') < html.indexOf('id="frame-stage"'),
  'スコアボードはアニメーションフレームより上に表示する'
);
// --- スコアボードは最初から試合結果を表示し、再生中のイニングを色で示す（issue #156） ---
assert.match(css, /\.line-score th\.is-current\s*\{/, '再生中のイニングの見出しを強調表示する');
assert.match(css, /\.line-score\s*\{/, 'スコアボードのスタイルを用意する');
assert.match(css, /#results \.result-head, #results \.line-score-wrap\s*\{\s*grid-column:\s*1 \/ -1;/, 'PC幅ではスコアボードを結果パネルの全幅に表示する');
assert.match(mobileCss, /\.result-head\s*\{\s*align-items:\s*center;\s*flex-wrap:\s*nowrap;/, 'スマホ幅ではスコアボードの分だけ縦幅を空けるため「打順を編集する」を見出しの横に並べる');
assert.match(mobileCss, /--field-size:\s*112px;/, 'スマホ幅ではスコアボードの分だけダイヤモンドを小さくしてページをスクロールさせない');

console.log('PASS: 1試合実行結果のアニメーションフレームと打順成績表の描画');
