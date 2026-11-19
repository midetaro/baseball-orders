// 全画面共通の左メニューの開閉。他の画面への導線はメニュー内にだけ置き、既定では閉じておく。
import { requireElement } from '../shared/dom.ts';

/** メニューボタン・閉じるボタン・メニュー外のクリック・Escapeキーで左メニューを開閉する。 */
export function initSiteMenu(root: Document = document): void {
  const toggle = requireElement<HTMLButtonElement>('#menu-toggle', root);
  const menu = requireElement('#site-menu', root);
  const close = requireElement<HTMLButtonElement>('#menu-close', root);
  const backdrop = requireElement('#menu-backdrop', root);
  // 開いたらメニューの先頭のリンクへ、閉じたらメニューボタンへフォーカスを戻す。
  const setOpen = (open: boolean): void => {
    menu.hidden = !open;
    backdrop.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    if (open) {
      requireElement<HTMLAnchorElement>('a', menu).focus();
    } else {
      toggle.focus();
    }
  };
  toggle.addEventListener('click', () => setOpen(Boolean(menu.hidden)));
  close.addEventListener('click', () => setOpen(false));
  backdrop.addEventListener('click', () => setOpen(false));
  root.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !menu.hidden) setOpen(false);
  });
}
