// 大規模実行の結果画面。集計値・得点分布・内訳を描画する。
import { createElement, requireElement } from '../shared/dom.ts';
import {
  SUMMARY_KEYS,
  breakdownGroups,
  scoreHistogram,
  summarizeBreakdown,
  summaryElementId,
  type BreakdownGroup,
  type SimulationStatistics,
} from './statistics.ts';

/** 大規模実行の結果を、テンプレートの結果パネルへ描画する。 */
export class SimulationResultsView {
  private readonly root: ParentNode;
  private readonly resultFeedback: HTMLElement;

  constructor(root: ParentNode = document) {
    this.root = root;
    this.resultFeedback = requireElement('#result-feedback', root);
  }

  /** 集計結果で結果パネル全体を描き直す。 */
  render(statistics: SimulationStatistics): void {
    for (const key of SUMMARY_KEYS) {
      this.element(`#${summaryElementId(key)}`).textContent = String(statistics[key] ?? 0);
    }
    this.element('#maximum-score-card').textContent = String(statistics.maximumScore ?? 0);
    this.renderHistogram(statistics);
    for (const group of breakdownGroups(statistics)) {
      this.renderBreakdown(group);
    }
    const gameCount = Number(statistics.gameCount) || 0;
    this.element('#score-empty-state').hidden = gameCount !== 0;
    this.resultFeedback.className = 'hint success';
    this.resultFeedback.textContent = gameCount === 0 ? '試合結果はありません。' : `試合終了：${gameCount}試合`;
  }

  private renderHistogram(statistics: SimulationStatistics): void {
    const histogram = scoreHistogram(statistics);
    const bars = this.element('#score-histogram-bars');
    bars.style.setProperty('--histogram-grid-step', `${histogram.gridStep}%`);
    this.element('#score-distribution-axis').replaceChildren(...histogram.axisLabels.map(rate => createElement('span', '', `${rate}%`)));
    bars.replaceChildren(
      ...histogram.bars.map(({ score, rate, height }) => {
        const bar = createElement('div', 'histogram-bar');
        bar.setAttribute('aria-label', `${score}点: ${rate.toFixed(1)}%`);
        const column = createElement('div');
        column.style.height = `${height}%`;
        bar.append(column, createElement('span', '', `${score}点`));
        return bar;
      }),
    );
  }

  private renderBreakdown({ prefix, items }: BreakdownGroup): void {
    const { total, segments } = summarizeBreakdown(items);
    this.element(`#${prefix}-breakdown`).replaceChildren(
      ...segments.map(({ label, count, kind, rate }) => {
        const segment = createElement('div', kind);
        segment.style.width = `${rate}%`;
        segment.setAttribute('aria-label', `${label}: ${count} (${rate.toFixed(1)}%)`);
        return segment;
      }),
    );
    this.element(`#${prefix}-legend`).replaceChildren(
      ...segments.map(({ label, count, kind, rate }) => {
        const item = createElement('li');
        item.append(createElement('span', `legend-swatch ${kind}`), createElement('span', '', `${label} ${count} (${rate.toFixed(1)}%)`));
        return item;
      }),
    );
    this.element(`#${prefix}-empty-state`).hidden = total !== 0;
  }

  private element(selector: string): HTMLElement {
    return requireElement(selector, this.root);
  }
}
