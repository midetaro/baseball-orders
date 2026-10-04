// 大規模実行の結果の共有。対応ブラウザではネイティブ共有を使い、非対応ならクリップボードへコピーする。
import { requireElement } from '../shared/dom.ts';

const SHARE_TITLE = '打順監督の結果';

/** 結果画面に表示中の平均・中央値・最大得点から共有文を作る。 */
export function shareText(root: ParentNode = document): string {
  const text = (selector: string): string | null => requireElement(selector, root).textContent;
  return `${SHARE_TITLE}\\n平均得点: ${text('#average-score')}\\n中央値得点: ${text('#median-score')}\\n最大得点: ${text('#maximum-score')}`;
}

/** 共有ボタンを有効にし、共有の結果を結果画面の通知領域へ表示する。利用者が共有を取り消した場合は何も表示しない。 */
export function bindShareButton(root: ParentNode = document): void {
  const button = requireElement<HTMLButtonElement>('#share-results', root);
  const resultFeedback = requireElement('#result-feedback', root);
  const notify = (className: string, message: string): void => {
    resultFeedback.className = className;
    resultFeedback.textContent = message;
  };
  button.addEventListener('click', async () => {
    const text = shareText(root);
    try {
      if (navigator.share) {
        await navigator.share({ title: SHARE_TITLE, text });
        notify('hint success', '結果を共有しました。');
      } else {
        await navigator.clipboard.writeText(text);
        notify('hint success', '共有用テキストをコピーしました。');
      }
    } catch (error) {
      if ((error as { name?: string }).name !== 'AbortError') {
        notify('hint error', '共有できませんでした。');
      }
    }
  });
}
