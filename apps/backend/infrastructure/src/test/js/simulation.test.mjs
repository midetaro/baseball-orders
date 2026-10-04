import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { readScript, toJs } from './typescript-source.mjs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const guideHtml = readFileSync(new URL('../../main/resources/templates/simulation-guide.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/simulation.css', import.meta.url), 'utf8');
const pageJs = readScript('simulation');
const lineupFormJs = readScript('lineup-form');
// 画面で実行されるのは共通の打順入力フォームと画面固有スクリプトを合わせたコードである。
const js = `${lineupFormJs}\n${pageJs}`;

// --- HTMLとCSSのファイル分離 ---
assert.ok(!html.includes('<style>'), 'シミュレーション画面のCSSをインラインで保持しない');
assert.ok(html.includes('<link rel="stylesheet" href="/css/simulation.css">'), 'シミュレーション画面から分離したCSSファイルを読み込む');

// --- HTMLとJSのファイル分離 ---
assert.ok(!html.includes('<script>'), 'シミュレーション画面のJSをインラインで保持しない');
assert.ok(html.includes('<script src="/js/simulation.js"></script>'), 'シミュレーション画面から分離したJSファイルを読み込む');
assert.ok(
  html.includes('<script src="/js/lineup-form.js"></script>\n<script src="/js/simulation.js"></script>'),
  '共通の打順入力フォームを画面固有スクリプトより先に読み込む'
);
assert.ok(pageJs.includes("startLineupForm({readyMessage:'準備完了。シミュレーションを実行できます。',runningMessage:'シミュレーション中…',endpoint:'/simulations',"), 'シミュレーション画面の文言と送信先で共通の打順入力フォームを開始する');

const singleGameHtml = readFileSync(new URL('../../main/resources/templates/single-game.html', import.meta.url), 'utf8');

for (const [page, template] of [
  ['シミュレーション画面', html],
  ['1試合実行画面', singleGameHtml],
  ['シミュレーションガイド画面', guideHtml]
]) {
  assert.ok(!template.includes('ログイン'), `${page}にログイン状態・ログイン導線を表示しない`);
  assert.ok(!template.includes('href="/login"'), `${page}にログイン画面への導線を用意しない`);
  assert.ok(!template.includes('#authorization'), `${page}で認証状態を参照しない`);
  assert.ok(!template.includes('#authentication'), `${page}でログインユーザーを参照しない`);
  assert.ok(!template.includes('action="/logout"'), `${page}にログアウトを用意しない`);
  assert.ok(!template.includes('_csrf'), `${page}でCSRFトークンを参照しない`);
}

