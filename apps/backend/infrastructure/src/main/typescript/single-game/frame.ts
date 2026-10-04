// 1プレーを表すアニメーションフレーム。演出は乱数を使わず、同じプレーなら同じフレームにする。
import { createElement } from '../shared/dom.ts';
import { OUTS_PER_INNING, type Play, type PlayEffect } from './plays.ts';

/** 走者の状況ごとの塁の埋まり方。 */
interface RunnerLayout {
  first: boolean;
  second: boolean;
  third: boolean;
}

const EMPTY_BASES: RunnerLayout = { first: false, second: false, third: false };

/** 1試合実行APIの走者の状況（runnerState）ごとの塁の埋まり方。 */
export const RUNNER_LAYOUT: Readonly<Record<string, RunnerLayout>> = {
  走者なし: EMPTY_BASES,
  一塁: { first: true, second: false, third: false },
  二塁: { first: false, second: true, third: false },
  '一・二塁': { first: true, second: true, third: false },
  三塁: { first: false, second: false, third: true },
  '一・三塁': { first: true, second: false, third: true },
  '二・三塁': { first: false, second: true, third: true },
  満塁: { first: true, second: true, third: true },
};

const HIT_HEADLINES: Readonly<Record<number, string>> = { 1: 'ヒット!', 2: 'ツーベース!', 3: 'スリーベース!' };
const GRAND_SLAM_RUNS = 4;
const FIREWORK_BURSTS = 3;
const FIREWORK_SPARKS = 12;

/** 演出の見出しを返す。見出しを出さない演出は空文字を返す。 */
export function effectHeadline(effect: PlayEffect): string {
  switch (effect.kind) {
    case 'home-run':
      return effect.runs === GRAND_SLAM_RUNS ? 'GRAND SLAM!' : 'HOME RUN!';
    case 'score':
      return effect.bases > 0 ? 'タイムリー!' : '得点!';
    case 'hit':
      return HIT_HEADLINES[effect.bases];
    case 'bunt':
      return 'バント成功!';
    case 'none':
      return '';
  }
}

/** アウトカウントを、アウトを●・残りを○で表す。 */
export function outCountMarks(outCount: number): string {
  return '●'.repeat(outCount) + '○'.repeat(OUTS_PER_INNING - outCount);
}

/** 1プレーのフレームを作る。 */
export function buildFrame(play: Play): HTMLDivElement {
  const { effect } = play;
  const frame = createElement('div', `frame effect-${effect.kind}`);
  frame.append(buildScoreboard(play), createElement('p', 'frame-banner', play.actionResult), buildDiamond(play));
  const headline = effectHeadline(effect);
  if (headline) frame.append(createElement('p', `effect-headline headline-${effect.kind}`, headline));
  if (effect.kind === 'home-run') frame.append(createElement('span', 'flash'));
  return frame;
}

/** イニング・アウト・得点の表示。 */
function buildScoreboard(play: Play): HTMLDivElement {
  const scoreboard = createElement('div', 'frame-meta scoreboard');
  const score = createElement('span', '', '得点 ');
  score.append(createElement('strong', 'score-value', String(play.cumulativeScore)));
  scoreboard.append(createElement('span', '', `${play.inning}回`), createElement('span', 'out-count', `アウト ${outCountMarks(play.outCount)}`), score);
  return scoreboard;
}

/** 塁の状況と、演出に応じた打球・走者・花火・得点表示を載せたダイヤモンド。 */
function buildDiamond(play: Play): HTMLDivElement {
  const { effect } = play;
  const layout = RUNNER_LAYOUT[play.runnerState] ?? EMPTY_BASES;
  const diamond = createElement('div', 'diamond ballpark');
  diamond.append(createElement('span', 'infield'), createElement('span', 'home-plate'));
  for (const base of ['second', 'first', 'third'] as const) {
    diamond.append(createElement('span', `base base-${base}${layout[base] ? ' occupied' : ''}`));
  }
  diamond.append(createElement('span', 'batter-order', `${play.battingOrder}番打者`));
  if (effect.bases > 0) diamond.append(createElement('span', `ball ball-${play.direction}`), createElement('span', `runner runner-${effect.bases}`));
  if (effect.kind === 'bunt') diamond.append(createElement('span', 'ball ball-bunt'));
  if (effect.kind === 'score') diamond.append(createElement('span', 'runner runner-home'));
  if (effect.kind === 'home-run') diamond.append(...buildFireworks());
  if (effect.runs > 0) diamond.append(createElement('span', 'score-burst', `+${effect.runs}点`));
  return diamond;
}

/** 本塁打の花火。火花を等間隔の角度に広げる。 */
function buildFireworks(): HTMLSpanElement[] {
  return Array.from({ length: FIREWORK_BURSTS }, (_, burstIndex) => {
    const burst = createElement('span', `firework firework-${burstIndex}`);
    for (let sparkIndex = 0; sparkIndex < FIREWORK_SPARKS; sparkIndex += 1) {
      const spark = createElement('span', 'spark');
      spark.style.setProperty('--angle', `${(sparkIndex * 360) / FIREWORK_SPARKS}deg`);
      burst.append(spark);
    }
    return burst;
  });
}
