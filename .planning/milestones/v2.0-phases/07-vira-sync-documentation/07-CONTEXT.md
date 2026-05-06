# Phase 7: `vira sync` & documentation - Context

**Gathered:** 2026-04-04  
**Status:** Ready for planning

<domain>
## Phase Boundary

Expose the existing **`DefaultSyncService`** / **`SyncService`** as a user-facing **`vira sync`** command with **`--project-name`**, optional **`--subscription`**, **`--dry-run`**, **`--verbose`**, and **`--json`**; map engine results to **stdout/stderr**, **exit codes**, and **README** updates. **Conflict resolution strategies** (overwrite / last-write-wins) are **out of scope for v2.0** — default remains **abort with structured conflicts** per Phase 6 / SYN-04.

</domain>

<decisions>
## Implementation Decisions

### CLI flags & naming
- **D-01:** Required project selector: **`--project-name <name>`** — aligns with **`vira generate`**; roadmap wording “`--project`” is the same user intent, **not** a separate flag name.
- **D-02:** Optional **`--subscription <uuid>`** — sync **only** that subscription; if omitted, sync **all** subscriptions on the project (Phase 5 stable ids).
- **D-03:** **`--dry-run`** and **`--verbose`** — same *families* as `generate` (dry-run = no mutations; verbose = per-path lines).
- **D-04:** **`--json`** — emit a **single JSON object** on **stdout** representing the full aggregated sync result (engine-shaped: per-subscription results, counts, conflict records). When **`--json`** is set, **do not** print the human one-line summary on stdout; **stderr** remains for errors and non-JSON diagnostics.

### Exit codes & output channels
- **D-05:** **Exit 0** only when the run completes with **zero conflicts** and **zero failed copies** (applies to real apply and to dry-run analysis).
- **D-06:** **Exit 1** for any conflict, copy failure, missing project, uninitialized config, I/O error, or invalid arguments — **single failure code** for v2.0 (finer granularity deferred).
- **D-07:** **Human mode:** final **summary line** on **stdout** (SYN-09 counts: copied / skipped / failed / conflicts — exact wording in implementation). **Conflict listings and error detail** go to **stderr**, not mixed into the one-line summary.
- **D-08:** **`--json` mode:** JSON document on **stdout** only; anything not part of that document uses **stderr**.

### Dry-run & verbose
- **D-09:** **`--dry-run`:** **Analyze-only** — **no** filesystem mutations; report paths that **would** be copied or skipped and **conflicts** as the engine would classify them. Conflicts → **non-zero exit**; no partial writes.
- **D-10:** **`--verbose`:** **One line per path** (or equivalent granularity) for created / updated / skipped / conflict — same spirit as `generate` verbose output.
- **D-11:** **`--dry-run` + `--verbose`:** Verbose lines describe **planned** actions **without** writing.

### Conflict strategy & README
- **D-12:** **Do not implement** `--conflict-strategy` / overwrite / last-write-wins in v2.0; **abort-only** remains. **Defer** strategy flags to a **post–v2.0** milestone; README may mention “future optional strategies.”
- **D-13:** **README:** Remove claims that subscriptions/sync are out of scope; add a **Subscriptions & sync** section: central config, **`vira subscription add`**, directions, **`vira sync`**, conflict behavior (abort + structured reporting), **local filesystem only**, **one-shot** (no watch — points to deferred WATCH-01).

### Claude's Discretion
- Exact JSON field names (must stay consistent with Java DTOs / Phase 6 enums).
- Human summary string formatting and verbose line prefixes.
- Test layout and fixture structure for X-01 integration tests.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — SYN-06 through SYN-09, X-01, X-02
- `.planning/ROADMAP.md` — Phase 7 goal, design notes, success criteria
- `.planning/PROJECT.md` — v2.0 scope, constraints (YAML, local-only, no Git)

### Prior phase context
- `.planning/phases/06-sync-engine/06-CONTEXT.md` — engine semantics, conflict kinds, atomicity per subscription, JSON-stable fields
- `.planning/phases/05-subscription-model-cli/05-CONTEXT.md` — subscription model, UUID ids, directions

### Code
- `src/main/java/org/saltations/sync/SyncService.java` — `syncProject` contract
- `src/main/java/org/saltations/sync/DefaultSyncService.java` — engine implementation
- `src/main/java/org/saltations/sync/SyncEngineResult.java`, `SyncSubscriptionResult.java`, `SyncConflictRecord.java`, `SyncConflictKind.java` — result shapes for CLI / JSON
- `src/main/java/org/saltations/generate/GenerateCommand.java` — `--project-name`, `--dry-run`, `--verbose` precedent
- `src/main/java/org/saltations/subscription/ListSubscriptionsCommand.java` — `--json` pattern (JSONL for lists; sync uses **single object** per D-04)

**External specs:** No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable assets
- **`DefaultSyncService`** / **`SyncService.syncProject(String)`** — primary integration point for the new command.
- **`GenerateCommand`** — pattern for picocli options, exit codes, verbose passthrough, dry-run.
- **Subscription / project listing commands** — `--json` conventions (sync uses one aggregate object, not JSONL).

### Established patterns
- Micronaut `@Singleton` + `@Inject` for commands; `Callable<Integer>` return.
- Config errors → stderr + exit 1 (match existing commands).

### Integration points
- New **`SyncCommand`** (or equivalent) under `org.saltations.sync` or `org.saltations` — register as subcommand of `ViracochaCommand`.
- Tests: extend patterns in `src/test/java/org/saltations/sync/` (e.g. `@TempDir` integration).

</code_context>

<specifics>
## Specific Ideas

- Prefer **CLI consistency** with `generate` over literal roadmap flag name (`--project` vs `--project-name`).
- **`--json`** should be suitable for scripting and mirrors the structured result Phase 6 prepared for “Phase 7 JSON.”

</specifics>

<deferred>
## Deferred Ideas

- **`--conflict-strategy`** (overwrite / last-write-wins) — post–v2.0 milestone
- **Finer exit codes** (e.g. separate code for conflicts vs I/O) — if needed later
- **Watch mode** — WATCH-01 / PROJECT.md Out of Scope

### Reviewed Todos (not folded)
- None (`todo match-phase` returned 0 for phase 7).

</deferred>

---

*Phase: 07-vira-sync-documentation*  
*Context gathered: 2026-04-04*
