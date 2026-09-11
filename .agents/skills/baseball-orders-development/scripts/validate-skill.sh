#!/usr/bin/env bash
set -euo pipefail

skill_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
codex_root="${CODEX_HOME:-${HOME}/.codex}"
validator="${codex_root}/skills/.system/skill-creator/scripts/quick_validate.py"
venv_dir="${TMPDIR:-/tmp}/baseball-orders-skill-validator"

if [[ ! -f "${validator}" ]]; then
  echo "Codex skill validator was not found: ${validator}" >&2
  exit 1
fi

if [[ ! -x "${venv_dir}/bin/python" ]]; then
  python3 -m venv "${venv_dir}"
fi

if ! "${venv_dir}/bin/python" -c 'import yaml' >/dev/null 2>&1; then
  "${venv_dir}/bin/python" -m pip install --disable-pip-version-check PyYAML
fi

"${venv_dir}/bin/python" "${validator}" "${skill_dir}"
