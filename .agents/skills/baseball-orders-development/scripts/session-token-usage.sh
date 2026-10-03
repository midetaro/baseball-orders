#!/usr/bin/env bash
# Print the tokens consumed by the current coding-agent session, including its subagents.
#
# Usage:
#   session-token-usage.sh claude [session-transcript.jsonl]
#   session-token-usage.sh codex  [rollout.jsonl]
#
# Without a file argument, the newest session started from this repository is used,
# which is the running session when the command is executed from inside it.
set -euo pipefail

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi

agent="${1:-}"
session_file="${2:-}"
repo_root="$(git rev-parse --show-toplevel)"

claude_usage() {
  local project_dir transcript session_dir
  local -a transcripts
  project_dir="${CLAUDE_CONFIG_DIR:-${HOME}/.claude}/projects/$(printf '%s' "${repo_root}" | sed 's/[^A-Za-z0-9]/-/g')"
  transcript="${session_file:-$(ls -t "${project_dir}"/*.jsonl 2>/dev/null | head -n 1 || true)}"
  if [[ -z "${transcript}" || ! -f "${transcript}" ]]; then
    echo "Claude Code session transcript was not found under ${project_dir}" >&2
    exit 1
  fi

  transcripts=("${transcript}")
  session_dir="${transcript%.jsonl}"
  if [[ -d "${session_dir}/subagents" ]]; then
    while IFS= read -r file; do
      transcripts+=("${file}")
    done < <(find "${session_dir}/subagents" -name '*.jsonl' -type f)
  fi

  # A response with several content blocks is logged once per block with the same
  # usage, so usage is counted once per message id.
  jq -rs --arg session "$(basename "${session_dir}")" --argjson files "${#transcripts[@]}" '
    [.[] | select(.type == "assistant" and .message.usage != null)
         | {key: (.message.id // .requestId), usage: .message.usage}]
    | unique_by(.key)
    | reduce .[].usage as $u ({input: 0, output: 0, cache_write: 0, cache_read: 0};
        .input += ($u.input_tokens // 0)
        | .output += ($u.output_tokens // 0)
        | .cache_write += ($u.cache_creation_input_tokens // 0)
        | .cache_read += ($u.cache_read_input_tokens // 0))
    | "agent: claude-code",
      "session: \($session)",
      "transcripts: \($files)",
      "total_tokens: \(.input + .output + .cache_write + .cache_read)",
      "input_tokens: \(.input)",
      "output_tokens: \(.output)",
      "cache_creation_input_tokens: \(.cache_write)",
      "cache_read_input_tokens: \(.cache_read)"
  ' "${transcripts[@]}"
}

codex_usage() {
  local sessions_dir rollout root_session file
  local -a rollouts
  sessions_dir="${CODEX_HOME:-${HOME}/.codex}/sessions"
  rollout="${session_file}"
  if [[ -z "${rollout}" ]]; then
    while IFS= read -r file; do
      if [[ "$(head -n 1 "${file}" | jq -r '.payload.cwd // empty')" == "${repo_root}" ]]; then
        rollout="${file}"
        break
      fi
    done < <(find "${sessions_dir}" -name 'rollout-*.jsonl' -type f -print0 | xargs -0 ls -t 2>/dev/null)
  fi
  if [[ -z "${rollout}" || ! -f "${rollout}" ]]; then
    echo "Codex rollout for ${repo_root} was not found under ${sessions_dir}" >&2
    exit 1
  fi

  # Subagent threads are separate rollouts that share the root thread's session_id.
  root_session="$(head -n 1 "${rollout}" | jq -r '.payload.session_id // .payload.id')"
  rollouts=()
  while IFS= read -r file; do
    if [[ "$(head -n 1 "${file}" | jq -r '.payload.session_id // .payload.id // empty')" == "${root_session}" ]]; then
      rollouts+=("${file}")
    fi
  done < <(find "${sessions_dir}" -name 'rollout-*.jsonl' -type f)
  if [[ ${#rollouts[@]} -eq 0 ]]; then
    rollouts=("${rollout}")
  fi

  # total_token_usage is cumulative per thread, so the last value of each thread is summed.
  for file in "${rollouts[@]}"; do
    jq -c 'select(.payload.type == "token_count" and .payload.info != null) | .payload.info.total_token_usage' "${file}" | tail -n 1
  done | jq -rs --arg session "${root_session}" --argjson files "${#rollouts[@]}" '
    reduce .[] as $u ({total: 0, input: 0, cached: 0, output: 0, reasoning: 0};
      .total += ($u.total_tokens // 0)
      | .input += ($u.input_tokens // 0)
      | .cached += ($u.cached_input_tokens // 0)
      | .output += ($u.output_tokens // 0)
      | .reasoning += ($u.reasoning_output_tokens // 0))
    | "agent: codex",
      "session: \($session)",
      "transcripts: \($files)",
      "total_tokens: \(.total)",
      "input_tokens: \(.input)",
      "cached_input_tokens: \(.cached)",
      "output_tokens: \(.output)",
      "reasoning_output_tokens: \(.reasoning)"
  '
}

case "${agent}" in
  claude) claude_usage ;;
  codex) codex_usage ;;
  *)
    echo "Usage: $0 <claude|codex> [session-file]" >&2
    exit 2
    ;;
esac
