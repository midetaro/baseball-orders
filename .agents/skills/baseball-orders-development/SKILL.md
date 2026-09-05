---
name: baseball-orders-development
description: Implement and verify Java, SQS messaging-contract, cross-application, and Terraform changes in the baseball-orders repository. Use for changes under apps, libs/messaging-contract, or infra/aws-terraform; do not use for prose-only questions.
---

# Baseball Orders Development

Start by reading the root and nearest nested `AGENTS.md`, then inspect the worktree and the affected boundaries.

## Route the change

- Backend-only: work from `apps/backend` and verify `backend`.
- Simulator-only: work from `apps/simulator` and verify `simulator`.
- SQS wire-format or queue behavior: treat `libs/messaging-contract` as authoritative, inspect both adapters, and verify `all`.
- Terraform: edit native HCL directly and verify `terraform`; never reintroduce a generator.

## Execute

Use red-green-refactor. A valid red run must reach the relevant test and fail because behavior is missing. If Gradle, credentials, Docker, or sandbox setup fails first, fix or report that execution problem separately.

For SQS changes, trace the complete path before editing:

```text
backend persistence/domain
  -> SimulationRequestMessage
  -> SqsSimulationScheduler
  -> SimulationResultMessage
  -> backend listener/registry
```

Keep queue names configurable and use unique queues in integration tests. Preserve request messages when deserialization, simulation, serialization, or result sending fails.

## Verify

Run `scripts/verify.sh <backend|simulator|all|terraform>`. Report conditional integration tests as skipped unless their endpoint environment variable was present. Finish with `git diff --check` and review the complete diff for accidental generated or IDE files.

When this skill is created or changed, run `scripts/validate-skill.sh`. It provisions
PyYAML in an isolated temporary virtual environment when necessary and runs the
official Codex `skill-creator/scripts/quick_validate.py`; do not substitute a
hand-written YAML or shell-only validation.
