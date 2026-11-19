import assert from 'node:assert/strict';
import { test } from 'node:test';
import { initSiteMenu } from '../../../../main/typescript/site-menu/site-menu.ts';
import { loadPage } from '../../support/dom.mjs';

const $ = selector => document.querySelector(selector);
const state = () => ({
  hidden: $('#site-menu').hidden,
  backdropHidden: $('#menu-backdrop').hidden,
  expanded: $('#menu-toggle').getAttribute('aria-expanded'),
  focused: document.activeElement?.id || document.activeElement?.textContent,
});

test('メニューボタンで左メニューを開き、Escape・閉じるボタン・メニュー外のクリックで閉じる（issue #146）', () => {
  loadPage('simulation-guide');
  initSiteMenu();
  $('#menu-toggle').click();
  assert.deepEqual(state(), { hidden: false, backdropHidden: false, expanded: 'true', focused: '打順組み替え' }, '開いたらメニューの先頭のリンクへフォーカスする');
  document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape' }));
  assert.deepEqual(state(), { hidden: true, backdropHidden: true, expanded: 'false', focused: 'menu-toggle' }, '閉じたらメニューボタンへフォーカスを戻す');
  $('#menu-toggle').click();
  $('#menu-backdrop').click();
  assert.equal($('#site-menu').hidden, true, 'メニュー外をクリックすると閉じる');
  $('#menu-toggle').click();
  $('#menu-close').click();
  assert.equal($('#site-menu').hidden, true, '閉じるボタンで閉じる');
  $('#menu-toggle').click();
  $('#menu-toggle').click();
  assert.equal($('#site-menu').hidden, true, 'メニューボタンをもう一度押すと閉じる');
});

test('閉じているときのEscapeではフォーカスを動かさない', () => {
  loadPage('simulation-guide');
  initSiteMenu();
  document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape' }));
  assert.equal(document.activeElement, document.body);
});
