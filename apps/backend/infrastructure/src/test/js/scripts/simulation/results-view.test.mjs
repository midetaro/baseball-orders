import assert from 'node:assert/strict';
import { test } from 'node:test';
import { SimulationResultsView } from '../../../../main/typescript/simulation/results-view.ts';
import { bindShareButton, shareText } from '../../../../main/typescript/simulation/share.ts';
import { installDom, readTemplate, settle } from '../../support/dom.mjs';

const $ = selector => document.querySelector(selector);
const statistics = {
  gameCount: 4, averageScore: 3.5, medianScore: 3, maximumScore: 9, hitCount: 40, homeRunCount: 4,
  buntCount: 2, buntFailureCount: 1, stealCount: 3, stealFailureCount: 1,
  singleHitCount: 30, doubleHitCount: 6, tripleHitCount: 0,
  soloHomeRunCount: 3, grandSlamCount: 1,
  advancingBuntCount: 2, squeezeBuntCount: 0, advancingBuntFailureCount: 1, squeezeBuntFailureCount: 0,
  stealToSecondCount: 3, stealToThirdCount: 0,
  scoreDistribution: { 9: 1, 3: 3 },
};

for (const template of ['simulation', 'batting-order']) {
  test(`${template}画面の結果パネルに集計値・得点分布・内訳を描画する`, () => {
    installDom(readTemplate(template));
    new SimulationResultsView().render(statistics);

    for (const [id, value] of [['game-count', '4'], ['average-score', '3.5'], ['median-score', '3'], ['maximum-score', '9'], ['maximum-score-card', '9'], ['hit-count', '40'], ['home-run-count', '4'], ['bunt-count', '2'], ['bunt-failure-count', '1'], ['steal-count', '3'], ['steal-failure-count', '1']]) {
      assert.equal($(`#${id}`).textContent, value, id);
    }
    assert.deepEqual([...$('#score-distribution-axis').children].map(label => label.textContent), ['75%', '50%', '25%', '0%']);
    assert.equal($('#score-histogram-bars').style.getPropertyValue('--histogram-grid-step'), `${(25 / 75) * 100}%`);
    const bars = [...$('#score-histogram-bars').children];
    assert.deepEqual(bars.map(bar => bar.getAttribute('aria-label')), ['3点: 75.0%', '9点: 25.0%']);
    assert.deepEqual(bars.map(bar => [bar.className, bar.firstElementChild.style.height, bar.lastElementChild.textContent]), [['histogram-bar', '100%', '3点'], ['histogram-bar', `${(25 / 75) * 100}%`, '9点']]);
    assert.equal($('#score-empty-state').hidden, true);

    const hitSegments = [...$('#hit-breakdown').children];
    assert.deepEqual(hitSegments.map(segment => [segment.className, segment.style.width, segment.getAttribute('aria-label')]), [
      ['single-hit', '75%', '一塁打: 30 (75.0%)'],
      ['double-hit', '15%', '二塁打: 6 (15.0%)'],
      ['home-run', '10%', '本塁打: 4 (10.0%)'],
    ], '件数0の三塁打は描かない');
    assert.deepEqual([...$('#hit-legend').children].map(item => [item.firstElementChild.className, item.textContent]), [
      ['legend-swatch single-hit', '一塁打 30 (75.0%)'],
      ['legend-swatch double-hit', '二塁打 6 (15.0%)'],
      ['legend-swatch home-run', '本塁打 4 (10.0%)'],
    ]);
    assert.deepEqual([...$('#home-run-legend').children].map(item => item.textContent), ['ソロ 3 (75.0%)', '満塁 1 (25.0%)']);
    assert.deepEqual([...$('#bunt-legend').children].map(item => item.textContent), ['進塁成功 2 (66.7%)', '進塁失敗 1 (33.3%)']);
    assert.deepEqual([...$('#steal-legend').children].map(item => item.textContent), ['二盗成功 3 (75.0%)', '失敗 1 (25.0%)']);
    for (const prefix of ['hit', 'home-run', 'bunt', 'steal']) {
      assert.equal($(`#${prefix}-empty-state`).hidden, true, `${prefix}の空状態を隠す`);
    }
    assert.deepEqual([$('#result-feedback').className, $('#result-feedback').textContent], ['hint success', '試合終了：4試合']);
  });
}

test('試合結果が無ければ0を表示し、空状態の表示を出す', () => {
  installDom(readTemplate('simulation'));
  const sut = new SimulationResultsView();
  sut.render(statistics);
  sut.render({ gameCount: 0 });
  assert.equal($('#game-count').textContent, '0');
  assert.equal($('#maximum-score-card').textContent, '0');
  assert.equal($('#score-histogram-bars').children.length, 0, '前回の結果を残さない');
  assert.equal($('#hit-breakdown').children.length, 0);
  for (const id of ['score-empty-state', 'hit-empty-state', 'home-run-empty-state', 'bunt-empty-state', 'steal-empty-state']) {
    assert.equal($(`#${id}`).hidden, false, id);
  }
  assert.equal($('#result-feedback').textContent, '試合結果はありません。');
});

/** navigator を差し替えて共有ボタンを押す。 */
async function share(navigator) {
  installDom(readTemplate('simulation'));
  new SimulationResultsView().render(statistics);
  Object.defineProperty(globalThis, 'navigator', { value: navigator, configurable: true });
  bindShareButton();
  $('#share-results').click();
  await settle();
  return $('#result-feedback');
}

test('対応ブラウザでは表示中の平均・中央値・最大得点をネイティブ共有する', async () => {
  const shared = [];
  const feedback = await share({ share: async data => shared.push(data) });
  assert.deepEqual(shared, [{ title: '打順監督の結果', text: shareText() }]);
  for (const part of ['打順監督の結果', '平均得点: 3.5', '中央値得点: 3', '最大得点: 9']) {
    assert.ok(shared[0].text.includes(part), part);
  }
  assert.deepEqual([feedback.className, feedback.textContent], ['hint success', '結果を共有しました。']);
});

test('ネイティブ共有に非対応なら共有文をコピーする', async () => {
  const copied = [];
  const feedback = await share({ clipboard: { writeText: async text => copied.push(text) } });
  assert.deepEqual(copied, [shareText()]);
  assert.equal(feedback.textContent, '共有用テキストをコピーしました。');
});

test('共有を取り消したら何も表示せず、失敗したら共有できなかったことを表示する', async () => {
  const cancelled = await share({ share: async () => { throw Object.assign(new Error('取消'), { name: 'AbortError' }); } });
  assert.equal(cancelled.textContent, '試合終了：4試合');
  const failed = await share({ share: async () => { throw new Error('失敗'); } });
  assert.deepEqual([failed.className, failed.textContent], ['hint error', '共有できませんでした。']);
});
