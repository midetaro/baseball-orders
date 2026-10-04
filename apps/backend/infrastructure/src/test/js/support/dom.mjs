import { readFileSync } from 'node:fs';
import { JSDOM } from 'jsdom';

const resource = path => new URL(`../../../main/resources/${path}`, import.meta.url);

/** テンプレートのHTMLを読む。 */
export const readTemplate = name => readFileSync(resource(`templates/${name}.html`), 'utf8');

/**
 * HTMLをjsdomで読み込み、画面スクリプトが参照するグローバル（window・document）を差し替える。
 * jsdomに無いスクロールとWeb Animationsは、何もしない実装で補う。
 */
export function installDom(html) {
  const { window } = new JSDOM(html, { pretendToBeVisual: true });
  window.scrollTo = () => {};
  window.Element.prototype.getAnimations = function getAnimations() {
    return [];
  };
  globalThis.window = window;
  globalThis.document = window.document;
  return window;
}

/**
 * Thymeleafが描画する画面を再現して読み込む。
 * jsdomはThymeleafの断片を展開しないため、共通の左メニュー断片の要素を差し込む。
 */
export function loadPage(name) {
  const window = installDom(readTemplate(name));
  const fragment = new JSDOM(readTemplate('fragments/site-menu')).window.document;
  const { document } = window;
  document.querySelector('button[th\\:replace*="menuToggle"]').outerHTML = fragment.querySelector('#menu-toggle').outerHTML;
  document.querySelector('th\\:block[th\\:replace*="siteMenu"]').outerHTML =
    fragment.querySelector('#site-menu').outerHTML + fragment.querySelector('#menu-backdrop').outerHTML;
  return window;
}

/** 入力欄に値を入れて、入力イベントを発生させる。 */
export function typeInto(input, value, type = 'input') {
  input.value = value;
  input.dispatchEvent(new input.ownerDocument.defaultView.Event(type, { bubbles: true }));
}

/** fetchの応答を差し替え、送信内容を記録する。resolve を呼ぶまで応答を保留する。 */
export function stubFetch() {
  const requests = [];
  let respond;
  globalThis.fetch = (url, init) => {
    requests.push({ url, init, body: JSON.parse(init.body) });
    return new Promise(resolve => {
      respond = (status, data) => resolve({ ok: status >= 200 && status < 300, status, json: async () => data });
    });
  };
  return { requests, respond: (status, data) => respond(status, data) };
}

/** 保留中のPromiseの処理を進める。 */
export const settle = () => new Promise(resolve => setImmediate(resolve));
