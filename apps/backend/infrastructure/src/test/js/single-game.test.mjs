import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/single-game.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/single-game.css', import.meta.url), 'utf8');
const pageJs = readFileSync(new URL('../../main/resources/static/js/single-game.js', import.meta.url), 'utf8');
const lineupFormJs = readFileSync(new URL('../../main/resources/static/js/lineup-form.js', import.meta.url), 'utf8');
// 画面で実行されるのは共通の打順入力フォームと画面固有スクリプトを合わせたコードである。
const js = `${lineupFormJs}\n${pageJs}`;

assert.ok(html.includes('<span>打率</span>'), '1試合画面に打率の列見出しを表示する');
assert.ok(!html.includes('出塁率'), '1試合画面に旧入力名を残さない');
assert.ok(!html.includes('長打率'), '1試合画面に長打率を表示しない');

// --- HTMLとCSSのファイル分離 ---
assert.ok(!html.includes('<style>'), '1試合実行画面のCSSをインラインで保持しない');
assert.ok(html.includes('<link rel="stylesheet" href="/css/single-game.css">'), '1試合実行画面から分離したCSSファイルを読み込む');

// --- HTMLとJSのファイル分離 ---
assert.ok(!html.includes('<script th:inline="none">'), '1試合実行画面のJSをインラインで保持しない');
assert.ok(html.includes('<script src="/js/single-game.js"></script>'), '1試合実行画面から分離したJSファイルを読み込む');
assert.ok(
  html.includes('<script src="/js/lineup-form.js"></script>\n<script src="/js/single-game.js"></script>'),
  '共通の打順入力フォームを画面固有スクリプトより先に読み込む'
);
assert.ok(pageJs.includes("startLineupForm({readyMessage:'準備完了。1試合を実行できます。',runningMessage:'試合を実行中…',endpoint:'/simulations/single-game',"), '1試合実行画面の文言と送信先で共通の打順入力フォームを開始する');

