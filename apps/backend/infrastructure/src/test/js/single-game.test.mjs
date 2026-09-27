import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/single-game.html', import.meta.url), 'utf8');
assert.match(html, /\.out-count\s*\{\s*color:\s*#ff4d4d;/, 'アウト数を赤色で表示する');
assert.ok(html.includes("span.className='out-count'"), 'アウト表示に専用スタイルを適用する');
for (const name of ['single-game', 'simulation', 'simulation-guide']) {
  const template = readFileSync(new URL(`../../main/resources/templates/${name}.html`, import.meta.url), 'utf8');
  assert.ok(template.includes('長距離砲'), `${name}の性格名は長距離砲`);
  assert.ok(!template.includes('ブンブン丸'), `${name}に旧名称を残さない`);
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
  html.includes("frameStage.dataset.frameDurationMillis"),
  'データ属性からフレーム間隔を読み取る'
);

assert.ok(html.includes('function annotateBattingOrder('), '状況推移から打順を推測する関数を用意する');
assert.ok(
  html.includes("actionResult.startsWith('盗塁')"),
  '盗塁のみの推移は打者の打席結果として扱わない'
);
assert.ok(
  html.includes('battingOrder === 9 ? 1 : battingOrder + 1'),
  '打順は1から9を継続して循環させる'
);

assert.ok(html.includes('function buildFrame('), '1推移から1フレームを生成する関数を用意する');
assert.ok(html.includes("'走者なし':{first:false,second:false,third:false}"), '走者なしの塁配置を定義する');
assert.ok(html.includes("'満塁':{first:true,second:true,third:true}"), '満塁の塁配置を定義する');
assert.ok(html.includes("'一・二塁':{first:true,second:true,third:false}"), '一・二塁の塁配置を定義する');
assert.ok(html.includes("class='diamond'") || html.includes('diamond.className'), 'ダイヤモンドを描画する');
assert.ok(html.includes("'●'.repeat"), 'アウトカウントを記号で表現する');

assert.ok(html.includes('function playFrames('), 'フレームを一定間隔で再生する関数を用意する');
assert.ok(html.includes('setInterval('), 'フレームをコマ送りで自動再生する');
assert.ok(html.includes('function stopFramePlayback('), '再実行時に前回の再生を止める');

assert.ok(html.includes('function buildOrderTable('), 'イニング×打順の結果表を生成する関数を用意する');
assert.ok(
  html.includes('battingOrder <= 9'),
  '打順表の行は固定の9行にする'
);
assert.ok(html.includes('id="order-table-scroll"'), '結果表を横スクロール可能な領域に置く');

assert.ok(html.includes('function renderGame('), '推移全体からフレーム再生と結果表を組み立てる関数を用意する');
assert.ok(html.includes('renderGame(data.transitions)'), '実行結果の描画をrenderGameへ切り替える');
assert.ok(!html.includes('renderTransitions'), '旧いテキスト描画関数を除去する');
assert.ok(
  html.includes('<script th:inline="none">'),
  '配列リテラルの[[が自動インライン化と誤認されないようスクリプトのインライン化を無効にする'
);

// --- 打順入力と結果表示の縦並び配置 ---
assert.match(
  html,
  /\.simulation-workspace\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/,
  '打順入力と結果を画面幅によらず縦に並べる'
);
assert.ok(
  !/@media[^}]*\{\s*\.simulation-workspace\s*\{\s*grid-template-columns/s.test(html),
  '縦並びが既定のため画面幅切り替え用のグリッド定義を残さない'
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
  html.includes('function setLineupCollapsed('),
  '折りたたみ状態を切り替える関数を用意する'
);
assert.ok(
  html.includes("toggleLineup.addEventListener('click',()=>setLineupCollapsed(!lineupBody.hidden))"),
  'トグルボタンで開閉を手動切り替えできる'
);
assert.ok(
  html.includes('renderGame(data.transitions);setLineupCollapsed(true);'),
  '1試合実行が成功したら打順入力欄を自動で折りたたむ'
);
assert.ok(
  html.includes('submit.disabled=inFlight || !complete || lineupBody.hidden;'),
  '打順入力欄が閉じている間は1試合実行ボタンを押せなくする'
);

// --- 打順入力欄の横スクロール解消（数値入力の余白削減とスマホ表示のカード化） ---
assert.match(html, /\.order\s*\{\s*width:\s*max-content;\s*padding:/, '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.match(html, /grid-template-columns:\s*38px\s+repeat\(3,\s*60px\)\s+118px\s+78px\s+78px/, '数値入力列の余白を詰めて幅を最小限にする');
assert.ok(html.includes('input[type="number"]::-webkit-inner-spin-button'), '数値入力のスピンボタンを除去して余白を詰める');
assert.ok(html.includes('function fieldWrapper('), '各入力欄をキャプション付きのフィールドとして構成する');
assert.match(html, /\.field-caption\s*\{\s*display:\s*none;\s*\}/, '通常幅では列見出しと入力キャプションを重複表示しない');
assert.match(html, /@media \(max-width: 760px\)[\s\S]*\.field-caption\s*\{\s*display:\s*block;\s*\}/, 'スマホ幅では列見出しの代わりに入力キャプションを表示する');
assert.match(html, /@media \(max-width: 760px\)[\s\S]*\.columns\s*\{\s*display:\s*none;\s*\}/, 'スマホ幅では横スクロールが必要な列見出し行を表示しない');
assert.match(html, /@media \(max-width: 760px\)[\s\S]*\.order-scroll\s*\{\s*overflow-x:\s*visible;\s*\}/, 'スマホ幅では打順入力欄の横スクロールを不要にする');
assert.match(html, /@media \(max-width: 760px\)[\s\S]*\.slot\s*\{\s*display:\s*grid;\s*grid-template-columns:\s*1fr;/, 'スマホ幅では打順入力行を打者ごとに縦一列のカードへ切り替える');

// --- バント成功率の入力欄を削除する（1試合実行画面のみ） ---
assert.ok(!html.includes("key:'buntSuccessRate'"), '1試合実行画面ではバント成功率の入力欄を表示しない');
assert.ok(!html.includes('バント成功率'), '1試合実行画面ではバント成功率のラベル・列見出しを表示しない');
assert.ok(html.includes("buntSuccessRate:'0.70'"), 'バント成功率は入力欄なしで既定値のままAPIへ送る');
assert.ok(html.includes('bunt_success_rate:Number(player.buntSuccessRate)'), 'バント成功率の既定値をAPIへ送信する');
assert.ok(html.includes("toggle('バント',player.buntEnabled"), 'バントを使うかどうかの切り替えボタンは残す');

console.log('PASS: 1試合実行結果のアニメーションフレームと打順成績表の描画');
