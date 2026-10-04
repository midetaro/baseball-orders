// 打順組み替え画面のドラッグハンドル。ポインター操作（マウス・タッチ）と上下キーで打者を入れ替える。
import { createElement } from '../shared/dom.ts';

/** ドラッグハンドルが打順へ問い合わせる操作。 */
export interface ReorderTarget {
  /** 打順の各行（.slot、data-index付き）を子に持つ要素。 */
  readonly container: HTMLElement;
  /** 実行中など、打順を動かせない間は true を返す。 */
  isLocked(): boolean;
  /** 打者を移動して再描画する。移動しなければ false を返す。 */
  move(from: number, to: number): boolean;
}

const KEY_OFFSETS: Readonly<Record<string, number>> = { ArrowUp: -1, ArrowDown: 1 };

/** 打順の行ごとのドラッグハンドルを作る。ドラッグ中の打順は全ハンドルで共有する。 */
export class DragReorder {
  private readonly target: ReorderTarget;
  private dragIndex: number | null = null;

  constructor(target: ReorderTarget) {
    this.target = target;
  }

  /** index番目の行のドラッグハンドルを作る。 */
  handle(row: HTMLElement, index: number): HTMLButtonElement {
    const handle = createElement('button', 'drag-handle', '⠿');
    handle.type = 'button';
    handle.disabled = this.target.isLocked();
    handle.setAttribute('aria-label', `${index + 1}番の打者を移動（ドラッグまたは上下キー）`);
    handle.addEventListener('pointerdown', event => {
      if (this.target.isLocked()) return;
      event.preventDefault();
      this.dragIndex = index;
      handle.setPointerCapture(event.pointerId);
      row.classList.add('dragging');
    });
    handle.addEventListener('pointermove', event => {
      if (this.dragIndex === null) return;
      this.clearDropTarget();
      const target = this.slotIndexAt(event.clientX, event.clientY);
      if (target !== null && target !== this.dragIndex) {
        this.target.container.children[target].classList.add('drop-target');
      }
    });
    handle.addEventListener('pointerup', event => {
      if (this.dragIndex === null) return;
      const from = this.dragIndex;
      this.dragIndex = null;
      const target = this.slotIndexAt(event.clientX, event.clientY);
      if (target === null || !this.target.move(from, target)) {
        this.clearDragState();
      }
    });
    handle.addEventListener('pointercancel', () => {
      this.dragIndex = null;
      this.clearDragState();
    });
    handle.addEventListener('keydown', event => {
      const offset = KEY_OFFSETS[event.key];
      if (offset === undefined || this.target.isLocked()) return;
      event.preventDefault();
      if (this.target.move(index, index + offset)) {
        this.target.container.children[index + offset].querySelector<HTMLButtonElement>('.drag-handle')?.focus();
      }
    });
    return handle;
  }

  private slotIndexAt(x: number, y: number): number | null {
    const slot = document.elementFromPoint(x, y)?.closest<HTMLElement>('.slot');
    return slot && this.target.container.contains(slot) ? Number(slot.dataset.index) : null;
  }

  private clearDropTarget(): void {
    this.target.container.querySelectorAll('.drop-target').forEach(slot => slot.classList.remove('drop-target'));
  }

  private clearDragState(): void {
    this.target.container.querySelectorAll('.dragging, .drop-target').forEach(slot => slot.classList.remove('dragging', 'drop-target'));
  }
}
