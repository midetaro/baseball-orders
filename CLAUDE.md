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
mechanism differs.

This section is the user's standing request to use subagents: when a feature
touches two or more of simulator domain/application, backend
domain/application/messaging, backend presentation, the shared contract, or
`integration-test`, orchestrate it with these agents rather than implementing
everything in the parent context. Keep only the small, single-area changes that
`docs/codex-graph.md` "When not to use the graph" describes in the parent.

Orchestrate the graph as follows:

1. Spawn `explorer` and wait for its report. For a large or unfamiliar area,
   spawn several `explorer` agents with disjoint questions in one message.
2. Build the dependency graph and give every writing agent an explicit,
   exclusive list of writable paths in its prompt, together with the issue
   number, acceptance criteria, and the agreed interfaces it consumes.
3. Run upstream nodes (for example a shared-contract change) first. Then spawn
   all independent workers (`domain-worker`, `backend-worker`, `ui-worker`)
   with multiple `Agent` calls **in a single message**, so that they run in
   parallel. Calls issued in separate messages run one after another.
4. Subagents run in the background and notify the parent on completion. Wait
   for every notification; never assume or fabricate a worker's result. While
   waiting, the parent may do only read-only work that does not touch the
   workers' files.
5. Treat any `blocked` or `failed` report as an unfinished node. Re-run that
   node, or continue it with `SendMessage`, before running `integrator`.
6. Run `reviewer` last, with the issue, final diff, and test evidence only. The
   parent then commits, as the workers never commit.

Do not pass `isolation: "worktree"`, because `AGENTS.md` forbids additional Git
worktrees. Exclusive file ownership is what keeps parallel workers from
conflicting. `.claude/settings.json` caps concurrency at four subagents through
`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`, matching
`max_concurrent_threads_per_session = 4` in `.codex/config.toml`. Workers use the
`sonnet` model alias and the read-only `explorer` and `reviewer` use `haiku`,
mirroring the Codex split between worker and read-only models.

### Browser debugging — Claude in Chrome

`.claude/settings.json` enables Claude in Chrome by default
(`CLAUDE_CODE_ENABLE_CFC`) and pre-approves its `mcp__claude-in-chrome__*` tools,
so screen debugging and `baseball-orders-screen-review` captures need no
`--chrome` flag or per-tool approval. The Chrome extension must still be
installed and connected in your browser.

### Pull request CI polling

Follow `AGENTS.md` and run `gh pr checks <pr-number> --watch --interval 60`. In
Claude Code, run that command in the foreground with a long Bash `timeout`, or
in the background (`run_in_background`) and wait for its completion
notification. Do not poll it in a `/loop` or `ScheduleWakeup` cycle shorter than
one minute.

### Skills — same skills, `.claude/skills`

`.agents/skills/baseball-orders-development`, `baseball-orders-test`,
`baseball-orders-review`, `baseball-orders-screen-review`, and
`simulator-guide-sync` are symlinked under `.claude/skills/` so Claude Code's
`Skill` tool can discover and invoke them by the same names. Use them exactly
as `AGENTS.md` and `docs/codex-graph.md` describe.

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
