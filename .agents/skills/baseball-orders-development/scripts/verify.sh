#!/usr/bin/env bash
set -euo pipefail

skill_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
repo_dir="$(cd "${skill_dir}/../../.." && pwd)"
target="${1:-all}"

verify_backend() {
  (cd "${repo_dir}/apps/backend" && ./gradlew --no-daemon --console=plain test build </dev/null)
}

verify_simulator() {
  (cd "${repo_dir}/apps/simulator" && \
    ./gradlew --no-daemon --console=plain test build \
      :infrastructure:jacocoTestCoverageVerification </dev/null)
}

verify_terraform() {
  if ! command -v terraform >/dev/null 2>&1; then
    echo "terraform CLI is required for Terraform verification" >&2
    return 1
  fi
  terraform -chdir="${repo_dir}/infra/aws-terraform" fmt -check
  terraform -chdir="${repo_dir}/infra/aws-terraform" init -backend=false -input=false
  terraform -chdir="${repo_dir}/infra/aws-terraform" validate
  terraform -chdir="${repo_dir}/infra/aws-terraform" test
}

case "${target}" in
  backend)
    verify_backend
    ;;
  simulator)
    verify_simulator
    ;;
  all)
    verify_backend
    verify_simulator
    ;;
  terraform)
    verify_terraform
    ;;
  *)
    echo "usage: $0 <backend|simulator|all|terraform>" >&2
    exit 2
    ;;
esac

git -C "${repo_dir}" diff --check
