import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

// 実行: node infrastructure/src/test/js/simulation.test.mjs
// 実際の画面スクリプトを実行する。DOMとHTTPのみを置換し、ブラウザ描画は対象外。
const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
function element(tagName) {
  return { tagName, children: [], handlers: {}, disabled: false, textContent: '',
    append(...children) { this.children.push(...children); },
    replaceChildren() { this.children = []; },
    addEventListener(type, handler) { this.handlers[type] = handler; },
    setAttribute(key, value) { this[key] = value; } };
}
function setup() {
  const nodes = Object.fromEntries(['order', 'submit', 'feedback', 'selected-count', 'results', 'result-rows', 'average-score', 'median-score', 'maximum-score'].map(id => [`#${id}`, element()]));
  const cards = Array.from({ length: 9 }, (_, i) => {
    const add = element();
    return { dataset: { id: String(i + 1), name: `選手${i + 1}`, hitAverage: '0.300', sluggish: '0.450', stealSuccessRate: '80%' }, querySelector: () => add };
  });
  let resolve;
  const requests = [];
  const document = { querySelector: id => nodes[id], createElement: element,
    querySelectorAll: selector => selector === '.player' ? cards : [
      ...cards.map(card => card.querySelector()),
      ...nodes['#order'].children.flatMap(row => row.children.at(-1).children)
    ] };
  runInNewContext(script, { document, fetch: (...args) => {
    requests.push(args); return new Promise(done => { resolve = done; });
  } });
  return { nodes, cards, requests, respond: value => resolve(value) };
}

export async function verifySimulationPage() {
  const page = setup();
  assert.equal(page.nodes['#selected-count'].textContent, 9, '初期表示で先頭9人を打順へ設定する');
  assert.equal(page.nodes['#submit'].disabled, false, '初期表示からシミュレーションを実行できる');
  assert.equal(page.nodes['#order'].children[0].children[1].textContent, '選手1');
  assert.equal(page.nodes['#order'].children[8].children[1].textContent, '選手9');
  const buntButton = page.nodes['#order'].children[0].children[5];
  buntButton.handlers.click();
  assert.equal(buntButton['aria-pressed'], 'true', '打順ごとにバント実行を選択できる');
  const pending = page.nodes['#submit'].handlers.click();
  assert.equal(page.nodes['#submit'].disabled, true);
  assert.equal(page.cards[0].querySelector().disabled, true, '待機中は打順操作を無効にする');
  const controls = page.nodes['#order'].children[0].children.at(-1).children;
  controls.forEach(button => assert.equal(button.disabled, true));
  controls[2].handlers.click();
  assert.equal(page.nodes['#selected-count'].textContent, 9);
  assert.equal(page.nodes['#feedback'].textContent, 'シミュレーション中…');
  // disabled属性だけでなく、イベントが重複しても二重要求を送らない。
  await page.nodes['#submit'].handlers.click();
  assert.equal(page.requests.length, 1);
  assert.deepEqual(JSON.parse(page.requests[0][1].body), Array.from({ length: 9 }, (_, i) => ({ player_id: i + 1, bunt_enabled: i === 0 })));
  page.respond({ ok: true, json: async () => ({ simulationId: 'request-id', results: [{ score: 5, runs: 4 }, { score: 0, runs: 2 }, { score: 3, runs: 3 }], statistics: { averageScore: 2.67, medianScore: 3, maximumScore: 5 } }) });
  await pending;
  assert.equal(page.nodes['#feedback'].textContent, '試合終了：3試合');
  assert.equal(page.nodes['#feedback'].className, 'success');
  assert.equal(page.nodes['#results'].hidden, false);
  assert.equal(page.nodes['#average-score'].textContent, '2.67');
  assert.equal(page.nodes['#median-score'].textContent, '3');
  assert.equal(page.nodes['#maximum-score'].textContent, '5');
  assert.deepEqual(page.nodes['#result-rows'].children.map(row => {
    assert.equal(row.tagName, 'tr');
    return row.children.map(cell => {
      assert.equal(cell.tagName, 'td');
      return cell.textContent;
    });
  }), [['1', '5', '4'], ['2', '0', '2'], ['3', '3', '3']]);
  assert.match(html, /<table[\s>]/);
  for (const label of ['試合', '得点', '失点']) {
    assert.ok(html.includes(`<th scope="col">${label}</th>`));
  }
  assert.equal(page.nodes['#submit'].disabled, false);
  controls.forEach(button => assert.equal(button.disabled, false));
  assert.equal(page.cards[0].querySelector().disabled, false);

  const retry = page.nodes['#submit'].handlers.click();
  assert.equal(page.nodes['#results'].hidden, true);
  assert.equal(page.nodes['#result-rows'].children.length, 0);
  page.respond({ ok: false, json: async () => ({ error: '結果の待機がタイムアウトしました。' }) });
  await retry;
  assert.equal(page.nodes['#feedback'].textContent, '結果の待機がタイムアウトしました。');
  assert.equal(page.nodes['#feedback'].className, 'error');
  assert.equal(page.nodes['#results'].hidden, true);
  assert.equal(page.nodes['#submit'].disabled, false);
  const empty = page.nodes['#submit'].handlers.click();
  page.respond({ ok: true, json: async () => ({ results: [], statistics: { averageScore: 0, medianScore: 0, maximumScore: 0 } }) });
  await empty;
  assert.equal(page.nodes['#feedback'].textContent, '試合結果はありません。');
  assert.equal(page.nodes['#result-rows'].children.length, 0);
  assert.equal(page.nodes['#results'].hidden, true);

  const next = page.nodes['#submit'].handlers.click();
  page.respond({ ok: true, json: async () => ({ results: [{ score: 1, runs: 0 }], statistics: { averageScore: 1, medianScore: 1, maximumScore: 1 } }) });
  await next;
  assert.equal(page.nodes['#result-rows'].children.length, 1);
  assert.equal(page.nodes['#results'].hidden, false);
}

await verifySimulationPage();
console.log('PASS: 結果表示、二重送信防止、待機中操作制御、エラー表示、再実行');
