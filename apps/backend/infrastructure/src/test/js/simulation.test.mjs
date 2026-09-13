import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');

assert.ok(html.includes('Array.from({length: 9}'), '9人分の固定打順を作成する');
assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(html.includes('一番〜九番として送信します。'), '名前を入力せず打順名を使うことを明示する');
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

console.log('PASS: 直接入力、必須値・率の範囲制御、バント選択の送信');
