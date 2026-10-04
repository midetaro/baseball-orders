// 大規模実行の集計結果を、画面に描く値へ変換する。DOMに依存しない。

/** シミュレーションAPIが返す集計値。未集計の項目は省略されうる。 */
export interface SimulationStatistics {
  gameCount?: number;
  averageScore?: number;
  medianScore?: number;
  maximumScore?: number;
  hitCount?: number;
  homeRunCount?: number;
  buntCount?: number;
  buntFailureCount?: number;
  stealCount?: number;
  stealFailureCount?: number;
  singleHitCount?: number;
  doubleHitCount?: number;
  tripleHitCount?: number;
  soloHomeRunCount?: number;
  twoRunHomeRunCount?: number;
  threeRunHomeRunCount?: number;
  grandSlamCount?: number;
  advancingBuntCount?: number;
  squeezeBuntCount?: number;
  advancingBuntFailureCount?: number;
  squeezeBuntFailureCount?: number;
  stealToSecondCount?: number;
  stealToThirdCount?: number;
  /** 得点ごとの試合数。キーは得点の文字列。 */
  scoreDistribution?: Record<string, number>;
}

/** シミュレーションAPIの応答。 */
export interface SimulationResponse {
  statistics: SimulationStatistics;
}

/** 件数をそのまま表示する集計項目。要素IDは項目名をケバブケースにしたもの。 */
export const SUMMARY_KEYS = [
  'gameCount',
  'averageScore',
  'medianScore',
  'maximumScore',
  'hitCount',
  'homeRunCount',
  'buntCount',
  'buntFailureCount',
  'stealCount',
  'stealFailureCount',
] as const satisfies readonly (keyof SimulationStatistics)[];

/** 集計項目名を、値を表示する要素のIDへ変換する（例: gameCount → game-count）。 */
export function summaryElementId(key: (typeof SUMMARY_KEYS)[number]): string {
  return key.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`);
}

/** 内訳の1項目。kind は色分けに使うクラス名。 */
export interface BreakdownItem {
  label: string;
  count: number | undefined;
  kind: string;
}

/** 内訳グループ。prefix から内訳グラフ・凡例・空状態の要素IDを決める（例: hit → hit-breakdown）。 */
export interface BreakdownGroup {
  prefix: 'hit' | 'home-run' | 'bunt' | 'steal';
  items: BreakdownItem[];
}

/** 安打・本塁打・バント・盗塁の内訳グループを作る。 */
export function breakdownGroups(statistics: SimulationStatistics): BreakdownGroup[] {
  return [
    {
      prefix: 'hit',
      items: [
        { label: '一塁打', count: statistics.singleHitCount, kind: 'single-hit' },
        { label: '二塁打', count: statistics.doubleHitCount, kind: 'double-hit' },
        { label: '三塁打', count: statistics.tripleHitCount, kind: 'triple-hit' },
        { label: '本塁打', count: statistics.homeRunCount, kind: 'home-run' },
      ],
    },
    {
      prefix: 'home-run',
      items: [
        { label: 'ソロ', count: statistics.soloHomeRunCount, kind: 'solo' },
        { label: 'ツーラン', count: statistics.twoRunHomeRunCount, kind: 'two-run' },
        { label: 'スリーラン', count: statistics.threeRunHomeRunCount, kind: 'three-run' },
        { label: '満塁', count: statistics.grandSlamCount, kind: 'grand-slam' },
      ],
    },
    {
      prefix: 'bunt',
      items: [
        { label: '進塁成功', count: statistics.advancingBuntCount, kind: 'advancing-bunt' },
        { label: 'スクイズ成功', count: statistics.squeezeBuntCount, kind: 'squeeze-bunt' },
        { label: '進塁失敗', count: statistics.advancingBuntFailureCount, kind: 'advancing-bunt-failure' },
        { label: 'スクイズ失敗', count: statistics.squeezeBuntFailureCount, kind: 'squeeze-bunt-failure' },
      ],
    },
    {
      prefix: 'steal',
      items: [
        { label: '二盗成功', count: statistics.stealToSecondCount, kind: 'steal-second' },
        { label: '三盗成功', count: statistics.stealToThirdCount, kind: 'steal-third' },
        { label: '失敗', count: statistics.stealFailureCount, kind: 'steal-failure' },
      ],
    },
  ];
}

/** 内訳グラフの1区間。rate は内訳合計に対する百分率。 */
export interface BreakdownSegment {
  label: string;
  count: number;
  kind: string;
  rate: number;
}

/**
 * 内訳の件数を割合にする。分母は集計総数ではなく内訳項目の合計にし、総数と内訳合計がずれても割合を誤表示しない。
 * 件数が0の項目は描かない。
 */
export function summarizeBreakdown(items: readonly BreakdownItem[]): { total: number; segments: BreakdownSegment[] } {
  let total = 0;
  for (const item of items) {
    total += Number(item.count) || 0;
  }
  const segments = items
    .filter(item => Number(item.count) > 0)
    .map(item => ({ label: item.label, count: Number(item.count), kind: item.kind, rate: (Number(item.count) / total) * 100 }));
  return { total, segments };
}

/** 得点分布の目盛りの刻み（%）。 */
export const HISTOGRAM_STEP_PERCENT = 25;

/** 得点分布ヒストグラムの棒。rate は全試合に対する割合、height は目盛り上限に対する棒の高さ（%）。 */
export interface HistogramBar {
  score: string;
  rate: number;
  height: number;
}

/** 得点分布ヒストグラムの描画内容。 */
export interface ScoreHistogram {
  /** 目盛りの上限（%）。最も高い棒が収まる刻みに切り上げ、最低でも1刻み分にする。 */
  maximum: number;
  /** 上から順に並べる目盛りの値（%）。 */
  axisLabels: number[];
  /** 目盛り線の間隔（グラフの高さに対する%）。 */
  gridStep: number;
  /** 得点の昇順に並べた棒。 */
  bars: HistogramBar[];
}

/** 得点ごとの試合数から得点分布ヒストグラムを組み立てる。試合数が0なら棒の高さは0にする。 */
export function scoreHistogram(statistics: SimulationStatistics): ScoreHistogram {
  const gameCount = Number(statistics.gameCount) || 0;
  const distribution = Object.entries(statistics.scoreDistribution ?? {}).sort(([left], [right]) => Number(left) - Number(right));
  const rates = distribution.map(([, count]) => (gameCount === 0 ? 0 : (Number(count) / gameCount) * 100));
  const maximumRate = Math.max(...rates, 0);
  const maximum = Math.max(HISTOGRAM_STEP_PERCENT, Math.ceil(maximumRate / HISTOGRAM_STEP_PERCENT) * HISTOGRAM_STEP_PERCENT);
  return {
    maximum,
    axisLabels: Array.from({ length: maximum / HISTOGRAM_STEP_PERCENT + 1 }, (_, index) => maximum - index * HISTOGRAM_STEP_PERCENT),
    gridStep: (HISTOGRAM_STEP_PERCENT / maximum) * 100,
    bars: distribution.map(([score], index) => ({ score, rate: rates[index], height: (rates[index] / maximum) * 100 })),
  };
}
