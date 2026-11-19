import assert from 'node:assert/strict';
import { test } from 'node:test';
import { SingleGameView, startSingleGamePage } from '../../../../main/typescript/single-game/single-game-page.ts';
import { installDom, readTemplate, settle, stubFetch } from '../../support/dom.mjs';

const $ = selector => document.querySelector(selector);
const transitions = [
  { inning: 1, actionResult: '本塁打', outCount: 0, cumulativeScore: 0, runnerState: '走者なし' },
  { inning: 1, actionResult: '三振', outCount: 0, cumulativeScore: 1, runnerState: '走者なし' },
];

test('推移が無ければフレーム・スコアボード・打席結果表に結果なしと表示する', () => {
  installDom(readTemplate('single-game'));
  const sut = new SingleGameView();
  for (const empty of [null, undefined, []]) {
    sut.render(empty);
    for (const id of ['frame-stage', 'line-score', 'order-table-scroll']) {
      assert.equal($(`#${id}`).textContent, '試合結果はありません。', `${id}`);
      assert.equal($(`#${id} .empty-chart-state`) !== null, true);
    }
  }
});

test('1試合実行APIへ打順を送り、成功したら試合の推移を再生して打席結果表を表示する', async t => {
  installDom(readTemplate('single-game'));
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const api = stubFetch();
  startSingleGamePage();
  assert.equal($('#feedback').textContent, '準備完了。1試合を実行できます。');

  $('#submit').click();
  assert.equal(api.requests[0].url, '/simulations/single-game');
  assert.equal($('#feedback').textContent, '試合を実行中…');
  api.respond(200, { transitions });
  await settle();

  assert.deepEqual([$('#result-feedback').className, $('#result-feedback').textContent], ['hint success', '試合が終了しました。']);
  assert.equal($('#results').hidden, false, '試合結果タブへ切り替える');
  assert.equal($('#frame-stage .frame-banner').textContent, '本塁打');
  assert.equal($('#line-score table').getAttribute('aria-label'), 'スコアボード');
  assert.equal($('#order-table-scroll .order-table tbody td').textContent, '本塁打');

  $('.speed-option[data-speed="slow"]').click();
  assert.equal($('.speed-option[data-speed="slow"]').getAttribute('aria-pressed'), 'true', '再生速度を切り替えられる');
});
