import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  SUMMARY_KEYS,
  breakdownGroups,
  scoreHistogram,
  summarizeBreakdown,
  summaryElementId,
} from '../../../../main/typescript/simulation/statistics.ts';

test('集計項目は項目名をケバブケースにしたIDの要素へ表示する', () => {
  assert.deepEqual(SUMMARY_KEYS.map(summaryElementId), [
    'game-count', 'average-score', 'median-score', 'maximum-score', 'hit-count',
    'home-run-count', 'bunt-count', 'bunt-failure-count', 'steal-count', 'steal-failure-count',
  ]);
});

test('安打・本塁打・バント・盗塁の内訳を、APIの件数と色分けのクラスで組み立てる', () => {
  const statistics = {
    singleHitCount: 1, doubleHitCount: 2, tripleHitCount: 3, homeRunCount: 4,
    soloHomeRunCount: 5, twoRunHomeRunCount: 6, threeRunHomeRunCount: 7, grandSlamCount: 8,
    advancingBuntCount: 9, squeezeBuntCount: 10, advancingBuntFailureCount: 11, squeezeBuntFailureCount: 12,
    stealToSecondCount: 13, stealToThirdCount: 14, stealFailureCount: 15,
    buntCount: 99, buntFailureCount: 99, stealCount: 99,
  };
  const groups = breakdownGroups(statistics).map(({ prefix, items }) => [prefix, items.map(({ label, count, kind }) => `${label}:${count}:${kind}`)]);
  assert.deepEqual(groups, [
    ['hit', ['一塁打:1:single-hit', '二塁打:2:double-hit', '三塁打:3:triple-hit', '本塁打:4:home-run']],
    ['home-run', ['ソロ:5:solo', 'ツーラン:6:two-run', 'スリーラン:7:three-run', '満塁:8:grand-slam']],
    ['bunt', ['進塁成功:9:advancing-bunt', 'スクイズ成功:10:squeeze-bunt', '進塁失敗:11:advancing-bunt-failure', 'スクイズ失敗:12:squeeze-bunt-failure']],
    ['steal', ['二盗成功:13:steal-second', '三盗成功:14:steal-third', '失敗:15:steal-failure']],
  ], '成功・失敗の総数は内訳に重複して表示しない');
});

test('内訳の割合は内訳項目の合計を分母にし、件数0の項目は描かない', () => {
  const { total, segments } = summarizeBreakdown([
    { label: 'A', count: 3, kind: 'a' },
    { label: 'B', count: 0, kind: 'b' },
    { label: 'C', count: 1, kind: 'c' },
    { label: 'D', count: undefined, kind: 'd' },
  ]);
  assert.equal(total, 4);
  assert.deepEqual(segments, [{ label: 'A', count: 3, kind: 'a', rate: 75 }, { label: 'C', count: 1, kind: 'c', rate: 25 }]);
  assert.deepEqual(summarizeBreakdown([{ label: 'A', count: 0, kind: 'a' }]), { total: 0, segments: [] }, '件数が無ければ空にする');
});

test('得点分布は得点の昇順に全試合に対する割合で並べ、目盛りの上限を25%刻みで切り上げる', () => {
  const histogram = scoreHistogram({ gameCount: 10, scoreDistribution: { 10: 1, 2: 3, 0: 6 } });
  assert.deepEqual(histogram.bars.map(bar => [bar.score, bar.rate, bar.height]), [['0', 60, 80], ['2', 30, 40], ['10', 10, (10 / 75) * 100]]);
  assert.equal(histogram.maximum, 75);
  assert.deepEqual(histogram.axisLabels, [75, 50, 25, 0]);
  assert.equal(histogram.gridStep, (25 / 75) * 100);
});

test('得点分布が無い・試合数が0なら、目盛りは最低の25%にして棒の高さを0にする', () => {
  assert.deepEqual(scoreHistogram({}), { maximum: 25, axisLabels: [25, 0], gridStep: 100, bars: [] });
  assert.deepEqual(scoreHistogram({ gameCount: 0, scoreDistribution: { 1: 2 } }).bars, [{ score: '1', rate: 0, height: 0 }]);
});
