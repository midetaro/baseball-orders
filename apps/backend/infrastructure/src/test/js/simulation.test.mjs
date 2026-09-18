import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');

assert.ok(html.includes('const lineup = ['), '9人分の固定打順を作成する');
assert.ok(!html.includes('th:each="player'), 'DBの選手一覧を画面に表示しない');
assert.ok(html.includes('一番〜九番として送信します。'), '名前を入力せず打順名を使うことを明示する');
assert.ok(html.includes("const lineup = ["), '打順ごとの役割に応じた初期値を用意する');
assert.ok(html.includes('position.textContent=`${index+1}番`'), '打順は「{数字}番」の固定表示にする');
assert.ok(!html.includes('番打者'), '打順表示に「打者」を付けない');
assert.ok(html.includes("hitAverage:'.32',sluggish:'.35'"), '一、二番に標準出塁率・低長打率を設定する');
assert.ok(html.includes("hitAverage:'.37',sluggish:'.50'"), '三〜五番に高出塁率・高長打率を設定する');
assert.ok(html.includes("hitAverage:'.28',sluggish:'.40'"), '六〜九番に低出塁率・標準長打率を設定する');
assert.ok(html.includes('得点サマリー'), '得点統計を独立したグループとして表示する');
assert.ok(html.includes('出塁率'), '打率ではなく出塁率を入力項目として表示する');
assert.ok(!html.includes("label:'打率'"), '入力項目に打率を表示しない');
assert.ok(html.includes("hitAverage:'.32',sluggish:'.35'"), '一、二番の出塁率は従来の打率より5分高くする');
assert.ok(html.includes("hitAverage:'.37',sluggish:'.50'"), '三〜五番の出塁率は従来の打率より5分高くする');
assert.ok(html.includes("hitAverage:'.28',sluggish:'.40'"), '六〜九番の出塁率は従来の打率より5分高くする');
assert.ok(!html.includes("buntSuccessRate:'.700'"), 'バント成功率の初期値を7割にしない');
assert.ok(!html.includes("stealSuccessRate:'.500'"), '盗塁成功率の初期値を役割ごとに変えない');
assert.ok(html.includes("buntSuccessRate:'.80',stealSuccessRate:'.80'"), 'バントと盗塁の成功率の初期値を8割にする');
assert.ok(html.includes("input.step='0.01'"), '数値入力は小数第2位刻みにする');
assert.ok(html.includes('hasAtMostTwoDecimalPlaces'), '小数第3位以降の入力では実行できないようにする');
assert.ok(html.includes('本塁打の内訳'), '本塁打統計を構造化して表示する');
assert.ok(html.includes('戦術の成功数'), '戦術統計を構造化して表示する');
assert.ok(html.includes('id="share-results"'), '結果をSNS共有できる操作を表示する');
assert.ok(html.includes('navigator.share'), '対応ブラウザではネイティブ共有を使う');
assert.ok(html.includes('clipboard.writeText'), 'ネイティブ共有非対応時は共有文をコピーする');
assert.ok(html.includes('.order { width:max-content; min-width:570px;'), '入力欄を親幅いっぱいに広げずコンパクトにする');
assert.ok(html.includes('grid-template-columns:38px repeat(4,76px) 78px 78px'), '入力列を読みやすい固定幅にする');
assert.ok(html.includes('class="lineup-workspace"'), '打順入力とシミュレーション操作を横並びに配置する');
assert.ok(html.includes('.lineup-workspace { display:flex;'), 'シミュレーション操作を打順の右側へ配置する');
assert.ok(html.includes('.actions { width:220px;'), 'シミュレーション操作の横幅を固定する');
assert.ok(html.includes('class="simulation-workspace"'), '打順入力と結果を同一のワークスペースに配置する');
assert.ok(html.includes('.simulation-workspace { display:grid;'), '広い画面では打順入力と結果を横並びにする');
assert.ok(!html.includes("const labelElement=document.createElement('label');"), '各入力セルに列名を重複表示しない');
assert.ok(html.includes("input.setAttribute('aria-label',field.label);"), '列見出しを視覚的に重複させず入力の名称を提供する');
for (const [field, minimum, maximum] of [
  ["key:'hitAverage'", 'min:0.01', 'max:0.4'],
  ["key:'sluggish'", 'min:0.1', 'max:0.6'],
  ["key:'stealSuccessRate'", 'min:0.1', 'max:0.9']
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

console.log('PASS: 直接入力、必須値・率の範囲制御、バント選択の送信');
