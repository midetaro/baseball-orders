// 1試合実行画面を組み立てる。
import { LineupForm, findLineupFormElements } from '../lineup/lineup-form.ts';
import { emptyState, requireElement } from '../shared/dom.ts';
import { buildOrderTable } from './order-table.ts';
import { FramePlayer, bindPlaybackSpeedOptions, readFrameDurations } from './playback.ts';
import { toPlays, type GameTransition, type SingleGameResponse } from './plays.ts';

const NO_RESULT_MESSAGE = '試合結果はありません。';

/** 1試合の結果パネル。フレーム再生・スコアボード・打席結果表を描画する。 */
export class SingleGameView {
  private readonly frameStage: HTMLElement;
  private readonly lineScore: HTMLElement;
  private readonly orderTableScroll: HTMLElement;
  private readonly player: FramePlayer;

  constructor(root: ParentNode = document) {
    this.frameStage = requireElement('#frame-stage', root);
    this.lineScore = requireElement('#line-score', root);
    this.orderTableScroll = requireElement('#order-table-scroll', root);
    this.player = new FramePlayer(this.frameStage, this.lineScore, readFrameDurations(this.frameStage));
    bindPlaybackSpeedOptions([...root.querySelectorAll<HTMLButtonElement>('.speed-option')], this.player);
  }

  /** 前回の再生を止めて、推移全体からフレーム再生と打席結果表を組み立てる。推移が無ければ結果なしと表示する。 */
  render(transitions: readonly GameTransition[] | null | undefined): void {
    this.player.stop();
    if (!Array.isArray(transitions) || transitions.length === 0) {
      this.frameStage.replaceChildren(emptyState(NO_RESULT_MESSAGE));
      this.lineScore.replaceChildren(emptyState(NO_RESULT_MESSAGE));
      this.orderTableScroll.replaceChildren(emptyState(NO_RESULT_MESSAGE));
      return;
    }
    const plays = toPlays(transitions);
    this.player.play(plays);
    this.orderTableScroll.replaceChildren(buildOrderTable(plays));
  }
}

/** 打順を1試合実行APIへ送り、試合の推移を再生する画面を開始する。 */
export function startSingleGamePage(root: ParentNode = document): void {
  const view = new SingleGameView(root);
  const resultFeedback = requireElement('#result-feedback', root);
  new LineupForm<SingleGameResponse>(findLineupFormElements(root), {
    readyMessage: '準備完了。1試合を実行できます。',
    runningMessage: '試合を実行中…',
    endpoint: '/simulations/single-game',
    onSuccess: data => {
      resultFeedback.className = 'hint success';
      resultFeedback.textContent = '試合が終了しました。';
      view.render(data.transitions);
    },
  }).start();
}
