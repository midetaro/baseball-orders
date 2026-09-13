import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');

assert.ok(html.includes('const lineup = ['), '9人分の固定打順を作成する');
assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(html.includes('一番〜九番として送信します。'), '名前を入力せず打順名を使うことを明示する');
assert.ok(html.includes("const lineup = ["), '打順ごとの役割に応じた初期値を用意する');
assert.ok(html.includes('position.textContent=`${index+1}番`'), '打順は「{数字}番」の固定表示にする');
assert.ok(!html.includes('番打者'), '打順表示に「打者」を付けない');
assert.ok(html.includes("hitAverage:'.270',sluggish:'.350'"), '一、二番に標準打率・低長打率を設定する');
assert.ok(html.includes("hitAverage:'.320',sluggish:'.500'"), '三〜五番に高打率・高長打率を設定する');
assert.ok(html.includes("hitAverage:'.230',sluggish:'.400'"), '六〜九番に低打率・標準長打率を設定する');
assert.ok(html.includes('得点サマリー'), '得点統計を独立したグループとして表示する');
assert.ok(html.includes('本塁打の内訳'), '本塁打統計を構造化して表示する');
assert.ok(html.includes('戦術の成功数'), '戦術統計を構造化して表示する');
assert.ok(html.includes('id="share-results"'), '結果をSNS共有できる操作を表示する');
assert.ok(html.includes('navigator.share'), '対応ブラウザではネイティブ共有を使う');
assert.ok(html.includes('clipboard.writeText'), 'ネイティブ共有非対応時は共有文をコピーする');
assert.ok(html.includes('.order { min-width:640px;'), '入力欄を横に長すぎないコンパクトな幅にする');
assert.ok(html.includes('grid-template-columns:38px repeat(4,minmax(72px,1fr)) 78px 78px'), '入力列と戦術列をコンパクトにする');
for (const [field, minimum, maximum] of [
  ["key:'hitAverage'", 'min:0.005', 'max:0.4'],
  ["key:'sluggish'", 'min:0.1', 'max:0.6'],
  ["key:'stealSuccessRate'", 'min:0.1', 'max:0.9']
]) {
  assert.ok(html.includes(field) && html.includes(minimum) && html.includes(maximum), `${field}の入力範囲を画面で制御する`);
}
assert.ok(html.includes("bunt_enabled:player.buntEnabled"), 'バント可否をAPIへ送る');
assert.ok(html.includes("steal_enabled:player.stealEnabled"), '盗塁可否をAPIへ送る');
assert.ok(html.includes("bunt_success_rate:Number(player.buntSuccessRate)"), 'バント成功率をAPIへ送る');
assert.ok(html.includes("steal_success_rate:Number(player.stealSuccessRate)"), '盗塁成功率をAPIへ送る');
assert.ok(html.includes("fetch('/simulations'"), '直接入力をシミュレーションAPIへ送る');
assert.ok(!html.includes('name:'), '固定表示の打者名をAPIへ送らない');

console.log('PASS: 直接入力、必須値・率の範囲制御、バント選択の送信');
