// 打順の1行を構成する入力部品。状態は持たず、変更は呼び出し元へコールバックで伝える。
import { createElement } from '../shared/dom.ts';
import { HIT_AVERAGE_RANGE, MEMO_MAX_LENGTH, PERSONALITY_LABELS, type Personality } from './player.ts';

const ENABLED_TEXT = 'する';
const DISABLED_TEXT = 'しない';

/** 項目名と状態を別要素にしたラベル。幅が狭いときは項目名だけをCSSで省略できる。 */
function toggleCaption(label: string, stateText: string): [HTMLSpanElement, HTMLSpanElement] {
  return [createElement('span', 'toggle-label', `${label}:`), createElement('span', 'toggle-state', stateText)];
}

/** バント・盗塁を使うかどうかを切り替えるボタンを作る。読み上げでは項目名と状態を伝える。 */
export function toggleButton(label: string, enabled: boolean, disabled: boolean, onClick: () => void): HTMLButtonElement {
  const button = createElement('button', 'bunt-toggle');
  const stateText = enabled ? ENABLED_TEXT : DISABLED_TEXT;
  button.type = 'button';
  button.disabled = disabled;
  button.append(...toggleCaption(label, stateText));
  button.setAttribute('aria-label', `${label}: ${stateText}`);
  button.setAttribute('aria-pressed', String(enabled));
  button.addEventListener('click', onClick);
  return button;
}

/** チーム既定で固定されたバント・盗塁を、切り替えられないラベルとして作る。 */
export function forcedLabel(label: string): HTMLSpanElement {
  const wrapper = createElement('span', 'forced-label');
  wrapper.append(...toggleCaption(label, ENABLED_TEXT));
  wrapper.setAttribute('aria-label', `${label}: ${ENABLED_TEXT}（固定）`);
  return wrapper;
}

/** 入力部品をキャプション付きのフィールドで包む。キャプションは列見出しと重なるためCSSで隠す。 */
export function fieldWrapper(caption: string, control: HTMLElement, extraClass?: string): HTMLLabelElement {
  const wrapper = createElement('label', extraClass ? `field ${extraClass}` : 'field');
  wrapper.append(createElement('span', 'eyebrow field-caption', caption), control);
  return wrapper;
}

/** 値を編集させずに表示するラベルを作る。 */
export function statLabel(value: string): HTMLSpanElement {
  return createElement('span', 'stat-value', value);
}

/** 打率の入力欄を作る。入力中は小数点前の0を補い、確定時に表示桁をそろえる。 */
export function hitAverageInput(
  value: string,
  disabled: boolean,
  handlers: { onInput(value: string): string; onChange(value: string): string },
): HTMLInputElement {
  const input = createElement('input');
  input.type = 'number';
  input.required = true;
  input.min = String(HIT_AVERAGE_RANGE.min);
  input.max = String(HIT_AVERAGE_RANGE.max);
  input.step = '0.01';
  input.value = value;
  input.disabled = disabled;
  input.addEventListener('input', () => {
    input.value = handlers.onInput(input.value);
  });
  input.addEventListener('change', () => {
    input.value = handlers.onChange(input.value);
  });
  return input;
}

/** 性格の選択欄を作る。 */
export function personalitySelect(
  selected: Personality,
  disabled: boolean,
  onChange: (personality: Personality) => void,
): HTMLSelectElement {
  const select = createElement('select');
  select.disabled = disabled;
  for (const [value, label] of Object.entries(PERSONALITY_LABELS)) {
    const option = createElement('option', '', label);
    option.value = value;
    option.selected = selected === value;
    select.append(option);
  }
  select.addEventListener('change', () => onChange(select.value as Personality));
  return select;
}

/** 打順組み替え画面のメモ欄を作る。 */
export function memoInput(value: string, battingOrder: number, disabled: boolean, onInput: (value: string) => void): HTMLInputElement {
  const input = createElement('input', 'memo-input');
  input.type = 'text';
  input.maxLength = MEMO_MAX_LENGTH;
  input.value = value;
  input.disabled = disabled;
  input.setAttribute('aria-label', `${battingOrder}番のメモ`);
  input.addEventListener('input', () => onInput(input.value));
  return input;
}
