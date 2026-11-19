// 大規模実行の画面（大規模実行画面と打順組み替え画面）を組み立てる。
import { LineupForm, findLineupFormElements } from '../lineup/lineup-form.ts';
import { SimulationResultsView } from './results-view.ts';
import { bindShareButton } from './share.ts';
import type { SimulationResponse } from './statistics.ts';

/** 打順を大規模実行APIへ送り、集計結果を結果画面に描画する画面を開始する。 */
export function startSimulationPage(root: ParentNode = document): void {
  const resultsView = new SimulationResultsView(root);
  bindShareButton(root);
  new LineupForm<SimulationResponse>(findLineupFormElements(root), {
    readyMessage: '準備完了。シミュレーションを実行できます。',
    runningMessage: 'シミュレーション中…',
    endpoint: '/simulations',
    onSuccess: data => resultsView.render(data.statistics),
  }).start();
}
