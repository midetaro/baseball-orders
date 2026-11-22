---
name: baseball-orders-development
description: Implement and verify Java, gRPC messaging-contract, cross-application, and Terraform changes in the baseball-orders repository. Use for changes under apps, libs/messaging-contract, or infra/aws-terraform; do not use for prose-only questions.
---

# Baseball Orders Development

Start by reading the root and nearest nested `AGENTS.md`, then inspect the worktree and the affected boundaries.

## Route the change

- Backend-only: work from `apps/backend` and verify `backend`.
- Simulator-only: work from `apps/simulator` and verify `simulator`.
- gRPC wire-format or call behavior: treat `libs/messaging-contract/src/main/proto/simulation.proto` as authoritative, inspect both adapters, and verify `all`.
- Terraform: edit native HCL directly and verify `terraform`; never reintroduce a generator.

## Execute

Use red-green-refactor. A valid red run must reach the relevant test and fail because behavior is missing. If Gradle, credentials, Docker, or sandbox setup fails first, fix or report that execution problem separately.

For gRPC changes, trace the complete path before editing:

```text
backend persistence/domain
  -> SimulationCoordinator -> SimulatorClient
  -> GrpcSimulatorClient -> SimulationRequestMessage
  -> GrpcSimulationService
  -> SimulationResultMessage
  -> GrpcSimulatorClient (synchronous return)
```

Keep the simulator address (`SIMULATOR_GRPC_HOST`/`SIMULATOR_GRPC_PORT`) and listen port (`SIMULATION_GRPC_PORT`) configurable, and use ephemeral ports in integration tests. Map failures to gRPC status codes (`INVALID_ARGUMENT` for invalid input, `INTERNAL` otherwise) and keep the backend deadline error mapping (HTTP 504 on timeout, 502 on other failures).

## Verify

Run `scripts/verify.sh <backend|simulator|all|terraform>`. Report any conditionally skipped integration test explicitly. Finish with `git diff --check` and review the complete diff for accidental generated or IDE files.

Before opening or updating a pull request, run `scripts/session-token-usage.sh <codex|claude>`
and record its `total_tokens` in the pull request title and its full output in a
`## Token usage` section of the description, as the root `AGENTS.md` requires.

When this skill is created or changed, run `scripts/validate-skill.sh`. It provisions
PyYAML in an isolated temporary virtual environment when necessary and runs the
official Codex `skill-creator/scripts/quick_validate.py`; do not substitute a
hand-written YAML or shell-only validation.
