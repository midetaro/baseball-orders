import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const guideHtml = readFileSync(new URL('../../main/resources/templates/simulation-guide.html', import.meta.url), 'utf8');

for (const [page, template] of [
  ['シミュレーション画面', html],
  ['シミュレーションガイド画面', guideHtml]
]) {
  assert.ok(template.includes('xmlns:th="http://www.thymeleaf.org"'), `${page}でThymeleafの認証表示を有効にする`);
  assert.ok(template.includes('ログイン中'), `${page}でログイン状態を表示する`);
  assert.ok(template.includes('未ログイン'), `${page}で未ログイン状態を表示する`);
  assert.ok(template.includes('href="/login"'), `${page}にログイン画面への導線を用意する`);
  assert.ok(template.includes("!#authorization.expression('isAuthenticated()')"), `${page}では未認証時にログイン導線を表示する`);
  assert.ok(template.includes('<form action="/logout" method="post"'), `${page}にPOSTログアウトを用意する`);
  assert.ok(template.includes('th:name="${_csrf.parameterName}"'), `${page}のログアウトにCSRFパラメータを含める`);
  assert.ok(template.includes('th:value="${_csrf.token}"'), `${page}のログアウトにCSRFトークンを含める`);
}

assert.ok(html.includes('const lineup = ['), '9人分の固定打順を作成する');
assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(!html.includes('一番〜九番として送信します。'), '不要な固定打順の説明を表示しない');
assert.ok(html.includes("const lineup = ["), '打順ごとの役割に応じた初期値を用意する');
assert.ok(html.includes('position.textContent=`${index+1}番`'), '打順は「{数字}番」の固定表示にする');
assert.ok(!html.includes('番打者'), '打順表示に「打者」を付けない');
assert.ok(html.includes("hitAverage:'0.35',sluggish:'0.35'"), '一、二番に制約内の出塁率・長打率を設定する');
assert.ok(html.includes("hitAverage:'0.35',sluggish:'0.50'"), '中軸に制約内の出塁率・高長打率を設定する');
assert.ok(html.includes("hitAverage:'0.28',sluggish:'0.40'"), '六〜九番に低出塁率・標準長打率を設定する');
assert.ok(html.includes('得点サマリー'), '得点統計を独立したグループとして表示する');
assert.ok(html.includes('出塁率'), '打率ではなく出塁率を入力項目として表示する');
assert.ok(!html.includes("label:'打率'"), '入力項目に打率を表示しない');
assert.ok(html.includes("hitAverage:'0.28',sluggish:'0.40'"), '下位打線に制約内の出塁率・長打率を設定する');
assert.ok(html.includes("buntSuccessRate:'0.70',stealSuccessRate:'0.70'"), 'バントと盗塁の成功率の初期値を7割にする');
assert.ok(!html.includes("stealSuccessRate:'.500'"), '盗塁成功率の初期値を役割ごとに変えない');
assert.ok(!html.includes("buntSuccessRate:'0.80',stealSuccessRate:'0.80'"), 'バントと盗塁の成功率の初期値を8割にしない');
assert.ok(html.includes("buntSuccessRate:[0,0.7], stealSuccessRate:[0.1,0.7]"), 'バントと盗塁の成功率を7割以下に制限する');
assert.ok(html.includes("function validLineup()"), '打順全体の入力制約を検証する');
assert.ok(html.includes("lineup.reduce((sum,player)=>sum+Number(player.hitAverage),0)/lineup.length<=0.35"), '出塁率の平均を3割5分以下に制限する');
assert.ok(html.includes("lineup.reduce((sum,player)=>sum+Number(player.sluggish),0)/lineup.length<=0.4"), '長打率の平均を4割以下に制限する');
assert.ok(html.includes("input.step='0.01'"), '数値入力は小数第2位刻みにする');
assert.ok(html.includes("key:'hitAverage',label:'出塁率',min:0.01,max:0.6"), '出塁率の上限を60%にする');
assert.ok(html.includes('function formatPercentage(value)'), '入力値を小数第2位に整形する');
assert.ok(html.includes('Number(value).toFixed(2)'), '小数第2位のゼロを常に表示する');
assert.ok(html.includes("input.addEventListener('change'"), '入力の確定時に小数第2位へ整形する');
assert.ok(!html.includes("hitAverage:'.32'"), '小数点前のゼロを省略しない');
assert.ok(html.includes("input.value.startsWith('.') ? `0${input.value}` : input.value"), '入力時も小数点前のゼロを表示する');
assert.ok(html.includes('hasAtMostTwoDecimalPlaces'), '小数第3位以降の入力では実行できないようにする');
assert.ok(html.includes('本塁打の内訳'), '本塁打統計を構造化して表示する');
assert.ok(html.includes('id="hit-count"'), '総安打数を表示する');
assert.ok(html.indexOf('id="hit-count"') < html.indexOf('id="home-run-count"'), '総安打数を本塁打の上に表示する');
assert.ok(html.includes("'hitCount'"), 'APIの総安打数を結果へ描画する');
assert.ok(html.includes('安打の内訳'), '安打統計を構造化して表示する');
assert.ok(html.includes('id="hit-breakdown"'), '安打内訳のグラフを表示する');
assert.ok(html.includes('id="hit-legend"'), '安打内訳の凡例を表示する');
assert.ok(html.includes("Array.of('一塁打',statistics.singleHitCount,'single-hit')"), '一塁打数を内訳へ表示する');
assert.ok(html.includes("Array.of('二塁打',statistics.doubleHitCount,'double-hit')"), '二塁打数を内訳へ表示する');
assert.ok(html.includes("Array.of('三塁打',statistics.tripleHitCount,'triple-hit')"), '三塁打数を内訳へ表示する');
assert.ok(html.includes("Array.of('本塁打',statistics.homeRunCount,'home-run')"), '本塁打数を安打内訳へ表示する');
assert.ok(html.includes('`${label} ${count} (${rate.toFixed(1)}%)`'), '安打内訳の凡例で件数と割合を表示する');
assert.ok(!html.includes('戦術の成否'), '重複する戦術統計を表示しない');
assert.ok(!html.includes("Array.of('成功バント',statistics.buntCount)"), '戦術統計の成功バントを表示しない');
assert.ok(!html.includes("Array.of('失敗バント',statistics.buntFailureCount)"), '戦術統計の失敗バントを表示しない');
assert.ok(!html.includes("Array.of('成功盗塁',statistics.stealCount)"), '戦術統計の成功盗塁を表示しない');
assert.ok(!html.includes("Array.of('失敗盗塁',statistics.stealFailureCount)"), '戦術統計の失敗盗塁を表示しない');
assert.ok(html.includes('バントの内訳'), 'バント統計を色分けした内訳として表示する');
assert.ok(html.includes('盗塁の内訳'), '盗塁統計を色分けした内訳として表示する');
assert.ok(html.includes("Array.of('進塁成功',statistics.advancingBuntCount,'advancing-bunt')"), '進塁バント成功数を表示する');
assert.ok(html.includes("Array.of('スクイズ成功',statistics.squeezeBuntCount,'squeeze-bunt')"), 'スクイズ成功数を表示する');
assert.ok(html.includes("Array.of('進塁失敗',statistics.advancingBuntFailureCount,'advancing-bunt-failure')"), '進塁バント失敗数を表示する');
assert.ok(html.includes("Array.of('スクイズ失敗',statistics.squeezeBuntFailureCount,'squeeze-bunt-failure')"), 'スクイズ失敗数を表示する');
assert.ok(html.includes("Array.of('二盗成功',statistics.stealToSecondCount,'steal-second')"), '二盗成功数を表示する');
assert.ok(html.includes("Array.of('三盗成功',statistics.stealToThirdCount,'steal-third')"), '三盗成功数を表示する');
assert.ok(html.includes("Array.of('失敗',statistics.stealFailureCount,'steal-failure')"), '盗塁失敗数を内訳へ表示する');
assert.ok(html.includes('const detailTotal=details.reduce'), '内訳項目の合計を割合の分母にする');
assert.ok(html.includes('Number(count)/detailTotal*100'), '集計総数と内訳合計が異なっても内訳比率を誤表示しない');
assert.ok(html.includes('id="bunt-count"'), '既存の成功バント総数を表示する');
assert.ok(html.includes('id="bunt-failure-count"'), '既存の失敗バント総数を表示する');
assert.ok(html.includes('id="steal-count"'), '既存の成功盗塁総数を表示する');
assert.ok(html.includes('id="steal-failure-count"'), '既存の失敗盗塁総数を表示する');
for (const [kind, color] of [
  ['single-hit', 'var(--lime)'],
  ['double-hit', 'var(--cyan)'],
  ['triple-hit', 'var(--orange)'],
  ['home-run', 'var(--pink)'],
  ['advancing-bunt', 'var(--cyan)'],
  ['squeeze-bunt', 'var(--lime)'],
  ['advancing-bunt-failure', 'var(--orange)'],
  ['squeeze-bunt-failure', 'var(--amber)'],
  ['steal-second', 'var(--cyan)'],
  ['steal-third', 'var(--violet)'],
  ['steal-failure', 'var(--orange)']
]) {
  assert.match(html, new RegExp(`\\.${kind}\\s*\\{\\s*background:\\s*${color.replace(/[()]/g, '\\$&')};\\s*\\}`), `${kind}を固有の色で表示する`);
}
assert.ok(html.includes('id="share-results"'), '結果をSNS共有できる操作を表示する');
assert.ok(html.includes('navigator.share'), '対応ブラウザではネイティブ共有を使う');
assert.ok(html.includes('clipboard.writeText'), 'ネイティブ共有非対応時は共有文をコピーする');
assert.match(html, /\.order\s*\{\s*width:\s*max-content;\s*min-width:\s*570px;/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.match(html, /grid-template-columns:\s*38px\s+repeat\(4,\s*76px\)\s+118px\s+78px\s+78px/, '性格列を含む入力列を読みやすい固定幅にする');
assert.ok(html.includes('class="lineup-workspace"'), '打順入力とシミュレーション操作を横並びに配置する');
assert.match(html, /\.section-head\s*\{\s*display:\s*flex;/, '打順入力の見出しと実行操作を横並びにする');
assert.match(html, /<h2 id="order-heading">打順入力<\/h2>\s*<button class="submit"/, '実行操作を打順入力ラベルの直後に置く');
assert.ok(html.includes('id="toggle-all-bunt"'), '全員バントを切り替える操作を表示する');
assert.ok(html.includes('id="toggle-all-steal"'), '全員盗塁を切り替える操作を表示する');
assert.ok(html.includes('lineup.every(player=>player.buntEnabled)'), '全員バントが有効なら次の操作で全員無効にする');
assert.ok(html.includes('lineup.every(player=>player.stealEnabled)'), '全員盗塁が有効なら次の操作で全員無効にする');
assert.ok(html.includes('class="simulation-workspace"'), '打順入力と結果を同一のワークスペースに配置する');
assert.ok(html.includes('<title>打順監督</title>'), 'ブラウザのタブにサービス名を表示する');
assert.ok(html.includes('<h1>打順監督</h1>'), '画面左上にサービス名を表示する');
assert.ok(!html.includes('Baseball Orders / Simulator'), '旧サービス名を画面から除去する');
assert.ok(!html.includes('LINEUP<br>BUILDER'), '旧見出しを画面から除去する');
assert.ok(!html.includes('id="results" aria-labelledby="results-heading" hidden'), '初期表示から結果の表示ラベルを隠さない');
assert.ok(!html.includes('id="home-run-empty-state" hidden'), '初期表示から本塁打なしの表示ラベルを隠さない');
assert.match(html, /\.simulation-workspace\s*\{\s*display:\s*grid;/, '広い画面では打順入力と結果を横並びにする');
assert.ok(!html.includes("const labelElement=document.createElement('label');"), '各入力セルに列名を重複表示しない');
assert.ok(html.includes("input.setAttribute('aria-label',field.label);"), '列見出しを視覚的に重複させず入力の名称を提供する');
for (const [field, minimum, maximum] of [
  ["key:'hitAverage'", 'min:0.01', 'max:0.6'],
  ["key:'sluggish'", 'min:0.1', 'max:0.6'],
  ["key:'stealSuccessRate'", 'min:0.1', 'max:0.7']
]) {
  assert.ok(html.includes(field) && html.includes(minimum) && html.includes(maximum), `${field}の入力範囲を画面で制御する`);
}
assert.ok(html.includes("bunt_enabled:player.buntEnabled"), 'バント可否をAPIへ送る');
assert.ok(html.includes("steal_enabled:player.stealEnabled"), '盗塁可否をAPIへ送る');
assert.ok(html.includes("bunt_success_rate:Number(player.buntSuccessRate)"), 'バント成功率をAPIへ送る');
assert.ok(html.includes("steal_success_rate:Number(player.stealSuccessRate)"), '盗塁成功率をAPIへ送る');
assert.ok(html.includes("enabledKey:'buntEnabled'"), 'バント成功率はバント選択に連動させる');
assert.ok(html.includes("enabledKey:'stealEnabled'"), '盗塁成功率は盗塁選択に連動させる');
assert.ok(html.includes("input.disabled=inFlight || (field.enabledKey && !player[field.enabledKey]);"), 'バント・盗塁をしない場合は対応する成功率を入力不可にする');
assert.ok(html.includes("fetch('/simulations'"), '直接入力をシミュレーションAPIへ送る');
assert.ok(!html.includes('name:'), '固定表示の打者名をAPIへ送らない');
assert.match(html, /--cyan:\s*#25d9ff/, 'ビビットなシアンを画面全体の強調色に使う');
assert.match(html, /--pink:\s*#ff4da6/, 'ビビットなピンクを画面全体の強調色に使う');
assert.ok(html.includes('radial-gradient(circle at 15% 10%'), '複数の差し色でページ背景に奥行きを作る');
assert.match(html, /linear-gradient\(135deg,\s*var\(--cyan\),\s*var\(--lime\)\)/, '主要アクションを鮮やかなグラデーションで強調する');

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
assert.ok(html.includes("querySelector('#score-empty-state').hidden=gameCount!==0"), '得点サマリーの空状態を試合数に応じて切り替える');
assert.ok(!html.includes('results.hidden'), '結果パネル全体を毎回消して再描画するとレイアウトが跳ねるため使わない');
assert.ok(html.includes('class="histogram-scroll"'), '得点分布のバー本数が多くても横スクロールで読める幅を確保する');
assert.match(html, /\.histogram-scroll\s*\{\s*overflow-x:\s*auto;/, '得点分布ヒストグラムを横スクロール可能にする');
assert.match(html, /\.histogram-bar\s*\{[^}]*flex:\s*0 0 40px;/, '得点分布の棒を固定幅にして詰まりすぎを防ぐ');
assert.match(html, /grid-template-columns:\s*minmax\(0,\s*1fr\)\s*minmax\(420px,\s*1\.05fr\)/, '結果パネルに入力パネル以上の幅を割り当てる');

// --- 結果画面の可読性改善（Phase2: 内訳グループの折りたたみ・失敗色の統一） ---
assert.match(html, /--amber:\s*#ffb12b/, 'バント失敗系統の識別用にアンバーの変数を追加する');
for (const heading of ['安打の内訳', '本塁打の内訳', 'バントの内訳', '盗塁の内訳']) {
  assert.ok(html.includes(`<summary class="group-heading">${heading}`), `${heading}グループを折りたたみ可能にする`);
}
assert.ok((html.match(/<details class="statistics-group" open>/g) ?? []).length === 4, '内訳4グループを初期状態では展開したまま折りたたみ可能にする');
assert.ok(html.includes('<h3 class="group-heading">得点サマリー'), '得点サマリーは折りたたまず常に見出しをh3で表示する');
assert.ok(!html.includes('<summary class="group-heading">得点サマリー'), '得点サマリーはdetails/summaryに変更しない');

console.log('PASS: 直接入力、必須値・率の範囲制御、バント選択の送信');
