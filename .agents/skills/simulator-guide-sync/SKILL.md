---
name: simulator-guide-sync
description: Update the HTML simulation guide so its player-facing explanation matches the current baseball-orders simulator behavior. Use when simulator rules or statistics change and the guide page needs synchronized documentation.
---

# Simulator Guide Sync

Keep `apps/backend/infrastructure/src/main/resources/templates/simulation-guide.html` accurate for users without turning it into a copy of implementation details.

## Establish the behavior to explain

Read the root `AGENTS.md`, `apps/simulator/AGENTS.md`, the guide template, and its controller/test before editing. Inspect the changed simulator code and the code it delegates to; do not infer rules from feature names, test names, or an older guide.

Trace each guide claim to its behavioral source. For ordinary game flow, begin with `GameBattingContext`; follow the relevant batting, bunt, steal, base-state, inning, and statistics collaborators. When input fields or displayed result metrics are described, also trace the backend request/result mapping and the page JavaScript or controller that exposes them.

Distinguish these categories while writing:

- Guaranteed behavior: describe it directly, including ordering and state changes when they matter to a user's interpretation.
- Probabilistic behavior: name the input probability or condition and state that outcomes vary; do not promise a particular result.
- Configuration or strategy behavior: describe it only if the user can select it or it is consistently active in the current production path. Otherwise omit it or clearly qualify it.

Do not document private class names, internal message transport, incidental random-number implementation, or behavior that the current production path cannot exercise. If implementation and existing page disagree, implementation is authoritative unless a feature specification explicitly says the implementation is incomplete; report that conflict rather than inventing a rule.

## Update the page and its regression coverage

Preserve the existing Japanese, player-facing tone, route (`/simulation-guide`), navigation, and page styling unless the request includes a design change. Explain the essential sequence and meaningful constraints in plain Japanese. Avoid exact percentages, thresholds, or exhaustive state tables unless they are stable user-facing rules and necessary to understand results.

Add or update the narrowest test that proves every newly documented material rule is rendered by the guide endpoint. Use the existing `SimulationPageIntegrationTest` when the rendered HTML is the observable contract. Assert distinctive Japanese phrases for concrete rules rather than broad words that could occur incidentally. Keep its required integration-test documentation accurate whenever its real components or mocks change.

Follow red-green-refactor: make the focused guide assertion fail because the new explanation is absent, then update the template and rerun it. If the requested wording cannot be verified by a test without duplicating simulation logic in the test, test the stable rendered claim and rely on the simulator's existing focused tests for the behavior itself.

## Verify

For a guide-only change, run the focused backend guide-page test and then:

```bash
./.agents/skills/baseball-orders-development/scripts/verify.sh backend
git diff --check
```

When the same task changes simulator behavior, also run its focused tests and `verify.sh simulator`. Do not claim a guide rule was validated solely by page rendering: report separately the simulator test coverage, rendered-page coverage, and any manually traced claims.
