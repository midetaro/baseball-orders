@AGENTS.md

## Claude Code specifics

This repository's working agreement above (`AGENTS.md`) was written for Codex CLI.
The rules, module boundaries, test-driven workflow, and completion criteria apply
identically to Claude Code. The items below are the Claude Code equivalents of the
Codex-specific mechanics referenced in `AGENTS.md`.

### Feature graph workflow — use `.claude/agents`, not `.codex/agents`

The six roles described in "Feature graph workflow" and `docs/codex-graph.md`
exist as Claude Code subagents under `.claude/agents/`: `explorer`,
`domain-worker`, `backend-worker`, `ui-worker`, `integrator`, `reviewer`. Invoke
them with the `Agent` tool (`subagent_type: "<name>"`) instead of Codex's agent
invocation. The graph shape, file-ownership rules, concurrency rules, and worker
loop/report format in `docs/codex-graph.md` are unchanged — only the invocation
mechanism differs. Prefer `isolation: "worktree"` for a worker whose changes
should be reviewable as an isolated diff before merging back.

### Skills — same skills, `.claude/skills`

`.agents/skills/baseball-orders-development`, `baseball-orders-test`,
`baseball-orders-review`, and `simulator-guide-sync` are symlinked under
`.claude/skills/` so Claude Code's `Skill` tool can discover and invoke them by
the same names. Use them exactly as `AGENTS.md` and `docs/codex-graph.md`
describe.

### Permissions

`.claude/settings.json` grants Claude Code the same scope Codex has in
`.codex/config.toml`: unrestricted read/write inside the repository, write
access to `~/.gradle`, and network access (`WebFetch`/`WebSearch`), with file
edits auto-accepted so routine implementation work does not stop for
confirmation. A shared, committed settings file cannot grant Claude Code's
equivalent of Codex's `approval_policy = "never"` (Claude Code's fully
prompt-free `bypassPermissions` mode is a personal, session-level opt-in, not
something a repo can silently turn on for everyone who clones it). If you
personally want that exact behavior, opt in yourself, either by launching with
`claude --dangerously-skip-permissions`, or by adding
`{"permissions": {"defaultMode": "bypassPermissions"}}` to your own
`.claude/settings.local.json` (already git-ignored).

### Session boundary

Where `AGENTS.md` recommends `/exit && codex` for a fresh session between
unrelated tasks, use `/clear` (or start a new terminal session) in Claude Code
instead.
