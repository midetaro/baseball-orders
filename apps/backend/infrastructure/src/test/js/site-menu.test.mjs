import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(`../../main/resources/${path}`, import.meta.url), 'utf8');
const fragment = read('templates/fragments/site-menu.html');
const css = read('static/css/site-menu.css');

// --- 全画面共通の左メニュー（issue #146） ---
// 画面遷移は左メニューからだけ行う。各画面は共通の断片を読み込み、自身のルートを現在の画面として渡す。
const pages = [
  ['batting-order', '/'],
  ['simulation', '/large-scale'],
  ['single-game', '/single-game'],
  ['simulation-guide', '/simulation-guide']
];
for (const [name, route] of pages) {
  const html = read(`templates/${name}.html`);
  assert.ok(html.includes(`<th:block th:replace="~{fragments/site-menu :: siteMenu('${route}')}"></th:block>`), `${name}画面に共通の左メニューを表示し、${route}を現在の画面として示す`);
  assert.ok(html.includes('<button th:replace="~{fragments/site-menu :: menuToggle}"></button>'), `${name}画面にメニューを開くボタンを表示する`);
  assert.ok(html.includes('<link rel="stylesheet" href="/css/site-menu.css">'), `${name}画面で共通の左メニューCSSを読み込む`);
  assert.doesNotMatch(html, /<a [^>]*href="\/(large-scale|single-game|simulation-guide)?"/, `${name}画面にはメニュー以外の画面遷移リンクを置かない`);
}

assert.ok(fragment.includes('<th:block th:fragment="siteMenu(current)">\n<nav aria-label="画面メニュー" class="site-menu" hidden id="site-menu">'), '左メニューは表示中の画面を受け取る共通断片にし、既定で閉じておく');
for (const [href, label] of [['/', '打順組み替え'], ['/large-scale', '大規模実行'], ['/single-game', '1試合実行'], ['/simulation-guide', 'シミュレーションの仕組み']]) {
  assert.ok(fragment.includes(`<a href="${href}" th:attr="aria-current=\${current == '${href}'} ? 'page'">${label}</a>`), `メニューから${label}画面を選べ、表示中ならそれを示す`);
}
assert.ok(fragment.includes('<div class="menu-backdrop" hidden id="menu-backdrop"></div>'), 'メニュー外を覆う背景を既定で隠す');
assert.match(fragment, /<button aria-controls="site-menu" aria-expanded="false" class="menu-toggle" id="menu-toggle" th:fragment="menuToggle" type="button">/, 'メニューを開くボタンは閉じた状態を示す');

// 案内画面は他画面のCSS変数を持たないため、メニューCSSは既定値付きで単独で成り立たせる。
assert.ok(css.includes('.site-menu {') && css.includes('left: 0'), 'メニューは画面左に表示する');
assert.doesNotMatch(css, /var\(--[a-z-]+\)/, 'メニューCSSは画面固有のCSS変数に既定値なしで依存しない');

// メニューの開閉の振る舞いは scripts/site-menu/site-menu.test.mjs で検査する。

console.log('PASS: 全画面共通の左メニュー');
