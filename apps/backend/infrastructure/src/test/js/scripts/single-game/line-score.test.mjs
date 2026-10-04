import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildLineScore, lineScoreInningCount, renderLineScore, summarizeLineScore } from '../../../../main/typescript/single-game/line-score.ts';
import { installDom } from '../../support/dom.mjs';

const played = (inning, runs, bases) => ({ inning, effect: { runs, bases } });
const texts = cells => [...cells].map(cell => cell.textContent);

test('プレーからイニング別得点・合計得点(R)・安打数(H)・失策数(E)を集計する（issue #149）', () => {
  assert.deepEqual(
    summarizeLineScore([played(1, 0, 1), played(1, 2, 4), played(1, 0, 0), played(2, 1, 0), played(2, 0, 2), played(3, 0, 0)]),
    { innings: new Map([[1, 2], [2, 1], [3, 0]]), runs: 3, hits: 3, errors: 0 },
  );
  assert.deepEqual(summarizeLineScore([]), { innings: new Map(), runs: 0, hits: 0, errors: 0 }, 'プレー前はすべて0にする');
});

test('スコアボードは最低9回まで列を用意し、延長戦なら最終イニングまで広げる', () => {
  assert.equal(lineScoreInningCount([played(1, 0, 0), played(7, 0, 0)]), 9);
  assert.equal(lineScoreInningCount([played(1, 0, 0), played(11, 0, 0)]), 11);
});

test('スコアボードにイニング別得点とR・H・Eを表示し、再生中のイニングを見出しごと強調する', () => {
  installDom('');
  const table = buildLineScore({ innings: new Map([[1, 2], [2, 0]]), runs: 2, hits: 3, errors: 0 }, 9, 2);
  assert.equal(table.getAttribute('aria-label'), 'スコアボード');
  assert.deepEqual(texts(table.querySelectorAll('thead th')), ['', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'R', 'H', 'E']);
  assert.deepEqual(texts(table.querySelectorAll('tbody th, tbody td')), ['自チーム', '2', '0', '', '', '', '', '', '', '', '2', '3', '0'], 'まだ無いイニングは空欄にする');
  assert.deepEqual(texts(table.querySelectorAll('.is-current')), ['2', '0'], '再生中のイニングの見出しと得点を強調する');
  assert.equal(buildLineScore({ innings: new Map(), runs: 0, hits: 0, errors: 0 }, 9, null).querySelector('.is-current'), null);
});

test('再生中のイニングに関わらず、スコアボードには試合全体の結果を表示する（issue #156）', () => {
  installDom('<div id="line-score"></div>');
  const container = document.querySelector('#line-score');
  renderLineScore(container, [played(1, 2, 4), played(1, 0, 1), played(5, 1, 0), played(10, 0, 2)], 1);
  assert.deepEqual(texts(container.querySelectorAll('tbody td')).slice(-3), ['3', '3', '0']);
  assert.equal(container.querySelectorAll('thead th').length, 1 + 10 + 3, '延長10回まで列を用意する');
  assert.deepEqual(texts(container.querySelectorAll('thead .is-current')), ['1']);
});
