// 1試合の状況推移を、再生するプレーへ変換する。DOMに依存しない。

/** 1試合実行APIが返す状況推移。アウト・走者・得点はプレー直前の状況で記録される。 */
export interface GameTransition {
  inning: number;
  outCount: number;
  runnerState: string;
  actionResult: string;
  cumulativeScore: number;
}

/** 1試合実行APIの応答。 */
export interface SingleGameResponse {
  transitions: GameTransition[] | null;
}

/** プレーの演出の種類。 */
export type EffectKind = 'none' | 'hit' | 'score' | 'home-run' | 'bunt';

/** プレーの演出。runs はそのプレーの得点、bases は安打の打者走者の進塁数（安打以外は0）。 */
export interface PlayEffect {
  kind: EffectKind;
  runs: number;
  bases: number;
}

/** プレー直後の状況に直した推移。scoreBefore はプレー直前の得点。 */
export interface PlayOutcome extends GameTransition {
  scoreBefore: number;
}

/** 再生する1プレー。 */
export interface Play extends PlayOutcome {
  battingOrder: number;
  effect: PlayEffect;
  /** 打球の方向。プレーの順に左・中・右を繰り返し、同じ試合なら同じ表示にする。 */
  direction: BallDirection;
}

export type BallDirection = 'left' | 'center' | 'right';

/** 打順の人数。 */
export const BATTING_ORDER_SIZE = 9;

/** 1イニングのアウト数。 */
export const OUTS_PER_INNING = 3;

const HIT_BASES: Readonly<Record<string, number>> = { 単打: 1, 二塁打: 2, 三塁打: 3, 本塁打: 4 };
const HOME_RUN_BASES = 4;
const BALL_DIRECTIONS: readonly BallDirection[] = ['left', 'center', 'right'];

/** 盗塁だけの推移かを返す。盗塁は打席結果ではないため打順を進めない。 */
export function isStealOnly(transition: Pick<GameTransition, 'actionResult'>): boolean {
  return transition.actionResult.startsWith('盗塁');
}

/**
 * 推移のアウト・走者・得点はプレー直前の状況で記録されるため、次の推移からプレー直後の状況を求める。
 * イニングや試合の最後のプレーは3アウトにし、走者はそのまま残す。
 */
export function resolvePlayOutcomes(transitions: readonly GameTransition[]): PlayOutcome[] {
  return transitions.map((transition, index) => {
    const next = transitions[index + 1];
    const sameInning = next !== undefined && next.inning === transition.inning;
    return {
      ...transition,
      scoreBefore: transition.cumulativeScore,
      cumulativeScore: next === undefined ? transition.cumulativeScore : next.cumulativeScore,
      outCount: sameInning ? next.outCount : OUTS_PER_INNING,
      runnerState: sameInning ? next.runnerState : transition.runnerState,
    };
  });
}

/** 推移に打順を付ける。1番から始めて打席ごとに進め、9番の次は1番に戻す。 */
export function annotateBattingOrder<T extends Pick<GameTransition, 'actionResult'>>(transitions: readonly T[]): (T & { battingOrder: number })[] {
  let battingOrder = 1;
  return transitions.map(transition => {
    const annotated = { ...transition, battingOrder };
    if (!isStealOnly(transition)) {
      battingOrder = battingOrder === BATTING_ORDER_SIZE ? 1 : battingOrder + 1;
    }
    return annotated;
  });
}

/**
 * プレーの演出を決める。本塁打を最優先し、次に得点（安打以外の得点も含む）、得点のない安打、バント成功の順に選ぶ。
 */
export function classifyEffect(transition: Pick<GameTransition, 'actionResult' | 'cumulativeScore'>, previousScore: number): PlayEffect {
  const runs = transition.cumulativeScore - previousScore;
  const bases = HIT_BASES[transition.actionResult] ?? 0;
  if (bases === HOME_RUN_BASES) return { kind: 'home-run', runs, bases };
  if (runs > 0) return { kind: 'score', runs, bases };
  if (bases > 0) return { kind: 'hit', runs: 0, bases };
  if (transition.actionResult === 'バント成功') return { kind: 'bunt', runs: 0, bases: 0 };
  return { kind: 'none', runs: 0, bases: 0 };
}

/** 状況推移から、プレー直後の状況・打順・演出を付けた再生用のプレーを作る。 */
export function toPlays(transitions: readonly GameTransition[]): Play[] {
  return annotateBattingOrder(resolvePlayOutcomes(transitions)).map((play, index) => ({
    ...play,
    effect: classifyEffect(play, play.scoreBefore),
    direction: BALL_DIRECTIONS[index % BALL_DIRECTIONS.length],
  }));
}
