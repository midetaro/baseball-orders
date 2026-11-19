import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const guideHtml = readFileSync(new URL('../../main/resources/templates/simulation-guide.html', import.meta.url), 'utf8');
const css = readFileSync(new URL('../../main/resources/static/css/simulation.css', import.meta.url), 'utf8');

// --- HTMLとCSSのファイル分離 ---
assert.ok(!html.includes('<style>'), 'シミュレーション画面のCSSをインラインで保持しない');
assert.ok(html.includes('<link rel="stylesheet" href="/css/simulation.css">'), 'シミュレーション画面から分離したCSSファイルを読み込む');

// --- HTMLとJSのファイル分離（読み込むスクリプトは scripts/pages.test.mjs で検査する） ---
assert.ok(!html.includes('<script>'), 'シミュレーション画面のJSをインラインで保持しない');

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

assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(!html.includes('一番〜九番として送信します。'), '不要な固定打順の説明を表示しない');
assert.ok(html.includes('得点サマリー'), '得点統計を独立したグループとして表示する');
assert.ok(html.includes('<span>打率</span>'), '打率を入力項目として表示する');
assert.ok(!html.includes('出塁率'), '旧入力名を表示しない');
assert.ok(!html.includes('長打率'), 'シミュレーション画面に長打率を表示しない');
assert.ok(!html.includes('バント成功率'), 'バント成功率のラベル・列見出しを表示しない');
assert.ok(!html.includes('盗塁成功率'), '盗塁成功率のラベル・列見出しを表示しない');
assert.ok(html.includes('本塁打の内訳'), '本塁打統計を構造化して表示する');
assert.ok(html.includes('id="hit-count"'), '総安打数を表示する');
assert.ok(html.indexOf('id="hit-count"') < html.indexOf('id="home-run-count"'), '総安打数を本塁打の上に表示する');
assert.ok(html.includes('安打の内訳'), '安打統計を構造化して表示する');
assert.ok(html.includes('id="hit-breakdown"'), '安打内訳のグラフを表示する');
assert.ok(html.includes('id="hit-legend"'), '安打内訳の凡例を表示する');
assert.ok(!html.includes('戦術の成否'), '重複する戦術統計を表示しない');
assert.ok(html.includes('バントの内訳'), 'バント統計を色分けした内訳として表示する');
assert.ok(html.includes('盗塁の内訳'), '盗塁統計を色分けした内訳として表示する');
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
assert.match(css, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.match(css, /grid-template-columns:\s*38px\s+60px\s+118px\s+96px\s+96px/, '数値入力列は詰めつつ、バント・盗塁の切り替えは1行で読める幅にする');
assert.ok(css.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.ok(html.includes('class="lineup-workspace"'), '打順入力とシミュレーション操作を横並びに配置する');
assert.match(css, /\.section-head\s*\{\s*display:\s*flex;/, '打順入力の見出しと実行操作を横並びにする');
assert.match(html, /<h2 id="order-heading">打順入力<\/h2>\s*<button class="submit"/, '実行操作を打順入力ラベルの直後に置く');
assert.ok(html.includes('id="toggle-all-bunt"'), '全員バントを切り替える操作を表示する');
assert.ok(html.includes('id="toggle-all-steal"'), '全員盗塁を切り替える操作を表示する');
assert.ok(html.includes('class="simulation-workspace"'), '打順入力画面と結果画面を同一のワークスペース内で切り替える');
assert.ok(html.includes('<title>打順監督</title>'), 'ブラウザのタブにサービス名を表示する');
assert.ok(html.includes('<h1>打順監督</h1>'), '画面左上にサービス名を表示する');
assert.ok(!html.includes('Baseball Orders / Simulator'), '旧サービス名を画面から除去する');
assert.ok(!html.includes('LINEUP<br>BUILDER'), '旧見出しを画面から除去する');
assert.ok(!html.includes('id="home-run-empty-state" hidden'), '初期表示から本塁打なしの表示ラベルを隠さない');
assert.match(css, /\.simulation-workspace\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/, '入力画面と結果画面は横並びにせず1画面ずつ表示する');
assert.match(css, /\.field-caption\s*\{\s*display:\s*none;\s*\}/, '通常幅では列見出しと入力キャプションを重複表示しない');
const mobileCss = css.slice(css.indexOf('@media (max-width: 760px)'));
assert.ok(!/\.field-caption\s*\{\s*display:\s*block;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく入力キャプションを重複表示しない');
assert.ok(!/\.columns\s*\{\s*display:\s*none;\s*\}/.test(mobileCss), 'スマホ幅でもPC幅と同じく列見出し行を表示する');
assert.match(mobileCss, /\.order-scroll\s*\{\s*overflow-x:\s*visible;\s*\}/, 'スマホ幅では打順入力欄の横スクロールを不要にする');
assert.match(mobileCss, /\.columns, \.slot\s*\{\s*grid-template-columns:\s*24px\s+minmax\(0,\s*\.8fr\)\s+minmax\(0,\s*1\.4fr\)\s+repeat\(2,\s*minmax\(0,\s*\.8fr\)\);/, 'スマホ幅でもPC幅と同じく打者ごとに一行で入力欄を横に並べる');
assert.match(css, /\.toggle-label, \.toggle-state\s*\{\s*white-space:\s*nowrap;\s*\}/, 'トグルの項目名と状態はそれぞれ語の途中で折り返さない');
assert.match(css, /\.bunt-toggle\s*\{[^}]*column-gap:\s*\.3em;/, 'トグルの項目名と状態の間に余白を空けて1行で表示する');
assert.match(mobileCss, /\.toggle-label\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では列見出しと重複するトグルの項目名を省略して1行で収める');
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
assert.ok(!html.includes('id="toggle-lineup"'), '入力欄の折りたたみトグルは画面切り替えに置き換える');
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
