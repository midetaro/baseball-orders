// 打順に並ぶ打者の値と、その検証・変換。DOMに依存しない。

/** 打者の性格。APIの personality にそのまま送る。 */
export type Personality = 'DEFAULT' | 'MIDDLE_DISTANCE' | 'EAGER_SLUGGISH' | 'HIGH_ON_BASE' | 'EAGER_STEAL' | 'EAGER_BUNT';

/** 性格の表示名。キーの並びが選択肢の並びになる。 */
export const PERSONALITY_LABELS = {
  DEFAULT: '単打マン',
  MIDDLE_DISTANCE: '中距離砲',
  EAGER_SLUGGISH: '長距離砲',
  HIGH_ON_BASE: '高出塁率',
  EAGER_STEAL: '盗塁重視',
  EAGER_BUNT: 'バント職人',
} as const satisfies Record<Personality, string>;

/** 画面で編集する打者。打率は入力欄の文字列のまま持つ。 */
export interface LineupPlayer {
  hitAverage: string;
  buntEnabled: boolean;
  stealEnabled: boolean;
  personality: Personality;
  /** チーム既定でバントが固定されていれば切り替えられない。 */
  buntForced?: boolean;
  /** チーム既定で盗塁が固定されていれば切り替えられない。 */
  stealForced?: boolean;
  /** 打順組み替え画面だけで使う、送信しないメモ。 */
  memo?: string;
}

/** シミュレーションAPIへ送る打者。 */
export interface LineupRequestPlayer {
  hit_average: number;
  bunt_enabled: boolean;
  steal_enabled: boolean;
  personality: Personality;
}

/** 入力できる打率の範囲。 */
export const HIT_AVERAGE_RANGE = { min: 0.01, max: 0.6 } as const;

/** 入力が範囲外のときに表示する案内。 */
export const HIT_AVERAGE_RANGE_MESSAGE = `打率は${HIT_AVERAGE_RANGE.min}〜${HIT_AVERAGE_RANGE.max}の範囲ですべての項目を入力してください。`;

/** メモに入力できる最大文字数。 */
export const MEMO_MAX_LENGTH = 20;

/** 大規模実行画面と1試合実行画面の初期打順を作る。一〜五番は高打率、六番以降は打順が下がるほど低打率にする。 */
export function createInitialLineup(): LineupPlayer[] {
  const player = (hitAverage: string, tacticsEnabled: boolean): LineupPlayer => ({
    hitAverage,
    buntEnabled: tacticsEnabled,
    stealEnabled: tacticsEnabled,
    personality: 'DEFAULT',
  });
  return [
    player('0.35', true),
    player('0.35', true),
    player('0.35', false),
    player('0.35', false),
    player('0.35', false),
    player('0.33', true),
    player('0.29', true),
    player('0.28', true),
    player('0.27', true),
  ];
}

/** 打率が入力済みで、入力できる範囲に収まっているかを返す。 */
export function isValidPlayer(player: Pick<LineupPlayer, 'hitAverage'>): boolean {
  const hitAverage = Number(player.hitAverage);
  return player.hitAverage !== '' && hitAverage >= HIT_AVERAGE_RANGE.min && hitAverage <= HIT_AVERAGE_RANGE.max;
}

/** 全打者の打率が有効かを返す。 */
export function isValidLineup(players: readonly Pick<LineupPlayer, 'hitAverage'>[]): boolean {
  return players.every(isValidPlayer);
}

/** 平均打率を小数第3位で返す。無効な打率が含まれていれば「—」を返す。 */
export function averageHitAverage(players: readonly Pick<LineupPlayer, 'hitAverage'>[]): string {
  if (!isValidLineup(players)) {
    return '—';
  }
  const total = players.reduce((sum, player) => sum + Number(player.hitAverage), 0);
  return (total / players.length).toFixed(3);
}

/** 入力途中の「.3」のような値に小数点前の0を補う。 */
export function withLeadingZero(value: string): string {
  return value.startsWith('.') ? `0${value}` : value;
}

/** 確定した打率を小数第2位までの表示にそろえる。未入力はそのまま返す。 */
export function formatHitAverage(value: string): string {
  return value === '' ? value : Number(value).toFixed(2);
}

/** 打順をAPIへ送る形に変換する。メモや固定フラグは送らない。 */
export function toLineupRequest(players: readonly LineupPlayer[]): LineupRequestPlayer[] {
  return players.map(player => ({
    hit_average: Number(player.hitAverage),
    bunt_enabled: player.buntEnabled,
    steal_enabled: player.stealEnabled,
    personality: player.personality,
  }));
}

/** チーム既定の打者を、テンプレートのdata属性から読み取る。固定された盗塁・バントだけを有効にして始める。 */
export function parseDefaultBatters(elements: Iterable<{ dataset: DOMStringMap }>): LineupPlayer[] {
  return Array.from(elements, element => {
    const data = element.dataset;
    const stealForced = data.stealForced === 'true';
    const buntForced = data.buntForced === 'true';
    return {
      hitAverage: data.hitAverage ?? '',
      personality: data.personality as Personality,
      stealForced,
      buntForced,
      stealEnabled: stealForced,
      buntEnabled: buntForced,
      memo: '',
    };
  });
}

/**
 * 打者を from の打順から to の打順へ移し、間の打者を詰める。
 * 移動しない場合や範囲外の場合は何もせず false を返す。
 */
export function movePlayer<T>(players: T[], from: number, to: number): boolean {
  if (from === to || to < 0 || to >= players.length) {
    return false;
  }
  const [moved] = players.splice(from, 1);
  players.splice(to, 0, moved);
  return true;
}
