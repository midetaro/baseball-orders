import assert from 'node:assert/strict';
import { test } from 'node:test';
import { LineupForm, findLineupFormElements } from '../../../../main/typescript/lineup/lineup-form.ts';
import { readTemplate, installDom, settle, stubFetch, typeInto } from '../../support/dom.mjs';

const config = {
  readyMessage: '準備完了。',
  runningMessage: '実行中…',
  endpoint: '/simulations',
};

/** テンプレートを読み込み、打順入力フォームを開始する。成功時に描画された応答を記録する。 */
function startForm(template = 'simulation', prepare = () => {}) {
  installDom(readTemplate(template));
  prepare(document);
  const successes = [];
  const sut = new LineupForm(findLineupFormElements(), { ...config, onSuccess: data => successes.push(data) });
  sut.start();
  return { successes };
}

const $ = selector => document.querySelector(selector);
const rows = () => [...document.querySelectorAll('#order .slot')];
const hitInputs = () => [...document.querySelectorAll('#order input[type="number"]')];
const toggles = label => [...document.querySelectorAll('#order .bunt-toggle')].filter(button => button.querySelector('.toggle-label').textContent === `${label}:`);

test('打順は「{数字}番」の固定表示で9人分を表示する', () => {
  startForm();
  assert.deepEqual(rows().map(row => row.querySelector('.slot-index').textContent), ['1番', '2番', '3番', '4番', '5番', '6番', '7番', '8番', '9番']);
  assert.ok(!$('#order').textContent.includes('番打者'), '打順表示に「打者」を付けない');
});

test('打率は0.01〜0.6・小数第2位刻みの必須の数値入力にする', () => {
  startForm();
  const [first] = hitInputs();
  assert.equal(hitInputs().length, 9);
  assert.deepEqual([first.type, first.required, first.min, first.max, first.step, first.value], ['number', true, '0.01', '0.6', '0.01', '0.35']);
  assert.equal(first.closest('.field').querySelector('.field-caption').textContent, '打率', '入力欄をキャプション付きのフィールドで包む');
});

test('入力中は小数点前の0を補い、確定時に小数第2位へ整形して平均打率を更新する', () => {
  startForm();
  const [first] = hitInputs();
  typeInto(first, '.3');
  assert.equal(first.value, '0.3');
  typeInto(first, '0.3', 'change');
  assert.equal(first.value, '0.30');
  assert.equal($('#average-hit-average').textContent, ((0.3 + 0.35 * 4 + 0.33 + 0.29 + 0.28 + 0.27) / 9).toFixed(3));
});

test('範囲外の打率があれば実行できず、範囲を案内する。直せばいつでも実行できる', () => {
  startForm();
  assert.equal($('#submit').disabled, false);
  assert.equal($('#feedback').textContent, config.readyMessage);
  typeInto(hitInputs()[8], '0.61');
  assert.equal($('#submit').disabled, true);
  assert.equal($('#feedback').textContent, '打率は0.01〜0.6の範囲ですべての項目を入力してください。');
  assert.equal($('#average-hit-average').textContent, '—');
  typeInto(hitInputs()[8], '');
  assert.equal($('#submit').disabled, true, '未入力でも実行できない');
  typeInto(hitInputs()[8], '0.2');
  assert.equal($('#submit').disabled, false);
});

test('バント・盗塁の切り替えは項目名と状態を別要素にし、読み上げでは両方を伝える', () => {
  startForm();
  const [bunt] = toggles('バント');
  assert.equal(bunt.querySelector('.toggle-state').textContent, 'する');
  assert.equal(bunt.getAttribute('aria-label'), 'バント: する');
  assert.equal(bunt.getAttribute('aria-pressed'), 'true');
  bunt.click();
  const [toggled] = toggles('バント');
  assert.equal(toggled.querySelector('.toggle-state').textContent, 'しない');
  assert.equal(toggled.getAttribute('aria-label'), 'バント: しない');
  assert.equal(toggled.getAttribute('aria-pressed'), 'false');
  assert.equal(toggles('盗塁').length, 9, '盗塁の切り替えも打者ごとに表示する');
});

