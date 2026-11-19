import assert from 'node:assert/strict';
import { test } from 'node:test';
import { FramePlayer, PLAYBACK_SPEED_MULTIPLIERS, bindPlaybackSpeedOptions, readFrameDurations } from '../../../../main/typescript/single-game/playback.ts';
import { toPlays } from '../../../../main/typescript/single-game/plays.ts';
import { installDom } from '../../support/dom.mjs';

const durations = { none: 1000, hit: 1200, score: 1400, 'home-run': 2000, bunt: 1600 };
const transitions = [
  { inning: 1, actionResult: '単打', outCount: 0, cumulativeScore: 0, runnerState: '走者なし' },
  { inning: 2, actionResult: '本塁打', outCount: 0, cumulativeScore: 0, runnerState: '一塁' },
  { inning: 2, actionResult: '三振', outCount: 0, cumulativeScore: 2, runnerState: '走者なし' },
];

/** フレーム表示領域とスコアボードを用意し、タイマーを差し替えてプレーヤーを作る。 */
function setup(t) {
  installDom('<div id="stage"></div><div id="line-score"></div>');
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const stage = document.querySelector('#stage');
  const lineScore = document.querySelector('#line-score');
  return { stage, lineScore, sut: new FramePlayer(stage, lineScore, durations) };
}
const banner = stage => stage.querySelector('.frame-banner').textContent;
const currentInning = lineScore => lineScore.querySelector('thead .is-current')?.textContent ?? null;

test('フレーム表示時間をdata属性から読み、演出ごとの時間が無ければ通常の時間を使う', () => {
  installDom('<div id="stage" data-frame-duration-millis="800" data-hit-frame-duration-millis="1200" data-home-run-frame-duration-millis="2400"></div><div id="empty"></div>');
  assert.deepEqual(readFrameDurations(document.querySelector('#stage')), { none: 800, hit: 1200, score: 800, 'home-run': 2400, bunt: 800 });
  assert.deepEqual(readFrameDurations(document.querySelector('#empty')), { none: 1000, hit: 1000, score: 1000, 'home-run': 1000, bunt: 1000 });
});

test('再生速度は遅い・普通・速いの3択で、「普通」の2倍ずつ表示時間を変える', () => {
  assert.deepEqual(Object.keys(PLAYBACK_SPEED_MULTIPLIERS), ['slow', 'normal', 'fast']);
  assert.equal(PLAYBACK_SPEED_MULTIPLIERS.normal, 1);
  assert.equal(PLAYBACK_SPEED_MULTIPLIERS.slow / PLAYBACK_SPEED_MULTIPLIERS.normal, 2);
  assert.equal(PLAYBACK_SPEED_MULTIPLIERS.normal / PLAYBACK_SPEED_MULTIPLIERS.fast, 2);
});

test('演出の種類に応じた表示時間でコマ送りし、再生中のイニングをスコアボードで強調する', t => {
  const { stage, lineScore, sut } = setup(t);
  sut.play(toPlays(transitions));
  assert.equal(banner(stage), '単打');
  assert.equal(currentInning(lineScore), '1');
  t.mock.timers.tick(durations.hit - 1);
  assert.equal(banner(stage), '単打', '安打フレームの表示時間が過ぎるまで次へ進まない');
  t.mock.timers.tick(1);
  assert.equal(banner(stage), '本塁打');
  assert.equal(currentInning(lineScore), '2');
  t.mock.timers.tick(durations['home-run']);
  assert.equal(banner(stage), '三振');
  t.mock.timers.tick(durations.none);
  assert.equal(banner(stage), '三振', '最後のフレームを残す');
  assert.equal(currentInning(lineScore), null, '最後のフレームの表示時間が過ぎたらイニングの強調を外す');
  assert.equal(lineScore.querySelector('tbody .line-score-total').textContent, '2', '再生前から試合全体の得点を表示する');
});

test('選んだ再生速度でフレームの表示時間と演出アニメーションの速さを変える', t => {
  const { stage, sut } = setup(t);
  const animations = [{ playbackRate: 1 }];
  window.Element.prototype.getAnimations = () => animations;
  sut.setSpeed('slow');
  sut.play(toPlays(transitions));
  assert.equal(animations[0].playbackRate, 0.5, '「遅い」では演出を半分の速さにする');
  t.mock.timers.tick(durations.hit * 2 - 1);
  assert.equal(banner(stage), '単打');
  t.mock.timers.tick(1);
  assert.equal(banner(stage), '本塁打');
  sut.setSpeed('fast');
  assert.equal(animations[0].playbackRate, 2, '表示中のフレームの演出にも速度を反映する');
});

test('再生し直すと前回の再生を止める', t => {
  const { stage, sut } = setup(t);
  sut.play(toPlays(transitions));
  sut.play(toPlays(transitions.slice(2)));
  t.mock.timers.tick(durations.hit);
  assert.equal(banner(stage), '三振', '前回の再生のタイマーでフレームを差し替えない');
  sut.stop();
  sut.stop();
});

test('再生速度のボタンで速度を選び、選択中のボタンを押下状態で示す', t => {
  const { sut } = setup(t);
  document.body.insertAdjacentHTML('beforeend', ['slow', 'normal', 'fast'].map(speed => `<button class="speed-option" data-speed="${speed}" aria-pressed="${speed === 'normal'}"></button>`).join(''));
  const options = [...document.querySelectorAll('.speed-option')];
  bindPlaybackSpeedOptions(options, sut);
  options[2].click();
  assert.equal(sut.playbackSpeed, 'fast');
  assert.deepEqual(options.map(option => option.getAttribute('aria-pressed')), ['false', 'false', 'true']);
});