assert.ok(js.includes('const lineup: LineupPlayer[] = ['), '9人分の固定打順を作成する');
assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(!html.includes('一番〜九番として送信します。'), '不要な固定打順の説明を表示しない');
assert.ok(js.includes("const lineup: LineupPlayer[] = ["), '打順ごとの役割に応じた初期値を用意する');
assert.ok(js.includes('position.textContent=`${index+1}番`'), '打順は「{数字}番」の固定表示にする');
assert.ok(!js.includes('番打者'), '打順表示に「打者」を付けない');
assert.ok(js.includes("hitAverage:'0.35',buntEnabled"), '一〜五番に制約内の打率を設定する');
assert.ok(js.includes("hitAverage:'0.28',buntEnabled"), '六〜九番に低打率を設定する');
assert.ok(html.includes('得点サマリー'), '得点統計を独立したグループとして表示する');
assert.ok(html.includes('<span>打率</span>'), '打率を入力項目として表示する');
assert.ok(!html.includes('出塁率'), '旧入力名を表示しない');
assert.ok(js.includes("label:'打率'"), '入力項目に打率を表示する');
assert.ok(!js.includes('sluggish'), 'シミュレーション画面のスクリプトに長打率の項目を残さない');
assert.ok(!html.includes('長打率'), 'シミュレーション画面に長打率を表示しない');
assert.ok(!js.includes('buntSuccessRate'), 'バント成功率の入力欄・初期値を保持しない');
assert.ok(!js.includes('stealSuccessRate'), '盗塁成功率の入力欄・初期値を保持しない');
assert.ok(!html.includes('バント成功率'), 'バント成功率のラベル・列見出しを表示しない');
assert.ok(!html.includes('盗塁成功率'), '盗塁成功率のラベル・列見出しを表示しない');
assert.ok(js.includes("function validLineup()"), '打順全体の入力制約を検証する');
assert.ok(!js.includes("lineup.length<=0.35"), '打率の平均に上限を設けない');
assert.ok(js.includes("input.step='0.01'"), '数値入力は小数第2位刻みにする');
assert.ok(js.includes("key:'hitAverage',label:'打率',min:0.01,max:0.6"), '打率の上限を60%にする');
assert.ok(js.includes('function formatPercentage(value: string)'), '入力値を小数第2位に整形する');
assert.ok(js.includes('Number(value).toFixed(2)'), '小数第2位のゼロを常に表示する');
assert.ok(js.includes("input.addEventListener('change'"), '入力の確定時に小数第2位へ整形する');
assert.ok(!js.includes("hitAverage:'.32'"), '小数点前のゼロを省略しない');
assert.ok(js.includes("input.value.startsWith('.') ? `0${input.value}` : input.value"), '入力時も小数点前のゼロを表示する');
assert.ok(!js.includes('hasAtMostTwoDecimalPlaces'), '小数第3位以降の入力を制限しない');
assert.ok(html.includes('本塁打の内訳'), '本塁打統計を構造化して表示する');
assert.ok(html.includes('id="hit-count"'), '総安打数を表示する');
assert.ok(html.indexOf('id="hit-count"') < html.indexOf('id="home-run-count"'), '総安打数を本塁打の上に表示する');
assert.ok(js.includes("'hitCount'"), 'APIの総安打数を結果へ描画する');
assert.ok(html.includes('安打の内訳'), '安打統計を構造化して表示する');
assert.ok(html.includes('id="hit-breakdown"'), '安打内訳のグラフを表示する');
assert.ok(html.includes('id="hit-legend"'), '安打内訳の凡例を表示する');
assert.ok(js.includes("['一塁打',statistics.singleHitCount,'single-hit']"), '一塁打数を内訳へ表示する');
assert.ok(js.includes("['二塁打',statistics.doubleHitCount,'double-hit']"), '二塁打数を内訳へ表示する');
assert.ok(js.includes("['三塁打',statistics.tripleHitCount,'triple-hit']"), '三塁打数を内訳へ表示する');
assert.ok(js.includes("['本塁打',statistics.homeRunCount,'home-run']"), '本塁打数を安打内訳へ表示する');
assert.ok(js.includes('`${label} ${count} (${rate.toFixed(1)}%)`'), '安打内訳の凡例で件数と割合を表示する');
assert.ok(!html.includes('戦術の成否'), '重複する戦術統計を表示しない');
assert.ok(!js.includes("['成功バント',statistics.buntCount]"), '戦術統計の成功バントを表示しない');
assert.ok(!js.includes("['失敗バント',statistics.buntFailureCount]"), '戦術統計の失敗バントを表示しない');
assert.ok(!js.includes("['成功盗塁',statistics.stealCount]"), '戦術統計の成功盗塁を表示しない');
assert.ok(!js.includes("['失敗盗塁',statistics.stealFailureCount]"), '戦術統計の失敗盗塁を表示しない');
assert.ok(html.includes('バントの内訳'), 'バント統計を色分けした内訳として表示する');
assert.ok(html.includes('盗塁の内訳'), '盗塁統計を色分けした内訳として表示する');
assert.ok(js.includes("['進塁成功',statistics.advancingBuntCount,'advancing-bunt']"), '進塁バント成功数を表示する');
assert.ok(js.includes("['スクイズ成功',statistics.squeezeBuntCount,'squeeze-bunt']"), 'スクイズ成功数を表示する');
assert.ok(js.includes("['進塁失敗',statistics.advancingBuntFailureCount,'advancing-bunt-failure']"), '進塁バント失敗数を表示する');
assert.ok(js.includes("['スクイズ失敗',statistics.squeezeBuntFailureCount,'squeeze-bunt-failure']"), 'スクイズ失敗数を表示する');
assert.ok(js.includes("['二盗成功',statistics.stealToSecondCount,'steal-second']"), '二盗成功数を表示する');
assert.ok(js.includes("['三盗成功',statistics.stealToThirdCount,'steal-third']"), '三盗成功数を表示する');
assert.ok(js.includes("['失敗',statistics.stealFailureCount,'steal-failure']"), '盗塁失敗数を内訳へ表示する');
assert.ok(js.includes('const detailTotal=details.reduce'), '内訳項目の合計を割合の分母にする');
assert.ok(js.includes('Number(count)/detailTotal*100'), '集計総数と内訳合計が異なっても内訳比率を誤表示しない');
assert.ok(html.includes('id="bunt-count"'), '既存の成功バント総数を表示する');
assert.ok(html.includes('id="bunt-failure-count"'), '既存の失敗バント総数を表示する');
assert.ok(html.includes('id="steal-count"'), '既存の成功盗塁総数を表示する');
assert.ok(html.includes('id="steal-failure-count"'), '既存の失敗盗塁総数を表示する');
for (const [kind, color] of [
  ['single-hit', 'var(--sage)'],
  ['double-hit', 'var(--sky)'],
  ['triple-hit', 'var(--ochre)'],
  ['home-run', 'var(--clay)'],
  ['solo', 'var(--sage)'],
  ['two-run', 'var(--sky)'],
  ['three-run', 'var(--ochre)'],
  ['grand-slam', 'var(--clay)'],
  ['advancing-bunt', 'var(--moss)'],
  ['squeeze-bunt', 'var(--sage)'],
  ['advancing-bunt-failure', 'var(--clay)'],
  ['squeeze-bunt-failure', 'var(--ochre)'],
  ['steal-second', 'var(--moss)'],
  ['steal-third', 'var(--sky)'],
  ['steal-failure', 'var(--clay)']
]) {
  assert.match(css, new RegExp(`\\.${kind}\\s*\\{\\s*background:\\s*${color.replace(/[()]/g, '\\$&')};\\s*\\}`), `${kind}を固有の色で表示する`);
}
assert.ok(html.includes('id="share-results"'), '結果をSNS共有できる操作を表示する');
assert.ok(js.includes('navigator.share'), '対応ブラウザではネイティブ共有を使う');
assert.ok(js.includes('clipboard.writeText'), 'ネイティブ共有非対応時は共有文をコピーする');
assert.match(css, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.match(css, /grid-template-columns:\s*38px\s+60px\s+118px\s+96px\s+96px/, '数値入力列は詰めつつ、バント・盗塁の切り替えは1行で読める幅にする');
assert.ok(css.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.ok(html.includes('class="lineup-workspace"'), '打順入力とシミュレーション操作を横並びに配置する');
assert.match(css, /\.section-head\s*\{\s*display:\s*flex;/, '打順入力の見出しと実行操作を横並びにする');
assert.match(html, /<h2 id="order-heading">打順入力<\/h2>\s*<button class="submit"/, '実行操作を打順入力ラベルの直後に置く');
assert.ok(html.includes('id="toggle-all-bunt"'), '全員バントを切り替える操作を表示する');
assert.ok(html.includes('id="toggle-all-steal"'), '全員盗塁を切り替える操作を表示する');
assert.ok(js.includes('targets.every(player=>player.buntEnabled)'), '全員バントが有効なら次の操作で全員無効にする');
assert.ok(js.includes('targets.every(player=>player.stealEnabled)'), '全員盗塁が有効なら次の操作で全員無効にする');
assert.ok(html.includes('class="simulation-workspace"'), '打順入力画面と結果画面を同一のワークスペース内で切り替える');
assert.ok(html.includes('<title>打順監督</title>'), 'ブラウザのタブにサービス名を表示する');
assert.ok(html.includes('<h1>打順監督</h1>'), '画面左上にサービス名を表示する');
assert.ok(!html.includes('Baseball Orders / Simulator'), '旧サービス名を画面から除去する');
assert.ok(!html.includes('LINEUP<br>BUILDER'), '旧見出しを画面から除去する');
assert.ok(!html.includes('id="home-run-empty-state" hidden'), '初期表示から本塁打なしの表示ラベルを隠さない');
assert.match(css, /\.simulation-workspace\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/, '入力画面と結果画面は横並びにせず1画面ずつ表示する');
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
assert.match(css, /\.bunt-toggle\s*\{[^}]*column-gap:\s*\.3em;/, 'トグルの項目名と状態の間に余白を空けて1行で表示する');
assert.match(mobileCss, /\.toggle-label\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では列見出しと重複するトグルの項目名を省略して1行で収める');
for (const [field, minimum, maximum] of [
  ["key:'hitAverage'", 'min:0.01', 'max:0.6']
]) {
  assert.ok(js.includes(field) && js.includes(minimum) && js.includes(maximum), `${field}の入力範囲を画面で制御する`);
}
assert.ok(js.includes("bunt_enabled:player.buntEnabled"), 'バント可否をAPIへ送る');
assert.ok(js.includes("steal_enabled:player.stealEnabled"), '盗塁可否をAPIへ送る');
assert.ok(!js.includes('bunt_success_rate'), 'バント成功率をAPIへ送らない');
assert.ok(!js.includes('steal_success_rate'), '盗塁成功率をAPIへ送らない');
assert.ok(js.includes("toggle('バント',player.buntEnabled"), 'バントを使うかどうかの切り替えボタンは残す');
assert.ok(js.includes("toggle('盗塁',player.stealEnabled"), '盗塁を使うかどうかの切り替えボタンは残す');
assert.ok(js.includes("fetch(lineupFormConfig.endpoint,") && pageJs.includes("endpoint:'/simulations',"), '直接入力をシミュレーションAPIへ送る');
assert.ok(!js.includes('name:'), '固定表示の打者名をAPIへ送らない');
// --- オーガニックな配色（issue #124: 見づらいネオン配色をアースカラーへ置き換える） ---
assert.match(css, /color-scheme:\s*light;/, '暗いネオン調ではなく明るい紙の地色を基調にする');
for (const [name, color] of [
  ['paper', '#f3eee2'],
  ['ink', '#2f3327'],
  ['moss', '#56704a'],
  ['sage', '#9fb08a'],
  ['clay', '#bf6b45'],
  ['ochre', '#d1a046'],
  ['sky', '#6e97a0']
]) {
  assert.match(css, new RegExp(`--${name}:\\s*${color}`), `アースカラーの${name}を配色トークンとして定義する`);
}
for (const neon of ['#25d9ff', '#ff4da6', '#d9ff43', '#875cff', '#070b18']) {
  assert.ok(!css.toLowerCase().includes(neon), `ネオン調の色${neon}をシミュレーション画面に残さない`);
}
assert.ok(!css.includes('background-clip: text'), '見出しをグラデーション文字にせず読みやすい単色にする');
assert.match(css, /\.submit\s*\{[^}]*background:\s*var\(--moss\);/, '主要アクションは落ち着いたモスグリーンの単色で強調する');

// --- 結果画面の可読性改善（Phase1: 空状態・単一指標の見出し統合・横幅・横スクロール） ---
for (const [id, label] of [
  ['game-count', '試合数'],
  ['average-score', '平均得点'],
  ['median-score', '中央値得点'],
  ['maximum-score', '最大得点'],
  ['hit-count', '総安打'],
  ['home-run-count', '本塁打']
]) {
  assert.ok(html.includes(`id="${id}">—<`), `${label}(${id})の初期表示を空文字にせずプレースホルダーを出す`);
}
assert.ok(html.includes('<span class="group-total"><span class="group-total-label">最大得点</span><span class="group-total-value" id="maximum-score">—</span></span>'), '最大得点を単独行にせず見出し行のバッジへ統合する');
assert.ok(html.includes('<span class="group-total"><span class="group-total-label">総安打</span><span class="group-total-value" id="hit-count">—</span></span>'), '総安打を単独行にせず見出し行のバッジへ統合する');
assert.ok(html.includes('<span class="group-total"><span class="group-total-label">本塁打</span><span class="group-total-value" id="home-run-count">—</span></span>'), '本塁打を単独行にせず見出し行のバッジへ統合する');
assert.ok(html.includes('id="score-empty-state">試合結果なし</p>'), '未実行・0試合時に得点サマリーへも内訳グループと同じ空状態表示を出す');
assert.ok(!html.includes('id="score-empty-state" hidden'), '初期表示から得点結果なしの表示ラベルを隠さない');
assert.ok(js.includes("querySelector<HTMLElement>('#score-empty-state')!.hidden=gameCount!==0"), '得点サマリーの空状態を試合数に応じて切り替える');
assert.ok(!js.includes('results.hidden'), '結果パネル全体を毎回消して再描画するとレイアウトが跳ねるため使わない');
assert.ok(html.includes('class="histogram-scroll"'), '得点分布のバー本数が多くても横スクロールで読める幅を確保する');
assert.match(css, /\.histogram-scroll\s*\{\s*overflow-x:\s*auto;/, '得点分布ヒストグラムを横スクロール可能にする');
assert.match(css, /\.histogram-bar\s*\{[^}]*flex:\s*0 0 40px;/, '得点分布の棒を固定幅にして詰まりすぎを防ぐ');

// --- PC入力欄の画面デザイン変更（issue #124: 入力画面と結果画面を分離する） ---
assert.ok(
  !/\.simulation-workspace\s*\{[^}]*minmax\(0,\s*1fr\)\s*minmax\(0,\s*1fr\)/.test(css),
  '入力と結果を2カラムで同時に表示しない'
);
assert.match(
  html,
  /<nav aria-label="画面切り替え" class="view-tabs" role="tablist">/,
  '入力画面と結果画面を切り替えるタブを用意する'
);
assert.match(
  html,
  /<button aria-controls="input-view" aria-selected="true" class="view-tab" id="tab-input" role="tab" type="button">/,
  '初期表示では打順入力画面のタブを選択状態にする'
);
assert.match(
  html,
  /<button aria-controls="results" aria-selected="false" class="view-tab" disabled id="tab-results" role="tab" type="button">/,
  '結果がまだ無い間は試合結果タブを選べなくする'
);
assert.match(
  html,
  /<section aria-labelledby="order-heading" id="input-view" role="tabpanel">/,
  '打順入力を独立した画面パネルにする'
);
assert.match(
  html,
  /<section aria-labelledby="results-heading" hidden id="results" role="tabpanel">/,
  '初期表示では結果画面を隠し、入力画面だけを表示する'
);
assert.ok(html.includes('id="edit-lineup"'), '結果画面から打順入力画面へ戻る操作を用意する');
assert.ok(html.includes('id="result-feedback" role="status"'), '結果画面に実行結果と共有結果を通知する領域を用意する');
assert.ok(js.includes('function showView(resultsVisible: boolean)'), '入力画面と結果画面を排他的に切り替える関数を用意する');
assert.ok(
  js.includes('inputView.hidden=resultsVisible; resultsView.hidden=!resultsVisible;'),
  '入力画面と結果画面は常にどちらか一方だけを表示する'
);
assert.ok(
  pageJs.includes('onSuccess:(data: SimulationResponse)=>{renderResults(data.statistics);}') && lineupFormJs.includes('lineupFormConfig.onSuccess(data);hasResults=true;showView(true);'),
  'シミュレーションが成功したら結果画面へ切り替える'
);
assert.ok(js.includes("editLineup.addEventListener('click',()=>showView(false))"), '結果画面から打順入力画面へ戻れる');
assert.ok(js.includes("tabResults.addEventListener('click',()=>showView(true))"), '入力画面から前回の結果画面へ戻れる');
assert.ok(js.includes('tabResults.disabled=inFlight || !hasResults;'), '結果が無い間と実行中は結果タブを押せなくする');
assert.ok(js.includes("resultFeedback.textContent=gameCount===0?'試合結果はありません。':`試合終了：${gameCount}試合`"), '実行結果の件数を結果画面に表示する');
assert.ok(js.includes("resultFeedback.textContent='共有用テキストをコピーしました。'"), '共有結果を結果画面に表示する');
assert.ok(!html.includes('id="toggle-lineup"'), '入力欄の折りたたみトグルは画面切り替えに置き換える');
assert.ok(!js.includes('setLineupCollapsed'), '入力欄を折りたたむ中途半端な表示を除去する');
assert.ok(
  js.includes('submit.disabled=inFlight || !complete;'),
  '入力画面では入力が有効ならいつでも実行できる'
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
assert.match(
  css,
  /@media \(min-width: 761px\)[\s\S]*\.group-total\s*\{\s*display:\s*none;\s*\}/,
  'PC幅では見出し内の合計バッジを非表示にし統計カード側へ寄せる'
);
assert.ok(
  html.includes('id="maximum-score-card"'),
  'PC幅の得点サマリーに最大得点を独立した統計カードとして追加する'
);
assert.match(
  css,
  /@media \(min-width: 761px\)[\s\S]*\.statistics-summary\s*\{\s*grid-template-columns:\s*repeat\(4,\s*1fr\);\s*\}/,
  'PC幅では得点サマリーの統計カードを4列で横並びにする'
);
assert.match(
  css,
  /@media \(min-width: 761px\)[\s\S]*\.statistics-groups\s*\{\s*grid-template-columns:\s*repeat\(2,\s*1fr\);\s*\}/,
  'PC幅では内訳グループを2列で横並びにする'
);
assert.ok(
  !html.includes('相手投手の性格'),
  '相手投手の性格の入力欄は実際の機能として存在しないため追加しない'
);

// --- 結果画面の可読性改善（Phase2: 内訳グループの折りたたみ・失敗色の統一） ---
for (const heading of ['安打の内訳', '本塁打の内訳', 'バントの内訳', '盗塁の内訳']) {
  assert.ok(html.includes(`<summary class="group-heading">${heading}`), `${heading}グループを折りたたみ可能にする`);
}
assert.ok((html.match(/<details class="statistics-group" open>/g) ?? []).length === 4, '内訳4グループを初期状態では展開したまま折りたたみ可能にする');
assert.ok(html.includes('<h3 class="group-heading">得点サマリー'), '得点サマリーは折りたたまず常に見出しをh3で表示する');
assert.ok(!html.includes('<summary class="group-heading">得点サマリー'), '得点サマリーはdetails/summaryに変更しない');

console.log('PASS: 直接入力、必須値・率の範囲制御、バント選択の送信');
