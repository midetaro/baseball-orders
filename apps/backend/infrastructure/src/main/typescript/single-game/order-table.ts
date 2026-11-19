// イニング×打順の打席結果表。
import { createElement } from '../shared/dom.ts';
import { BATTING_ORDER_SIZE, isStealOnly, type Play } from './plays.ts';

/** 打順を行、打席のあったイニングを列にした表を作る。盗塁だけの推移は打席結果に含めない。 */
export function buildOrderTable(plays: readonly Play[]): HTMLTableElement {
  const plateAppearances = plays.filter(play => !isStealOnly(play));
  const innings = [...new Set(plateAppearances.map(play => play.inning))].sort((left, right) => left - right);
  const results = new Map(plateAppearances.map(play => [`${play.battingOrder}-${play.inning}`, play]));
  const headRow = createElement('tr');
  headRow.append(createElement('th', '', '打順'));
  for (const inning of innings) {
    const heading = createElement('th', '', `${inning}回`);
    heading.scope = 'col';
    headRow.append(heading);
  }
  const body = createElement('tbody');
  for (let battingOrder = 1; battingOrder <= BATTING_ORDER_SIZE; battingOrder += 1) {
    const row = createElement('tr');
    const label = createElement('th', '', `${battingOrder}番`);
    label.scope = 'row';
    row.append(label);
    for (const inning of innings) {
      const cell = createElement('td');
      const result = results.get(`${battingOrder}-${inning}`);
      if (result) {
        cell.textContent = result.actionResult;
        cell.className = `cell-${result.effect.kind}`;
      }
      row.append(cell);
    }
    body.append(row);
  }
  const head = createElement('thead');
  head.append(headRow);
  const table = createElement('table', 'order-table');
  table.append(head, body);
  return table;
}