test('全員バント・全員盗塁は、全員有効なら全員無効にし、そうでなければ全員有効にする', () => {
  startForm();
  const pressed = label => toggles(label).map(button => button.getAttribute('aria-pressed'));
  assert.equal($('#toggle-all-bunt').getAttribute('aria-pressed'), 'false', '一部が無効なら押下状態にしない');
  $('#toggle-all-bunt').click();
  assert.deepEqual(pressed('バント'), Array(9).fill('true'));
  assert.equal($('#toggle-all-bunt').getAttribute('aria-pressed'), 'true');
  $('#toggle-all-bunt').click();
  assert.deepEqual(pressed('バント'), Array(9).fill('false'));
  $('#toggle-all-steal').click();
  assert.deepEqual(pressed('盗塁'), Array(9).fill('true'));
  assert.deepEqual(pressed('バント'), Array(9).fill('false'), '盗塁の一括切り替えはバントに影響しない');
});

test('性格を選び、性格の初期化で全員を単打マンに戻す', () => {
  startForm();
  const select = () => $('#order select');
  assert.deepEqual([...select().options].map(option => option.textContent), ['単打マン', '中距離砲', '長距離砲', '高出塁率', '盗塁重視', 'バント職人']);
  select().value = 'EAGER_BUNT';
  select().dispatchEvent(new window.Event('change'));
  $('#toggle-all-bunt').click();
  assert.equal(select().value, 'EAGER_BUNT', '再描画しても選んだ性格を保つ');
  $('#reset-all-personalities').click();
  assert.equal(select().value, 'DEFAULT');
});

test('打順をAPIへ送り、実行中は操作を止め、成功したら結果画面へ切り替える', async () => {
  const { successes } = startForm();
  const api = stubFetch();
  $('#order select').value = 'HIGH_ON_BASE';
  $('#order select').dispatchEvent(new window.Event('change'));
  $('#submit').click();

  assert.equal(api.requests.length, 1);
  const [{ url, init, body }] = api.requests;
  assert.equal(url, '/simulations');
  assert.equal(init.method, 'POST');
  assert.equal(init.headers['Content-Type'], 'application/json');
  assert.deepEqual(body[0], { hit_average: 0.35, bunt_enabled: true, steal_enabled: true, personality: 'HIGH_ON_BASE' });
  assert.equal(body.length, 9);
  assert.equal($('#feedback').textContent, config.runningMessage);
  for (const control of [$('#submit'), $('#tab-input'), $('#tab-results'), $('#edit-lineup'), $('#toggle-all-bunt'), hitInputs()[0], toggles('盗塁')[0]]) {
    assert.equal(control.disabled, true, '実行中は打順と画面の切り替えを操作できない');
  }
  $('#submit').click();
  assert.equal(api.requests.length, 1, '実行中は重ねて送信しない');

  api.respond(200, { statistics: { gameCount: 1 } });
  await settle();
  assert.deepEqual(successes, [{ statistics: { gameCount: 1 } }], '応答JSONを画面の結果描画へ渡す');
  assert.deepEqual([$('#input-view').hidden, $('#results').hidden], [true, false], '結果画面だけを表示する');
  assert.deepEqual([$('#tab-input').getAttribute('aria-selected'), $('#tab-results').getAttribute('aria-selected')], ['false', 'true']);
  assert.equal($('#tab-results').disabled, false, '結果が得られたら結果タブを押せる');
  assert.equal($('#submit').disabled, false);

  $('#edit-lineup').click();
  assert.deepEqual([$('#input-view').hidden, $('#results').hidden], [false, true], '「打順を編集する」で入力画面へ戻る');
  $('#tab-results').click();
  assert.equal($('#results').hidden, false, '入力画面から前回の結果画面へ戻れる');
  $('#tab-input').click();
  assert.equal($('#input-view').hidden, false);
});

