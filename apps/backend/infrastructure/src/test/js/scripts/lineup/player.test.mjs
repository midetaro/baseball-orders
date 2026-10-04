import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  HIT_AVERAGE_RANGE_MESSAGE,
  PERSONALITY_LABELS,
  averageHitAverage,
  createInitialLineup,
  formatHitAverage,
  isValidLineup,
  isValidPlayer,
  movePlayer,
  parseDefaultBatters,
  toLineupRequest,
  withLeadingZero,
} from '../../../../main/typescript/lineup/player.ts';

const hit = values => values.map(hitAverage => ({ hitAverage }));

test('性格は単打マン・中距離砲・長距離砲・高出塁率・盗塁重視・バント職人の順に選択できる（issue #145）', () => {
  assert.deepEqual(Object.entries(PERSONALITY_LABELS), [
    ['DEFAULT', '単打マン'],
    ['MIDDLE_DISTANCE', '中距離砲'],
    ['EAGER_SLUGGISH', '長距離砲'],
    ['HIGH_ON_BASE', '高出塁率'],
    ['EAGER_STEAL', '盗塁重視'],
    ['EAGER_BUNT', 'バント職人'],
  ]);
});

test('初期打順は一〜五番を打率0.35、六番以降を打順が下がるほど低い打率にし、三〜五番だけバント・盗塁をしない', () => {
  const lineup = createInitialLineup();
  assert.deepEqual(lineup.map(player => player.hitAverage), ['0.35', '0.35', '0.35', '0.35', '0.35', '0.33', '0.29', '0.28', '0.27']);
  assert.deepEqual(lineup.map(player => player.buntEnabled), [true, true, false, false, false, true, true, true, true]);
  assert.deepEqual(lineup.map(player => player.stealEnabled), [true, true, false, false, false, true, true, true, true]);
  assert.ok(lineup.every(player => player.personality === 'DEFAULT'));
  assert.notEqual(createInitialLineup()[0], lineup[0], '画面ごとに別の打順を作る');
});

test('打率は0.01〜0.6の範囲で入力済みなら有効にする', () => {
  for (const [value, expected] of [['0.35', true], ['0.01', true], ['0.6', true], ['0.61', false], ['0.009', false], ['', false], ['abc', false]]) {
    assert.equal(isValidPlayer({ hitAverage: value }), expected, `${value || '未入力'}`);
  }
  assert.equal(isValidLineup(hit(['0.3', '0.3'])), true);
  assert.equal(isValidLineup(hit(['0.3', ''])), false, '1人でも無効なら打順全体を無効にする');
  assert.equal(HIT_AVERAGE_RANGE_MESSAGE, '打率は0.01〜0.6の範囲ですべての項目を入力してください。');
});

test('平均打率を小数第3位で表示し、無効な打率があれば表示しない', () => {
  assert.equal(averageHitAverage(hit(['0.300', '0.300', '0.300', '0.300', '0.300', '0.300', '0.300', '0.300', '0.255'])), '0.295');
  assert.equal(averageHitAverage(hit(['0.300', ''])), '—', '未入力があれば表示しない');
  assert.equal(averageHitAverage(hit(['0.300', '0.9'])), '—', '範囲外があれば表示しない');
});

test('入力中は小数点前の0を補い、確定時は小数第2位にそろえる', () => {
  assert.equal(withLeadingZero('.3'), '0.3');
  assert.equal(withLeadingZero('0.3'), '0.3');
  assert.equal(formatHitAverage('0.3'), '0.30');
  assert.equal(formatHitAverage('0.4'), '0.40', '小数第2位のゼロも表示する');
  assert.equal(formatHitAverage(''), '', '未入力はそのまま残す');
});

test('APIへは打率・バント可否・盗塁可否・性格だけを送り、メモや固定フラグは送らない', () => {
  const request = toLineupRequest([{ hitAverage: '0.300', buntEnabled: true, stealEnabled: false, personality: 'EAGER_BUNT', memo: '秘密', buntForced: true }]);
  assert.deepEqual(request, [{ hit_average: 0.3, bunt_enabled: true, steal_enabled: false, personality: 'EAGER_BUNT' }]);
});

test('チーム既定の打者は、固定された盗塁・バントだけを有効にして始める', () => {
  const parsed = parseDefaultBatters([
    { dataset: { hitAverage: '0.300', personality: 'EAGER_STEAL', stealForced: 'true', buntForced: 'false' } },
    { dataset: { hitAverage: '0.250', personality: 'DEFAULT', stealForced: 'false', buntForced: 'false' } },
  ]);
  assert.deepEqual(parsed[0], { hitAverage: '0.300', personality: 'EAGER_STEAL', stealForced: true, buntForced: false, stealEnabled: true, buntEnabled: false, memo: '' });
  assert.deepEqual([parsed[1].stealEnabled, parsed[1].buntEnabled], [false, false]);
});

test('打者を移動先の打順へ差し込み、間の打者を詰める', () => {
  const players = () => ['A', 'B', 'C', 'D'];
  const down = players();
  assert.equal(movePlayer(down, 0, 2), true);
  assert.deepEqual(down, ['B', 'C', 'A', 'D'], '下へ移動すると間の打者を繰り上げる');
  const up = players();
  assert.equal(movePlayer(up, 3, 1), true);
  assert.deepEqual(up, ['A', 'D', 'B', 'C'], '上へ移動すると間の打者を繰り下げる');
  for (const [from, to] of [[1, 1], [0, -1], [3, 4]]) {
    const unchanged = players();
    assert.equal(movePlayer(unchanged, from, to), false, `${from}から${to}へは移動しない`);
    assert.deepEqual(unchanged, players());
  }
});
