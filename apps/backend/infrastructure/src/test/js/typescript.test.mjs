import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

const tsconfig = JSON.parse(readFileSync(new URL('../../../tsconfig.json', import.meta.url), 'utf8'));
const packageJson = JSON.parse(readFileSync(new URL('../../../package.json', import.meta.url), 'utf8'));
const options = tsconfig.compilerOptions;

// --- Thymeleaf画面のスクリプトをTypeScriptで書く（issue #155） ---
assert.ok(!existsSync(new URL('../../main/resources/static/js', import.meta.url)), '静的リソースに手書きのJSを置かず、TypeScriptから生成する');

assert.equal(options.strict, true, '型検査は厳格モードで行う');
assert.equal(options.erasableSyntaxOnly, true, '型注釈を取り除くだけでJSになる構文に限り、Nodeのテストが.tsをそのまま実行できるようにする');
assert.equal(options.verbatimModuleSyntax, true, '型だけのimportを明示し、型を取り除いた結果をtscの出力と同じにする');
assert.equal(options.rewriteRelativeImportExtensions, true, 'importの.tsを、配信する.jsへ書き換えて出力する');
assert.equal(options.module, 'es2022', 'ブラウザがES moduleとして読み込めるように出力する');
assert.equal(options.noEmitOnError, true, '型エラーがあればJSを出力しない');
assert.equal(options.rootDir, 'src/main/typescript', 'src/main/typescriptのソースをコンパイルする');
assert.equal(options.outDir, 'build/generated/typescript/static/js', '生成したJSを静的リソースの/js配下として配信する');
for (const [name, version] of Object.entries(packageJson.devDependencies)) {
  assert.match(version, /^\d+\.\d+\.\d+$/, `${name}のバージョンを固定する`);
}

console.log('PASS: Thymeleaf画面のスクリプトをTypeScriptで書く');
