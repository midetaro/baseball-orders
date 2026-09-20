#!/usr/bin/env bash

set -euo pipefail

if ! command -v gum >/dev/null 2>&1; then
    echo "gumがインストールされていません。"
    echo "macOS: brew install gum"
    exit 1
fi

# Gitリポジトリのルートを取得する
if project_root=$(git rev-parse --show-toplevel 2>/dev/null); then
    :
else
    project_root="$(cd "$(dirname "$0")" && pwd)"
fi

# 1. 対象アプリを選択する
application=$(gum choose \
    --header "対象アプリを選択してください" \
    "simulator" \
    "backend" \
    "web")

if [[ -z "$application" ]]; then
    exit 0
fi

# アプリごとの保存先を設定する
case "$application" in
    simulator)
        docs_dir="$project_root/apps/simulator/docs"
        ;;
    backend)
        docs_dir="$project_root/apps/backend/docs"
        ;;
    web)
        docs_dir="$project_root/apps/web/docs"
        ;;
    *)
        echo "未対応のアプリです: $application"
        exit 1
        ;;
esac

# 2. メモの分類を選択する
category=$(gum choose \
    --header "設計メモの分類を選択してください" \
    "plan" \
    "feature" \
    "refactor")

if [[ -z "$category" ]]; then
    exit 0
fi

mkdir -p "$docs_dir"

today=$(date '+%Y-%m-%d')
current_time=$(date '+%H:%M')

# 分類ごとのファイル名を決定する
case "$category" in
    feature)
        target_file="$docs_dir/feature-$today.md"
        document_title="$application Feature - $today"
        ;;
    refactor)
        target_file="$docs_dir/refactor.md"
        document_title="$application Refactoring"
        ;;
    plan)
        target_file="$docs_dir/plan.md"
        document_title="$application Plan"
        ;;
    *)
        echo "未対応の分類です: $category"
        exit 1
        ;;
esac

# 3. タイトルを入力する
title=$(gum input \
    --header "項目のタイトルを入力してください" \
    --placeholder "例: シミュレーション履歴を保存する")

if [[ -z "${title//[[:space:]]/}" ]]; then
    echo "タイトルが空なので中止しました。"
    exit 0
fi

# 4. 本文を入力する
body=$(gum write \
    --header "内容を入力してください" \
    --placeholder "目的、仕様、未決事項などを入力してください" \
    --width 100 \
    --height 15)

# ファイルが存在しない場合だけ見出しを作成する
if [[ ! -f "$target_file" ]]; then
    printf '# %s\n' "$document_title" > "$target_file"
fi

# ファイル末尾に追記する
{
    printf '\n'
    printf '## %s\n' "$title"
    printf '\n'
    printf -- '- Recorded: %s %s\n' "$today" "$current_time"

    if [[ -n "${body//[[:space:]]/}" ]]; then
        printf '\n%s\n' "$body"
    fi
} >> "$target_file"

relative_path="${target_file#"$project_root"/}"

gum style \
    --foreground 42 \
    "追記しました: $relative_path"