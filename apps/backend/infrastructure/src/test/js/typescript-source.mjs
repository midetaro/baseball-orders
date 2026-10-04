import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';

// 画面スクリプトはTypeScriptで書く。文字列の検査は型付きのソースに対して行い、
// 関数を実行して検査するときは型注釈だけを取り除いたJSにする（tsconfigのerasableSyntaxOnlyにより、取り除いた結果はtscの出力と同じ動作になる）。
export const readScript = name => readFileSync(new URL(`../../main/typescript/${name}.ts`, import.meta.url), 'utf8');
// 抜き出した関数に続けて「return 関数名;」を書けるよう、関数本体として型を取り除く。型の除去は空白への置き換えで位置を変えない。
const prefix = 'function script() {\n', suffix = '\n}';
export const toJs = source => {
  const stripped = stripTypeScriptTypes(`${prefix}${source}${suffix}`);
  return stripped.slice(prefix.length, stripped.length - suffix.length);
};
