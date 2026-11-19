// プレーのフレームをコマ送りで自動再生する。
import { buildFrame } from './frame.ts';
import { renderLineScore } from './line-score.ts';
import type { EffectKind, Play } from './plays.ts';

/** 再生速度。 */
export type PlaybackSpeed = 'slow' | 'normal' | 'fast';

/** 再生速度は「普通」を基準に2倍ずつ変える。値はフレーム表示時間に掛ける倍率。 */
export const PLAYBACK_SPEED_MULTIPLIERS = { slow: 2, normal: 1, fast: 0.5 } as const satisfies Record<PlaybackSpeed, number>;

/** 演出の種類ごとのフレーム表示時間（ミリ秒）。 */
export type FrameDurations = Record<EffectKind, number>;

/** テンプレートにフレーム表示時間が無いときの表示時間（ミリ秒）。 */
const FALLBACK_FRAME_DURATION_MILLIS = 1000;

/**
 * サーバー設定のフレーム表示時間を、フレーム表示領域のdata属性から読み取る。
 * 演出ごとの時間が無ければ通常の表示時間を使う。
 */
export function readFrameDurations(stage: HTMLElement): FrameDurations {
  const data = stage.dataset;
  const none = Number(data.frameDurationMillis) || FALLBACK_FRAME_DURATION_MILLIS;
  return {
    none,
    hit: Number(data.hitFrameDurationMillis) || none,
    score: Number(data.scoreFrameDurationMillis) || none,
    'home-run': Number(data.homeRunFrameDurationMillis) || none,
    bunt: Number(data.buntFrameDurationMillis) || none,
  };
}

/** プレーを1つずつフレームにして再生し、スコアボードの強調を再生中のイニングに合わせる。 */
export class FramePlayer {
  private readonly stage: HTMLElement;
  private readonly lineScore: HTMLElement;
  private readonly durations: FrameDurations;
  private speed: PlaybackSpeed = 'normal';
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor(stage: HTMLElement, lineScore: HTMLElement, durations: FrameDurations) {
    this.stage = stage;
    this.lineScore = lineScore;
    this.durations = durations;
  }

  /** 現在の再生速度。 */
  get playbackSpeed(): PlaybackSpeed {
    return this.speed;
  }

  /** 再生速度を変え、表示中のフレームの演出アニメーションにも反映する。 */
  setSpeed(speed: PlaybackSpeed): void {
    this.speed = speed;
    const frame = this.stage.querySelector('.frame');
    if (frame) this.applyPlaybackRate(frame);
  }

  /** 前回の再生を止めて、最初のプレーから再生する。最後のフレームの表示時間が過ぎたらイニングの強調を外す。 */
  play(plays: readonly Play[]): void {
    this.stop();
    let index = 0;
    const show = (): void => {
      const play = plays[index];
      const frame = buildFrame(play);
      this.stage.replaceChildren(frame);
      this.applyPlaybackRate(frame);
      renderLineScore(this.lineScore, plays, play.inning);
      index += 1;
      const delay = this.durations[play.effect.kind] * PLAYBACK_SPEED_MULTIPLIERS[this.speed];
      if (index >= plays.length) {
        this.timer = setTimeout(() => {
          this.timer = null;
          renderLineScore(this.lineScore, plays, null);
        }, delay);
        return;
      }
      this.timer = setTimeout(show, delay);
    };
    show();
  }

  /** 再生中なら止める。 */
  stop(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private applyPlaybackRate(frame: Element): void {
    for (const animation of frame.getAnimations({ subtree: true })) {
      animation.playbackRate = 1 / PLAYBACK_SPEED_MULTIPLIERS[this.speed];
    }
  }
}

/** 再生速度のボタンで再生速度を切り替え、選択中のボタンを押下状態で示す。 */
export function bindPlaybackSpeedOptions(options: readonly HTMLButtonElement[], player: FramePlayer): void {
  for (const option of options) {
    option.addEventListener('click', () => {
      player.setSpeed(option.dataset.speed as PlaybackSpeed);
      for (const each of options) {
        each.setAttribute('aria-pressed', String(each.dataset.speed === player.playbackSpeed));
      }
    });
  }
}
