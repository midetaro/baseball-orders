import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildOrderTable } from '../../../../main/typescript/single-game/order-table.ts';
import { toPlays } from '../../../../main/typescript/single-game/plays.ts';
import { installDom } from '../../support/dom.mjs';

test('打順を固定の9行、打席のあったイニングを列にして打席結果を表示する。盗塁は打席結果に含めない', () => {
  installDom('');
  const transition = (inning, actionResult, cumulativeScore = 0) => ({ inning, actionResult, outCount: 0, cumulativeScore, runnerState: '走者なし' });
  const table = buildOrderTable(toPlays([
    transition(1, '本塁打'),
    transition(1, '盗塁成功(二塁)', 1),
    transition(1, '三振', 1),
    transition(3, '単打', 1),
  ]));
  assert.equal(table.className, 'order-table');
  assert.deepEqual([...table.querySelectorAll('thead th')].map(cell => cell.textContent), ['打順', '1回', '3回']);
  const rows = [...table.querySelectorAll('tbody tr')];
  assert.deepEqual(rows.map(row => row.querySelector('th').textContent), ['1番', '2番', '3番', '4番', '5番', '6番', '7番', '8番', '9番']);
  assert.deepEqual(rows.slice(0, 4).map(row => [...row.querySelectorAll('td')].map(cell => `${cell.textContent}:${cell.className}`)), [
    ['本塁打:cell-home-run', ':'],
    ['三振:cell-none', ':'],
    [':', '単打:cell-hit'],
    [':', ':'],
  ]);
});
