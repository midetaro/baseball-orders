// 大規模実行・1試合実行・打順組み替え画面で共通の打順入力フォーム。
// 打順の状態を持ち、入力画面と結果画面の切り替えと、打順のAPI送信を担う。
import { optionalElement, requireElement } from '../shared/dom.ts';
import {
  fieldWrapper,
  forcedLabel,
  hitAverageInput,
  memoInput,
  personalitySelect,
  statLabel,
  toggleButton,
} from './controls.ts';
import { DragReorder } from './drag-reorder.ts';
import {
  HIT_AVERAGE_RANGE_MESSAGE,
  PERSONALITY_LABELS,
  averageHitAverage,
  createInitialLineup,
  formatHitAverage,
  isValidLineup,
  movePlayer,
  parseDefaultBatters,
  toLineupRequest,
  withLeadingZero,
  type LineupPlayer,
} from './player.ts';

/** 打順入力フォームが操作するテンプレートの要素。画面によって無い要素は null になる。 */
export interface LineupFormElements {
  order: HTMLElement;
  submit: HTMLButtonElement;
  feedback: HTMLElement;
  toggleAllBunt: HTMLButtonElement;
  toggleAllSteal: HTMLButtonElement;
  resetAllPersonalities: HTMLButtonElement | null;
  teamSelect: HTMLSelectElement | null;
  teamDefaults: HTMLElement | null;
  averageDisplay: HTMLElement | null;
  inputView: HTMLElement;
  resultsView: HTMLElement;
  tabInput: HTMLButtonElement;
  tabResults: HTMLButtonElement;
  editLineup: HTMLButtonElement;
}

/** 画面ごとの文言と送信先、成功時の結果描画。 */
export interface LineupFormConfig<T> {
  /** 入力が揃ったときの案内。 */
  readyMessage: string;
  /** 実行中の案内。 */
  runningMessage: string;
  /** 打順をPOSTするAPI。 */
  endpoint: string;
  /** 成功時の応答JSONを結果画面へ描画する。 */
  onSuccess(data: T): void;
}

/** 応答がエラーのときにAPIが返すJSON。 */
interface ErrorResponse {
  error?: string;
  message?: string;
}

const REQUEST_FAILED_MESSAGE = 'リクエストに失敗しました。';

/** テンプレートから打順入力フォームの要素を集める。 */
export function findLineupFormElements(root: ParentNode = document): LineupFormElements {
  return {
    order: requireElement('#order', root),
    submit: requireElement('#submit', root),
    feedback: requireElement('#feedback', root),
    toggleAllBunt: requireElement('#toggle-all-bunt', root),
    toggleAllSteal: requireElement('#toggle-all-steal', root),
    resetAllPersonalities: optionalElement('#reset-all-personalities', root),
    teamSelect: optionalElement('#team-select', root),
    teamDefaults: optionalElement('#team-defaults', root),
    averageDisplay: optionalElement('#average-hit-average', root),
    inputView: requireElement('#input-view', root),
    resultsView: requireElement('#results', root),
    tabInput: requireElement('#tab-input', root),
    tabResults: requireElement('#tab-results', root),
    editLineup: requireElement('#edit-lineup', root),
  };
}

/**
 * 打順入力フォーム。
 * 打順組み替え画面（#order に data-lineup-mode="reorder"）では、打率と性格を編集させずに表示し、ドラッグで打順を入れ替える。
 */
export class LineupForm<T> {
  private readonly elements: LineupFormElements;
  private readonly config: LineupFormConfig<T>;
  private readonly lineup: LineupPlayer[] = createInitialLineup();
  private readonly reorderMode: boolean;
  private readonly dragReorder: DragReorder;
  private inFlight = false;
  private hasResults = false;

  constructor(elements: LineupFormElements, config: LineupFormConfig<T>) {
    this.elements = elements;
    this.config = config;
    this.reorderMode = elements.order.dataset.lineupMode === 'reorder';
    this.dragReorder = new DragReorder({
      container: elements.order,
      isLocked: () => this.inFlight,
      move: (from, to) => this.move(from, to),
    });
    if (this.reorderMode && elements.teamSelect) {
      this.applyTeam(elements.teamSelect.value);
    }
  }

  /** 操作のイベントを登録し、初期の打順を描画する。 */
  start(): void {
    const { tabInput, tabResults, editLineup, toggleAllBunt, toggleAllSteal, teamSelect, resetAllPersonalities, submit } = this.elements;
    tabInput.addEventListener('click', () => this.showView(false));
    tabResults.addEventListener('click', () => this.showView(true));
    editLineup.addEventListener('click', () => this.showView(false));
    toggleAllBunt.addEventListener('click', () => this.toggleAll('buntForced', 'buntEnabled'));
    toggleAllSteal.addEventListener('click', () => this.toggleAll('stealForced', 'stealEnabled'));
    teamSelect?.addEventListener('change', () => {
      this.applyTeam(teamSelect.value);
      this.render();
    });
    resetAllPersonalities?.addEventListener('click', () => {
      this.lineup.forEach(player => {
        player.personality = 'DEFAULT';
      });
      this.render();
    });
    submit.addEventListener('click', () => this.submit());
    this.render();
  }

  private move(from: number, to: number): boolean {
    if (!movePlayer(this.lineup, from, to)) {
      return false;
    }
    this.render();
    return true;
  }

  /** 固定されていない打者のバントまたは盗塁を、全員有効なら全員無効に、そうでなければ全員有効にする。 */
  private toggleAll(forcedKey: 'buntForced' | 'stealForced', enabledKey: 'buntEnabled' | 'stealEnabled'): void {
    const targets = this.lineup.filter(player => !player[forcedKey]);
    const enabled = !targets.every(player => player[enabledKey]);
    targets.forEach(player => {
      player[enabledKey] = enabled;
    });
    this.render();
  }

