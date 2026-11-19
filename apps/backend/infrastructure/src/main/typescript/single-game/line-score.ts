// 野球のスコアボード（イニング別得点とR・H・E）。
import { createElement } from '../shared/dom.ts';
import type { Play } from './plays.ts';

/** スコアボードの集計。innings はイニングごとの得点。 */
export interface LineScoreSummary {
  innings: Map<number, number>;
  runs: number;
  hits: number;
  errors: number;
}

/** スコアボードに最低限用意するイニング数。 */
export const REGULATION_INNINGS = 9;

/** プレーからイニング別得点・得点(R)・安打数(H)・失策数(E)を集計する。シミュレーターは失策を扱わないため、失策数は常に0。 */
export function summarizeLineScore(plays: readonly Pick<Play, 'inning' | 'effect'>[]): LineScoreSummary {
  const innings = new Map<number, number>();
  let runs = 0;
  let hits = 0;
  for (const play of plays) {
    innings.set(play.inning, (innings.get(play.inning) ?? 0) + play.effect.runs);
    runs += play.effect.runs;
    if (play.effect.bases > 0) hits += 1;
  }
  return { innings, runs, hits, errors: 0 };
}

/** スコアボードの列数。延長戦なら最終イニングまで広げる。 */
export function lineScoreInningCount(plays: readonly Pick<Play, 'inning'>[]): number {
  return Math.max(REGULATION_INNINGS, plays[plays.length - 1].inning);
}

/** スコアボードの表を作る。currentInning のイニングは見出しと得点を強調し、null ならどれも強調しない。 */
export function buildLineScore(summary: LineScoreSummary, inningCount: number, currentInning: number | null): HTMLTableElement {
  const table = createElement('table', 'line-score');
  table.setAttribute('aria-label', 'スコアボード');
  const headRow = createElement('tr');
  const bodyRow = createElement('tr');
  const team = createElement('th', 'line-score-team', '自チーム');
  team.scope = 'row';
  headRow.append(createElement('th', 'line-score-team'));
  bodyRow.append(team);
  for (let inning = 1; inning <= inningCount; inning += 1) {
    const className = inning === currentInning ? 'is-current' : '';
    const runs = summary.innings.get(inning);
    const heading = createElement('th', className, String(inning));
    heading.scope = 'col';
    headRow.append(heading);
    bodyRow.append(createElement('td', className, runs === undefined ? '' : String(runs)));
  }
  const totals: [string, number][] = [['R', summary.runs], ['H', summary.hits], ['E', summary.errors]];
  for (const [label, value] of totals) {
    const heading = createElement('th', 'line-score-total', label);
    heading.scope = 'col';
    headRow.append(heading);
    bodyRow.append(createElement('td', 'line-score-total', String(value)));
  }
  const head = createElement('thead');
  const body = createElement('tbody');
  head.append(headRow);
  body.append(bodyRow);
  table.append(head, body);
  return table;
}

/** スコアボードは再生前から試合全体の結果を表示し、再生中のイニングだけを強調する。 */
export function renderLineScore(container: HTMLElement, plays: readonly Play[], currentInning: number | null): void {
  container.replaceChildren(buildLineScore(summarizeLineScore(plays), lineScoreInningCount(plays), currentInning));
}
