import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const loginHtml = readFileSync(new URL('../../main/resources/templates/login.html', import.meta.url), 'utf8');

assert.ok(loginHtml.includes('Googleでログイン'), 'ログイン画面にGoogleログイン操作を表示する');
assert.ok(loginHtml.includes('href="/oauth2/authorization/google"'), 'ログイン画面からGoogle認可へ遷移する');
assert.ok(loginHtml.includes('th:if="${googleOauthEnabled}"'), 'Google OAuth未設定時はログイン操作を隠す');
assert.ok(loginHtml.includes('Googleログインは現在利用できません'), 'Google OAuth未設定時の案内を表示する');
assert.ok(loginHtml.includes('th:action="@{/login}"'), 'ログイン画面からローカル認証を送信できる');
assert.ok(loginHtml.includes('name="userId"'), 'ローカルログインIDを入力できる');
assert.ok(loginHtml.includes('name="password"'), 'ローカルログインパスワードを入力できる');
assert.ok(loginHtml.includes('th:action="@{/register}"'), 'ローカルアカウントを登録できる');
assert.ok(loginHtml.includes('href="/"'), 'ログイン画面からトップへ戻れる');
