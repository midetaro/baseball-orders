import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

// 実行: node infrastructure/src/test/js/simulation.test.mjs
// 実際の画面スクリプトを実行する。DOMとHTTPのみを置換し、ブラウザ描画は対象外。
const html = readFileSync(new URL('../../main/resources/templates/simulation.html', import.meta.url), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
function element() {
  return { children: [], handlers: {}, disabled: false, textContent: '',
    append(...children) { this.children.push(...children); },
    replaceChildren() { this.children = []; },
    addEventListener(type, handler) { this.handlers[type] = handler; },
    setAttribute(key, value) { this[key] = value; } };
}
function setup() {
  const nodes = Object.fromEntries(['order', 'submit', 'feedback', 'selected-count'].map(id => [`#${id}`, element()]));
  const cards = Array.from({ length: 9 }, (_, i) => {
    const add = element();
    return { dataset: { id: String(i + 1), name: `選手${i + 1}` }, querySelector: () => add };
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
  cards.forEach(card => card.querySelector().handlers.click());
  return { nodes, cards, requests, respond: value => resolve(value) };
}

export async function verifySimulationPage() {
  const page = setup();
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
  assert.deepEqual(JSON.parse(page.requests[0][1].body), Array.from({ length: 9 }, (_, i) => ({ player_id: i + 1 })));
  page.respond({ ok: true, json: async () => ({ simulationId: 'request-id', score: 5, runs: 4 }) });
  await pending;
  assert.equal(page.nodes['#feedback'].textContent, '試合終了\n得点 5 — 失点 4');
  assert.equal(page.nodes['#feedback'].className, 'success');
  assert.equal(page.nodes['#submit'].disabled, false);
  controls.forEach(button => assert.equal(button.disabled, false));
  assert.equal(page.cards[0].querySelector().disabled, false);

  const retry = page.nodes['#submit'].handlers.click();
  page.respond({ ok: false, json: async () => ({ error: '結果の待機がタイムアウトしました。' }) });
  await retry;
  assert.equal(page.nodes['#feedback'].textContent, '結果の待機がタイムアウトしました。');
  assert.equal(page.nodes['#feedback'].className, 'error');
  assert.equal(page.nodes['#submit'].disabled, false);
}

await verifySimulationPage();
console.log('PASS: 結果表示、二重送信防止、待機中操作制御、エラー表示、再実行');
