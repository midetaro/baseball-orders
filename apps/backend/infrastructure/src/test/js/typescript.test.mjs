import assert from 'node:assert/strict';
import { existsSync, readdirSync, readFileSync } from 'node:fs';

const resource = path => new URL(`../../main/resources/${path}`, import.meta.url);
const tsconfig = JSON.parse(readFileSync(new URL('../../../tsconfig.json', import.meta.url), 'utf8'));
const packageJson = JSON.parse(readFileSync(new URL('../../../package.json', import.meta.url), 'utf8'));

// --- Thymeleaf画面のスクリプトをTypeScriptで書く（issue #155） ---
assert.ok(!existsSync(resource('static/js')), '静的リソースに手書きのJSを置かず、TypeScriptから生成する');

const scripts = new Set();
for (const template of readdirSync(resource('templates')).filter(name => name.endsWith('.html'))) {
  const html = readFileSync(resource(`templates/${template}`), 'utf8');
  assert.doesNotMatch(html, /<script(?![^>]*\ssrc=)[^>]*>/, `${template}にインラインのスクリプトを書かない`);
  for (const [, name] of html.matchAll(/<script src="\/js\/([\w-]+)\.js"><\/script>/g)) scripts.add(name);
}
assert.deepEqual([...scripts].sort(), ['lineup-form', 'simulation', 'single-game', 'site-menu'], '画面が読み込むスクリプトを把握する');
for (const name of scripts) {
  assert.ok(existsSync(new URL(`../../main/typescript/${name}.ts`, import.meta.url)), `/js/${name}.jsはTypeScriptのソース${name}.tsから生成する`);
}

assert.equal(tsconfig.compilerOptions.strict, true, '型検査は厳格モードで行う');
assert.equal(tsconfig.compilerOptions.erasableSyntaxOnly, true, '型注釈を取り除くだけでJSになる構文に限る');
assert.equal(tsconfig.compilerOptions.noEmitOnError, true, '型エラーがあればJSを出力しない');
assert.equal(tsconfig.compilerOptions.rootDir, 'src/main/typescript', 'src/main/typescriptのソースをコンパイルする');
assert.equal(tsconfig.compilerOptions.outDir, 'build/generated/typescript/static/js', '生成したJSを静的リソースの/js配下として配信する');
assert.match(packageJson.devDependencies.typescript, /^\d+\.\d+\.\d+$/, 'TypeScriptのバージョンを固定する');

console.log('PASS: Thymeleaf画面のスクリプトをTypeScriptで書く');