assert.match(css, /\.out-count\s*\{\s*color:\s*#b8432f;/, 'アウト数をアースカラーの赤（レンガ色）で表示する');
assert.ok(css.includes('color-scheme: light') && css.includes('--moss: #56704a'), '1試合実行画面は / と同じアースカラー配色にする');
assert.ok(!/--(cyan|lime|pink|violet)\b/.test(css), '1試合実行画面にネオン調の配色を残さない');
assert.ok(js.includes("span.className='out-count'"), 'アウト表示に専用スタイルを適用する');
for (const name of ['single-game', 'simulation', 'simulation-guide']) {
  const template = readFileSync(new URL(`../../main/resources/templates/${name}.html`, import.meta.url), 'utf8');
  const script = ['single-game', 'simulation'].includes(name)
    ? `${lineupFormJs}\n${readFileSync(new URL(`../../main/resources/static/js/${name}.js`, import.meta.url), 'utf8')}`
    : '';
  assert.ok(template.includes('長距離砲') || script.includes('長距離砲'), `${name}の性格名は長距離砲`);
  assert.ok(!template.includes('ブンブン丸') && !script.includes('ブンブン丸'), `${name}に旧名称を残さない`);
}

// --- 1試合実行結果のアニメーションフレーム描画 ---
assert.ok(!html.includes('id="transitions"'), '推移のプレーンテキスト表示を除去する');
assert.ok(html.includes('id="frame-stage"'), 'アニメーションフレームの表示領域を用意する');
assert.match(
  html,
  /th:attr="data-frame-duration-millis=\$\{frameDurationMillis}[,"]/,
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
assert.ok(js.includes('frameTimer=setTimeout('), 'フレームごとの表示時間でコマ送りに自動再生する');
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
  pageJs.includes('renderGame(data.transitions);}});') && lineupFormJs.includes('lineupFormConfig.onSuccess(data);hasResults=true;showView(true);'),
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
assert.match(mobileCss, /\.columns, \.slot\s*\{\s*grid-template-columns:\s*24px\s+minmax\(0,\s*\.8fr\)\s+minmax\(0,\s*1\.4fr\)\s+repeat\(2,\s*minmax\(0,\s*\.8fr\)\);/, 'スマホ幅でもPC幅と同じく打者ごとに一行で入力欄を横に並べる');
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

// --- 本塁打・得点・安打の専用アニメーション（issue #141） ---
function extractFunction(name) {
  const start = js.indexOf(`function ${name}(`);
  assert.ok(start >= 0, `${name} を定義する`);
  return js.slice(start, js.indexOf('\n', start));
}
const classifyEffect = new Function(`${js.match(/const HIT_BASES = \{[^}]*\};/)[0]}\n${extractFunction('classifyEffect')}\nreturn classifyEffect;`)();
const transition = (actionResult, cumulativeScore) => ({actionResult, cumulativeScore});
assert.deepEqual(classifyEffect(transition('本塁打', 1), 0), {kind:'home-run', runs:1, bases:4}, 'ソロ本塁打は本塁打演出にする');
assert.deepEqual(classifyEffect(transition('本塁打', 6), 2), {kind:'home-run', runs:4, bases:4}, '満塁本塁打は4点の本塁打演出にする');
assert.deepEqual(classifyEffect(transition('二塁打', 3), 1), {kind:'score', runs:2, bases:2}, '得点が入った安打は得点演出にし、打者走者の進塁数を保つ');
assert.deepEqual(classifyEffect(transition('スクイズ成功', 1), 0), {kind:'score', runs:1, bases:0}, '安打以外でも得点が入れば得点演出にする');
assert.deepEqual(classifyEffect(transition('四球', 2), 1), {kind:'score', runs:1, bases:0}, '押し出しも得点演出にする');
assert.deepEqual(classifyEffect(transition('単打', 0), 0), {kind:'hit', runs:0, bases:1}, '得点のない単打は安打演出にする');
assert.deepEqual(classifyEffect(transition('二塁打', 0), 0), {kind:'hit', runs:0, bases:2}, '得点のない二塁打は安打演出にする');
assert.deepEqual(classifyEffect(transition('三塁打', 2), 2), {kind:'hit', runs:0, bases:3}, '得点のない三塁打は安打演出にする');
const resolvePlayOutcomes = new Function(`${extractFunction('resolvePlayOutcomes')}\nreturn resolvePlayOutcomes;`)();
const recorded = [
  {inning:1, actionResult:'三塁打', outCount:1, cumulativeScore:0, runnerState:'一・二塁'},
  {inning:1, actionResult:'本塁打', outCount:1, cumulativeScore:2, runnerState:'三塁'},
  {inning:1, actionResult:'凡打', outCount:1, cumulativeScore:4, runnerState:'走者なし'},
  {inning:1, actionResult:'三振', outCount:2, cumulativeScore:4, runnerState:'走者なし'},
  {inning:2, actionResult:'単打', outCount:0, cumulativeScore:4, runnerState:'走者なし'},
];
assert.deepEqual(resolvePlayOutcomes(recorded), [
  {inning:1, actionResult:'三塁打', outCount:1, cumulativeScore:2, runnerState:'三塁', scoreBefore:0},
  {inning:1, actionResult:'本塁打', outCount:1, cumulativeScore:4, runnerState:'走者なし', scoreBefore:2},
  {inning:1, actionResult:'凡打', outCount:2, cumulativeScore:4, runnerState:'走者なし', scoreBefore:4},
  {inning:1, actionResult:'三振', outCount:3, cumulativeScore:4, runnerState:'走者なし', scoreBefore:4},
  {inning:2, actionResult:'単打', outCount:3, cumulativeScore:4, runnerState:'走者なし', scoreBefore:4},
], '記録はプレー直前の状況なので、次の推移からプレー直後のアウト・走者・得点を求め、イニングや試合の最後のプレーは3アウトにする');
assert.ok(js.includes('annotateEffects(annotateBattingOrder(resolvePlayOutcomes(gameTransitions)))'), 'プレー直後の状況で演出と表示を組み立てる');
assert.ok(js.includes('classifyEffect(transition,transition.scoreBefore)'), 'プレー直前と直後の得点差で得点数を求める');
for (const result of ['凡打', '三振', '四球', 'バント失敗', 'スクイズ失敗', '盗塁成功(二塁)', '盗塁失敗(三塁)']) {
  assert.deepEqual(classifyEffect(transition(result, 0), 0), {kind:'none', runs:0, bases:0}, `得点のない${result}は通常表示にする`);
}

// --- バント成功の演出（issue #156） ---
assert.deepEqual(classifyEffect(transition('バント成功', 0), 0), {kind:'bunt', runs:0, bases:0}, '得点のない進塁バント成功はバント演出にする');
assert.deepEqual(classifyEffect(transition('スクイズ成功', 1), 0), {kind:'score', runs:1, bases:0}, '得点の入ったスクイズ成功は得点演出を優先する');
assert.ok(js.includes("bunt:()=>'バント成功!'"), 'バント成功の見出しを表示する');
assert.ok(js.includes("if (effect.kind === 'bunt') diamond.append(element('span','ball ball-bunt'));"), 'バント成功では本塁前に転がる打球を表示する');
assert.match(html, /th:attr="[^"]*data-bunt-frame-duration-millis=\$\{buntFrameDurationMillis}/, 'バント成功フレームの表示時間をサーバー設定から渡す');
assert.ok(js.includes('bunt:Number(frameStage.dataset.buntFrameDurationMillis) || frameDurationMillis'), 'バント成功フレームの表示時間をデータ属性から読み取る');
assert.ok(css.includes('@keyframes bunt-ball'), 'バントの打球が本塁前に転がる');
assert.match(css, /\.effect-bunt \.ball\s*\{[^}]*animation:\s*bunt-ball/, 'バント成功フレームの打球にバント用の動きを適用する');
assert.match(css, /\.effect-bunt \.base\.occupied/, 'バント成功で進塁した走者の塁を点灯させる');
assert.match(css, /\.headline-bunt\s*\{/, 'バント成功の見出しのスタイルを用意する');

assert.match(html, /th:attr="[^"]*data-hit-frame-duration-millis=\$\{hitFrameDurationMillis}/, '安打フレームの表示時間をサーバー設定から渡す');
assert.match(html, /th:attr="[^"]*data-score-frame-duration-millis=\$\{scoreFrameDurationMillis}/, '得点フレームの表示時間をサーバー設定から渡す');
assert.match(html, /th:attr="[^"]*data-home-run-frame-duration-millis=\$\{homeRunFrameDurationMillis}/, '本塁打フレームの表示時間をサーバー設定から渡す');
assert.ok(js.includes('frameStage.dataset.hitFrameDurationMillis') && js.includes('frameStage.dataset.scoreFrameDurationMillis') && js.includes('frameStage.dataset.homeRunFrameDurationMillis'), '演出ごとのフレーム表示時間をデータ属性から読み取る');
assert.ok(js.includes('frameDurations[effect.kind]'), '演出の種類に応じて次のフレームまでの時間を変える');
assert.ok(js.includes("frame.className=`frame effect-${effect.kind}`"), 'フレームに演出の種類を示すクラスを付ける');
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
assert.ok(js.includes("'GRAND SLAM!'") && js.includes("'HOME RUN!'"), '満塁本塁打とそれ以外の本塁打で見出しを変える');
assert.ok(js.includes("{1:'ヒット!',2:'ツーベース!',3:'スリーベース!'}"), '安打の種類ごとに見出しを変える');
assert.ok(js.includes('`+${effect.runs}点`'), '得点数を「+N点」で表示する');
assert.ok(!js.includes('Math.random'), '演出は乱数を使わず同じ結果なら同じ表示にする');

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
const speedMultipliers = new Function(`${js.match(/const PLAYBACK_SPEED_MULTIPLIERS = \{[^}]*\};/)[0]}\nreturn PLAYBACK_SPEED_MULTIPLIERS;`)();
assert.deepEqual(Object.keys(speedMultipliers), ['slow', 'normal', 'fast'], '再生速度は遅い・普通・速いの3択にする');
assert.equal(speedMultipliers.normal, 1, '「普通」はサーバー設定どおりの表示時間で再生する');
assert.equal(speedMultipliers.slow / speedMultipliers.normal, 2, '「遅い」は「普通」の2倍の時間をかけて再生する');
assert.equal(speedMultipliers.normal / speedMultipliers.fast, 2, '「速い」は「普通」の半分の時間で再生する');
assert.ok(js.includes('frameDurations[effect.kind] * PLAYBACK_SPEED_MULTIPLIERS[playbackSpeed]'), '選んだ速度に応じて次のフレームまでの時間を変える');
assert.ok(js.includes('animation.playbackRate=1 / PLAYBACK_SPEED_MULTIPLIERS[playbackSpeed];'), 'フレーム内の演出アニメーションも選んだ速度で再生する');
assert.ok(js.includes("option.setAttribute('aria-pressed',String(option.dataset.speed === playbackSpeed))"), '選択中の速度を押下状態で示す');
assert.match(css, /\.speed-option\[aria-pressed="true"\]\s*\{/, '選択中の速度ボタンを強調表示する');

// --- 野球のスコアボード（イニング別得点とR・H・E）（issue #149） ---
assert.ok(html.includes('id="line-score"'), 'スコアボードの表示領域を用意する');
assert.ok(
  html.indexOf('id="line-score"') < html.indexOf('id="frame-stage"'),
  'スコアボードはアニメーションフレームより上に表示する'
);
const summarizeLineScore = new Function(`${extractFunction('summarizeLineScore')}\nreturn summarizeLineScore;`)();
const played = (inning, runs, bases) => ({inning, effect:{runs, bases}});
assert.deepEqual(
  summarizeLineScore([
    played(1, 0, 1),
    played(1, 2, 4),
    played(1, 0, 0),
    played(2, 1, 0),
    played(2, 0, 2),
    played(3, 0, 0),
  ]),
  {innings:new Map([[1, 2], [2, 1], [3, 0]]), runs:3, hits:3, errors:0},
  'プレー済みの推移からイニング別得点・合計得点(R)・安打数(H)・失策数(E)を集計する'
);
assert.deepEqual(
  summarizeLineScore([]),
  {innings:new Map(), runs:0, hits:0, errors:0},
  'プレー前はすべて0のスコアボードにする'
);
assert.ok(js.includes('const REGULATION_INNINGS = 9;'), 'スコアボードは最低9回まで列を用意する');
assert.ok(js.includes('function buildLineScore('), 'スコアボードの表を生成する関数を用意する');
assert.ok(js.includes("['R','H','E']"), 'スコアボードにR・H・Eの列を表示する');
assert.ok(!js.includes('renderLineScore(annotated, index + 1)'), 'スコアボードの得点を再生済みのプレーだけに絞らない');
// --- スコアボードは最初から試合結果を表示し、再生中のイニングを色で示す（issue #156） ---
const lineScoreCalls = [];
const renderLineScore = new Function(
  'lineScore', 'buildLineScore', 'summarizeLineScore', 'REGULATION_INNINGS',
  `${extractFunction('renderLineScore')}\nreturn renderLineScore;`
)(
  {replaceChildren:table=>lineScoreCalls.push(table)},
  (summary, inningCount, currentInning)=>({summary, inningCount, currentInning}),
  summarizeLineScore,
  9
);
const fullGame = [played(1, 2, 4), played(1, 0, 1), played(5, 1, 0), played(10, 0, 2)];
renderLineScore(fullGame, 1);
assert.deepEqual(
  lineScoreCalls.at(-1),
  {summary:{innings:new Map([[1, 2], [5, 1], [10, 0]]), runs:3, hits:3, errors:0}, inningCount:10, currentInning:1},
  '1回の再生中でも試合全体のイニング別得点とR・H・Eを表示し、再生中のイニングを渡す'
);
renderLineScore(fullGame, null);
assert.equal(lineScoreCalls.at(-1).currentInning, null, '再生が終わったらどのイニングも強調しない');
assert.ok(js.includes('renderLineScore(annotated, transition.inning);'), 'フレームの再生に合わせて強調するイニングを進める');
assert.ok(js.includes('renderLineScore(annotated, null);'), '最後のフレームの表示時間が過ぎたらイニングの強調を外す');
assert.ok(
  js.includes("element('th',inning === currentInning ? 'is-current' : '',String(inning))"),
  '再生中のイニングは見出しの回数も色で示す'
);
assert.match(css, /\.line-score th\.is-current\s*\{/, '再生中のイニングの見出しを強調表示する');
assert.match(css, /\.line-score\s*\{/, 'スコアボードのスタイルを用意する');
assert.match(css, /#results \.result-head, #results \.line-score-wrap\s*\{\s*grid-column:\s*1 \/ -1;/, 'PC幅ではスコアボードを結果パネルの全幅に表示する');
assert.match(mobileCss, /\.result-head\s*\{\s*align-items:\s*center;\s*flex-wrap:\s*nowrap;/, 'スマホ幅ではスコアボードの分だけ縦幅を空けるため「打順を編集する」を見出しの横に並べる');
assert.match(mobileCss, /--field-size:\s*112px;/, 'スマホ幅ではスコアボードの分だけダイヤモンドを小さくしてページをスクロールさせない');

console.log('PASS: 1試合実行結果のアニメーションフレームと打順成績表の描画');
