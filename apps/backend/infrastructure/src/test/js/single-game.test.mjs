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

console.log('PASS: 1試合実行結果のアニメーションフレームと打順成績表の描画');
