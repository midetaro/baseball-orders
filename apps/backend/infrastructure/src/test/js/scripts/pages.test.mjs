import assert from 'node:assert/strict';
import { existsSync, readdirSync } from 'node:fs';
import { test } from 'node:test';
import { loadPage, readTemplate } from '../support/dom.mjs';

const pages = readdirSync(new URL('../../../main/resources/templates/', import.meta.url)).filter(name => name.endsWith('.html')).map(name => name.replace(/\.html$/, ''));
const entry = name => new URL(`../../../main/typescript/pages/${name}.ts`, import.meta.url);
let loadCount = 0;

/** 画面の入口モジュールを、Thymeleafが描画する画面と同じDOMで実行する。 */
async function boot(name) {
  loadPage(name);
  // 入口モジュールは読み込み時に画面を組み立てるため、画面ごとに読み込み直す。
  loadCount += 1;
  await import(`${entry(name).href}?load=${loadCount}`);
}

test('各画面は、画面名と同じ入口モジュールだけをES moduleとして読み込む', () => {
  assert.deepEqual(pages.sort(), ['batting-order', 'simulation', 'simulation-guide', 'single-game']);
  for (const name of pages) {
    const html = readTemplate(name);
    assert.deepEqual([...html.matchAll(/<script[^>]*>/g)].map(([tag]) => tag), [`<script src="/js/pages/${name}.js" type="module">`], `${name}はインラインのスクリプトを書かない`);
    assert.ok(html.includes(`<script src="/js/pages/${name}.js" type="module"></script>\n</body>`), `${name}は本文の最後でスクリプトを読み込む`);
    assert.ok(existsSync(entry(name)), `/js/pages/${name}.js は pages/${name}.ts から生成する`);
  }
});

for (const name of ['batting-order', 'simulation', 'single-game']) {
  test(`${name}画面の入口は、テンプレートの要素で打順入力フォームと左メニューを開始する`, async () => {
    await boot(name);
    assert.equal(document.querySelectorAll('#order .slot').length, 9, '打順を描画する');
    document.querySelector('#menu-toggle').click();
    assert.equal(document.querySelector('#site-menu').hidden, false, '左メニューを開ける');
  });
}

test('simulation-guide画面の入口は左メニューだけを開始する', async () => {
  await boot('simulation-guide');
  document.querySelector('#menu-toggle').click();
  assert.equal(document.querySelector('#site-menu').hidden, false);
});
