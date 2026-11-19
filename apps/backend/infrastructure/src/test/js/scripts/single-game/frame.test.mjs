import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildFrame, effectHeadline, outCountMarks } from '../../../../main/typescript/single-game/frame.ts';
import { installDom } from '../../support/dom.mjs';

const play = (effect, overrides = {}) => ({
  inning: 3, outCount: 1, runnerState: '一・三塁', actionResult: '単打', cumulativeScore: 2, scoreBefore: 2,
  battingOrder: 4, direction: 'center', effect, ...overrides,
});
const classes = (frame, selector) => [...frame.querySelectorAll(selector)].map(element => element.className);

test('演出の種類ごとに見出しを変える', () => {
  assert.equal(effectHeadline({ kind: 'home-run', runs: 4, bases: 4 }), 'GRAND SLAM!');
  assert.equal(effectHeadline({ kind: 'home-run', runs: 1, bases: 4 }), 'HOME RUN!');
  assert.equal(effectHeadline({ kind: 'score', runs: 1, bases: 1 }), 'タイムリー!');
  assert.equal(effectHeadline({ kind: 'score', runs: 1, bases: 0 }), '得点!');
  assert.deepEqual([1, 2, 3].map(bases => effectHeadline({ kind: 'hit', runs: 0, bases })), ['ヒット!', 'ツーベース!', 'スリーベース!']);
  assert.equal(effectHeadline({ kind: 'bunt', runs: 0, bases: 0 }), 'バント成功!');
  assert.equal(effectHeadline({ kind: 'none', runs: 0, bases: 0 }), '');
});

test('アウトカウントを●と○の記号で表す', () => {
  assert.deepEqual([0, 1, 3].map(outCountMarks), ['○○○', '●○○', '●●●']);
});

test('フレームにイニング・アウト・得点・プレー結果・塁の状況・打者を表示する', () => {
  installDom('');
  const frame = buildFrame(play({ kind: 'hit', runs: 0, bases: 1 }));
  assert.equal(frame.className, 'frame effect-hit', '演出の種類を示すクラスを付ける');
  assert.deepEqual([...frame.querySelector('.scoreboard').children].map(span => span.textContent), ['3回', 'アウト ●○○', '得点 2']);
  assert.equal(frame.querySelector('.out-count').textContent, 'アウト ●○○', 'アウト表示に専用スタイルを適用する');
  assert.equal(frame.querySelector('.score-value').textContent, '2');
  assert.equal(frame.querySelector('.frame-banner').textContent, '単打');
  assert.deepEqual(classes(frame, '.base'), ['base base-second', 'base base-first occupied', 'base base-third occupied']);
  assert.equal(frame.querySelector('.batter-order').textContent, '4番打者');
  assert.deepEqual(classes(frame, '.ball, .runner'), ['ball ball-center', 'runner runner-1'], '安打の打球と打者走者を表示する');
  assert.deepEqual(classes(frame, '.effect-headline'), ['effect-headline headline-hit']);
  assert.equal(frame.querySelector('.effect-headline').textContent, 'ヒット!');
});

test('満塁本塁打は花火・閃光・得点表示で演出する', () => {
  installDom('');
  const frame = buildFrame(play({ kind: 'home-run', runs: 4, bases: 4 }, { runnerState: '満塁', direction: 'left' }));
  assert.deepEqual(classes(frame, '.base'), ['base base-second occupied', 'base base-first occupied', 'base base-third occupied']);
  assert.deepEqual(classes(frame, '.firework'), ['firework firework-0', 'firework firework-1', 'firework firework-2']);
  const sparks = [...frame.querySelectorAll('.firework-0 .spark')];
  assert.equal(sparks.length, 12);
  assert.deepEqual([sparks[0], sparks[3]].map(spark => spark.style.getPropertyValue('--angle')), ['0deg', '90deg'], '火花を等間隔の角度に広げる');
  assert.equal(frame.querySelector('.score-burst').textContent, '+4点');
  assert.equal(frame.querySelector('.effect-headline').textContent, 'GRAND SLAM!');
  assert.equal(frame.lastElementChild.className, 'flash');
});

test('得点・バント成功・通常のプレーはそれぞれの演出だけを表示する', () => {
  installDom('');
  const score = buildFrame(play({ kind: 'score', runs: 1, bases: 0 }));
  assert.deepEqual(classes(score, '.ball, .runner, .score-burst'), ['runner runner-home', 'score-burst']);
  const bunt = buildFrame(play({ kind: 'bunt', runs: 0, bases: 0 }, { actionResult: 'バント成功' }));
  assert.deepEqual(classes(bunt, '.ball, .runner'), ['ball ball-bunt'], 'バント成功では本塁前に転がる打球を表示する');
  assert.equal(bunt.querySelector('.effect-headline').textContent, 'バント成功!');
  const none = buildFrame(play({ kind: 'none', runs: 0, bases: 0 }, { runnerState: '不明' }));
  assert.equal(none.querySelector('.effect-headline, .flash, .ball, .runner, .score-burst'), null);
  assert.deepEqual(classes(none, '.base.occupied'), [], '未知の走者状況は走者なしで表示する');
});
