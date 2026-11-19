// 画面スクリプトが共通で使うDOM操作。

/**
 * テンプレートに必ず存在する要素を取得する。
 * 見つからない場合はテンプレートとスクリプトの食い違いなので、黙って続けずに例外にする。
 */
export function requireElement<E extends Element = HTMLElement>(selector: string, root: ParentNode = document): E {
  const element = root.querySelector<E>(selector);
  if (element === null) {
    throw new Error(`画面に ${selector} がありません。`);
  }
  return element;
}

/** 画面によっては存在しない要素を取得する。 */
export function optionalElement<E extends Element = HTMLElement>(selector: string, root: ParentNode = document): E | null {
  return root.querySelector<E>(selector);
}

/** クラス名と表示テキストを指定して要素を作る。 */
export function createElement<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className = '',
  text?: string,
): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  if (className !== '') {
    element.className = className;
  }
  if (text !== undefined) {
    element.textContent = text;
  }
  return element;
}

/** 結果がまだ無い領域に表示する案内文を作る。 */
export function emptyState(text: string): HTMLParagraphElement {
  return createElement('p', 'empty-chart-state', text);
}
