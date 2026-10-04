import assert from 'node:assert/strict';
import { test } from 'node:test';
import { annotateBattingOrder, classifyEffect, resolvePlayOutcomes, toPlays } from '../../../../main/typescript/single-game/plays.ts';

const transition = (actionResult, cumulativeScore) => ({ actionResult, cumulativeScore });

test('本塁打・得点・安打の演出を選ぶ（issue #141）', () => {
  assert.deepEqual(classifyEffect(transition('本塁打', 1), 0), { kind: 'home-run', runs: 1, bases: 4 }, 'ソロ本塁打は本塁打演出にする');
  assert.deepEqual(classifyEffect(transition('本塁打', 6), 2), { kind: 'home-run', runs: 4, bases: 4 }, '満塁本塁打は4点の本塁打演出にする');
  assert.deepEqual(classifyEffect(transition('二塁打', 3), 1), { kind: 'score', runs: 2, bases: 2 }, '得点が入った安打は得点演出にし、打者走者の進塁数を保つ');
  assert.deepEqual(classifyEffect(transition('スクイズ成功', 1), 0), { kind: 'score', runs: 1, bases: 0 }, '安打以外でも得点が入れば得点演出を優先する');
  assert.deepEqual(classifyEffect(transition('四球', 2), 1), { kind: 'score', runs: 1, bases: 0 }, '押し出しも得点演出にする');
  assert.deepEqual(classifyEffect(transition('単打', 0), 0), { kind: 'hit', runs: 0, bases: 1 });
  assert.deepEqual(classifyEffect(transition('二塁打', 0), 0), { kind: 'hit', runs: 0, bases: 2 });
  assert.deepEqual(classifyEffect(transition('三塁打', 2), 2), { kind: 'hit', runs: 0, bases: 3 });
  assert.deepEqual(classifyEffect(transition('バント成功', 0), 0), { kind: 'bunt', runs: 0, bases: 0 }, '得点のない進塁バント成功はバント演出にする（issue #156）');
  for (const result of ['凡打', '三振', '四球', 'バント失敗', 'スクイズ失敗', '盗塁成功(二塁)', '盗塁失敗(三塁)']) {
    assert.deepEqual(classifyEffect(transition(result, 0), 0), { kind: 'none', runs: 0, bases: 0 }, `得点のない${result}は通常表示にする`);
  }
});

test('記録はプレー直前の状況なので、次の推移からプレー直後のアウト・走者・得点を求め、イニングや試合の最後のプレーは3アウトにする', () => {
  const recorded = [
    { inning: 1, actionResult: '三塁打', outCount: 1, cumulativeScore: 0, runnerState: '一・二塁' },
    { inning: 1, actionResult: '本塁打', outCount: 1, cumulativeScore: 2, runnerState: '三塁' },
    { inning: 1, actionResult: '凡打', outCount: 1, cumulativeScore: 4, runnerState: '走者なし' },
    { inning: 1, actionResult: '三振', outCount: 2, cumulativeScore: 4, runnerState: '走者なし' },
    { inning: 2, actionResult: '単打', outCount: 0, cumulativeScore: 4, runnerState: '走者なし' },
  ];
  assert.deepEqual(resolvePlayOutcomes(recorded), [
    { inning: 1, actionResult: '三塁打', outCount: 1, cumulativeScore: 2, runnerState: '三塁', scoreBefore: 0 },
    { inning: 1, actionResult: '本塁打', outCount: 1, cumulativeScore: 4, runnerState: '走者なし', scoreBefore: 2 },
    { inning: 1, actionResult: '凡打', outCount: 2, cumulativeScore: 4, runnerState: '走者なし', scoreBefore: 4 },
    { inning: 1, actionResult: '三振', outCount: 3, cumulativeScore: 4, runnerState: '走者なし', scoreBefore: 4 },
    { inning: 2, actionResult: '単打', outCount: 3, cumulativeScore: 4, runnerState: '走者なし', scoreBefore: 4 },
  ]);
});

test('打順は1から9を循環させ、盗塁のみの推移は打席として数えない', () => {
  const results = ['単打', '盗塁成功(二塁)', '凡打', ...Array(7).fill('三振'), '四球'];
  assert.deepEqual(annotateBattingOrder(results.map(actionResult => ({ actionResult }))).map(play => play.battingOrder), [1, 2, 2, 3, 4, 5, 6, 7, 8, 9, 1]);
});

test('プレー直前と直後の得点差で演出を決め、打球の方向は左・中・右を順に繰り返す', () => {
  const plays = toPlays([
    { inning: 1, actionResult: '二塁打', outCount: 0, cumulativeScore: 0, runnerState: '一塁' },
    { inning: 1, actionResult: '単打', outCount: 0, cumulativeScore: 1, runnerState: '二塁' },
    { inning: 1, actionResult: '盗塁成功(三塁)', outCount: 0, cumulativeScore: 1, runnerState: '一・二塁' },
    { inning: 1, actionResult: '三振', outCount: 0, cumulativeScore: 1, runnerState: '一・三塁' },
  ]);
  assert.deepEqual(plays.map(play => [play.battingOrder, play.effect.kind, play.effect.runs, play.direction]), [
    [1, 'score', 1, 'left'],
    [2, 'hit', 0, 'center'],
    [3, 'none', 0, 'right'],
    [3, 'none', 0, 'left'],
  ]);
});