test('APIがエラーを返したら結果を描画せず、結果タブも押せないままにする', async () => {
  const { successes } = startForm();
  const api = stubFetch();
  $('#submit').click();
  api.respond(500, { error: '失敗' });
  await settle();
  assert.deepEqual(successes, []);
  assert.deepEqual([$('#input-view').hidden, $('#results').hidden], [false, true]);
  assert.equal($('#tab-results').disabled, true);
  assert.equal($('#submit').disabled, false, '失敗後は再実行できる');
});

// --- 打順組み替え画面（issue #146） ---
/** チーム既定の打順をテンプレートへ入れる。Thymeleafが描画する data 属性と同じ形にする。 */
function addTeams(document) {
  const batter = (hitAverage, personality, stealForced = false, buntForced = false) =>
    `<span data-batter data-hit-average="${hitAverage}" data-personality="${personality}" data-steal-forced="${stealForced}" data-bunt-forced="${buntForced}"></span>`;
  const team = (key, first) => `<div data-team="${key}">${first}${batter('0.250', 'DEFAULT').repeat(8)}</div>`;
  document.querySelector('#team-defaults').innerHTML =
    team('lions', batter('0.300', 'EAGER_STEAL', true)) + team('tigers', batter('0.320', 'EAGER_BUNT', false, true));
  document.querySelector('#team-select').innerHTML = '<option value="lions" selected>ライオンズ</option><option value="tigers">タイガース</option>';
}

test('組み替えモードでは選択中のチームの既定打順を、打率・性格を編集させずに表示する', () => {
  startForm('batting-order', addTeams);
  assert.equal(hitInputs().length, 0, '打率を入力させない');
  assert.equal($('#order select'), null, '性格を選ばせない');
  const [first] = rows();
  assert.deepEqual([...first.querySelectorAll('.stat-value')].map(label => label.textContent), ['0.300', '盗塁重視']);
  assert.equal(first.querySelector('.forced-label').getAttribute('aria-label'), '盗塁: する（固定）', '固定の盗塁は切り替えられないラベルにする');
  assert.equal($('#average-hit-average').textContent, ((0.3 + 0.25 * 8) / 9).toFixed(3));
  $('#team-select').value = 'tigers';
  $('#team-select').dispatchEvent(new window.Event('change'));
  assert.equal(rows()[0].querySelector('.stat-value').textContent, '0.320', 'チームを切り替えると既定打順を読み込み直す');
  assert.equal(rows()[0].querySelector('.forced-label').getAttribute('aria-label'), 'バント: する（固定）');
});

test('組み替えモードではメモを書け、ドラッグハンドルの上下キーで打順を入れ替える', () => {
  startForm('batting-order', addTeams);
  const memo = rows()[0].querySelector('.memo-input');
  assert.deepEqual([memo.maxLength, memo.getAttribute('aria-label')], [20, '1番のメモ']);
  typeInto(memo, '俊足');
  const handle = rows()[0].querySelector('.drag-handle');
  assert.equal(handle.getAttribute('aria-label'), '1番の打者を移動（ドラッグまたは上下キー）');
  handle.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
  assert.deepEqual(rows().slice(0, 2).map(row => row.querySelector('.stat-value').textContent), ['0.250', '0.300']);
  assert.equal(rows()[1].querySelector('.memo-input').value, '俊足', 'メモは打者と一緒に移動する');
  assert.equal(document.activeElement, rows()[1].querySelector('.drag-handle'), '移動した打者のハンドルにフォーカスを移す');
  rows()[0].querySelector('.drag-handle').dispatchEvent(new window.KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true }));
  assert.equal(rows()[0].querySelector('.stat-value').textContent, '0.250', '先頭より上へは移動しない');
});

test('組み替え画面のAPI送信にはメモと固定フラグを含めない', () => {
  startForm('batting-order', addTeams);
  const api = stubFetch();
  typeInto(rows()[0].querySelector('.memo-input'), '秘密');
  $('#submit').click();
  assert.deepEqual(api.requests[0].body[0], { hit_average: 0.3, bunt_enabled: false, steal_enabled: true, personality: 'EAGER_STEAL' });
});