  private applyTeam(key: string): void {
    const team = this.elements.teamDefaults?.querySelector(`[data-team="${key}"]`);
    if (!team) return;
    this.lineup.splice(0, this.lineup.length, ...parseDefaultBatters(team.querySelectorAll<HTMLElement>('[data-batter]')));
  }

  private async submit(): Promise<void> {
    if (this.inFlight || !isValidLineup(this.lineup)) return;
    const { feedback } = this.elements;
    this.inFlight = true;
    this.render();
    feedback.className = '';
    feedback.textContent = this.config.runningMessage;
    try {
      const response = await fetch(this.config.endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(toLineupRequest(this.lineup)),
      });
      const data: unknown = await response.json();
      if (!response.ok) {
        const error = data as ErrorResponse;
        throw new Error(error.error || error.message || REQUEST_FAILED_MESSAGE);
      }
      this.config.onSuccess(data as T);
      this.hasResults = true;
      this.showView(true);
    } catch (error) {
      feedback.className = 'error';
      feedback.textContent = error instanceof Error ? error.message : String(error);
    } finally {
      this.inFlight = false;
      this.render();
    }
  }

  private render(): void {
    const { order } = this.elements;
    order.replaceChildren(...this.lineup.map((player, index) => this.renderRow(player, index)));
    this.update();
  }

  private renderRow(player: LineupPlayer, index: number): HTMLDivElement {
    const row = document.createElement('div');
    row.className = 'slot';
    row.dataset.index = String(index);
    const position = document.createElement('span');
    position.className = 'slot-index';
    position.textContent = `${index + 1}番`;
    row.append(position);
    if (this.reorderMode) {
      row.append(this.dragReorder.handle(row, index));
    }
    row.append(
      fieldWrapper('打率', this.hitAverageControl(player)),
      fieldWrapper('性格', this.personalityControl(player), 'field-personality'),
      this.tacticControl('バント', player, 'buntForced', 'buntEnabled'),
      this.tacticControl('盗塁', player, 'stealForced', 'stealEnabled'),
    );
    if (this.reorderMode) {
      row.append(fieldWrapper('メモ', memoInput(player.memo || '', index + 1, this.inFlight, value => {
        player.memo = value;
      })));
    }
    return row;
  }

  private hitAverageControl(player: LineupPlayer): HTMLElement {
    if (this.reorderMode) {
      return statLabel(player.hitAverage);
    }
    const setHitAverage = (value: string): string => {
      player.hitAverage = value;
      this.update();
      return value;
    };
    return hitAverageInput(player.hitAverage, this.inFlight, {
      onInput: value => setHitAverage(withLeadingZero(value)),
      onChange: value => setHitAverage(formatHitAverage(value)),
    });
  }

  private personalityControl(player: LineupPlayer): HTMLElement {
    if (this.reorderMode) {
      return statLabel(PERSONALITY_LABELS[player.personality]);
    }
    return personalitySelect(player.personality, this.inFlight, personality => {
      player.personality = personality;
    });
  }

  private tacticControl(
    label: string,
    player: LineupPlayer,
    forcedKey: 'buntForced' | 'stealForced',
    enabledKey: 'buntEnabled' | 'stealEnabled',
  ): HTMLElement {
    if (player[forcedKey]) {
      return forcedLabel(label);
    }
    return toggleButton(label, player[enabledKey], this.inFlight, () => {
      player[enabledKey] = !player[enabledKey];
      this.render();
    });
  }

  /** 入力の有効性と実行中かどうかに合わせて、操作の有効・無効と案内を更新する。 */
  private update(): void {
    const { submit, feedback, toggleAllBunt, toggleAllSteal, resetAllPersonalities, teamSelect, averageDisplay, tabInput, tabResults, editLineup } = this.elements;
    const complete = isValidLineup(this.lineup);
    const allEnabled = (forcedKey: 'buntForced' | 'stealForced', enabledKey: 'buntEnabled' | 'stealEnabled'): boolean =>
      this.lineup.filter(player => !player[forcedKey]).every(player => player[enabledKey]);
    if (averageDisplay) averageDisplay.textContent = averageHitAverage(this.lineup);
    if (teamSelect) teamSelect.disabled = this.inFlight;
    submit.disabled = this.inFlight || !complete;
    toggleAllBunt.disabled = this.inFlight;
    toggleAllBunt.setAttribute('aria-pressed', String(allEnabled('buntForced', 'buntEnabled')));
    toggleAllSteal.disabled = this.inFlight;
    toggleAllSteal.setAttribute('aria-pressed', String(allEnabled('stealForced', 'stealEnabled')));
    if (resetAllPersonalities) resetAllPersonalities.disabled = this.inFlight;
    tabInput.disabled = this.inFlight;
    tabResults.disabled = this.inFlight || !this.hasResults;
    editLineup.disabled = this.inFlight;
    if (!this.inFlight) {
      feedback.className = '';
      feedback.textContent = complete ? this.config.readyMessage : HIT_AVERAGE_RANGE_MESSAGE;
    }
  }

  /** 入力画面と結果画面のどちらか一方だけを表示する。 */
  private showView(resultsVisible: boolean): void {
    const { inputView, resultsView, tabInput, tabResults } = this.elements;
    inputView.hidden = resultsVisible;
    resultsView.hidden = !resultsVisible;
    tabInput.setAttribute('aria-selected', String(!resultsVisible));
    tabResults.setAttribute('aria-selected', String(resultsVisible));
    this.update();
    window.scrollTo({ top: 0 });
  }
}
