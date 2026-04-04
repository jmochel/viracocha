# Phase 6: Sync engine - Context

**Gathered:** 2026-04-04  
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement a **filesystem sync engine** as a **testable service** (no user-facing `vira sync` command — Phase 7). For each subscription, resolve absolute paths (`publisher.root / sourcePath`, `project.workspace / destinationPath`), walk the subscribed subtrees, **copy** files according to **direction**, and **detect conflicts** when both sides have a path but content or file-kind disagree. Default behavior on conflict: **abort** with a **structured list** (SYN-04, ROADMAP). **Hidden** segments use the same rule as `generate`: skip paths where `PatternPathUtils.hasHiddenPathSegment` applies to the walk roots (SYN-05, parity with `GeneratorService`).

Out of scope for Phase 6: CLI flags, `--dry-run` / `--verbose` user output, README (Phase 7); **optional conflict-strategy flags** reserved for Phase 7 if implemented.

</domain>

<decisions>
## Implementation Decisions

### 1. Conflict detection (gray area 1)
- **D-01:** **Regular files:** Two sides **differ** when both exist, both are regular files (`Files.isRegularFile`), and byte content is not identical (use `Files.mismatch` or equivalent; UTF-8 text is not special-cased).
- **D-02:** **Type mismatch:** If one side has a **regular file** and the other has a **directory** at the same relative path → **conflict** (do not delete or merge automatically).
- **D-03:** **Symbolic links:** Do **not** follow symlinks for sync. If either side’s path is a symbolic link (`Files.isSymbolicLink`) → treat as **conflict** (or a dedicated **blocked** / **unsupported** outcome in the result list) so behavior is explicit in tests and docs — not silent data loss from following links.
- **D-04:** **One side missing:** Not a conflict — it drives a **copy** in the allowed direction(s) for that subscription (SYN-01 / SYN-02). No “delete the extra side” in Phase 6 (see area 2).

### 2. Tree semantics (gray area 2)
- **D-05:** **Copy-only:** Phase 6 does **not** delete files on the destination when the source tree omits a path (no mirror-delete). Aligns with “copy files” wording and avoids surprising data loss before Phase 7 documents UX.
- **D-06:** **Directories:** Create parent directories as needed when copying a file. **Empty directories** are not materialized as first-class sync targets (no copying of empty-dir-only nodes); if a file copy implies parents, those directories appear as a side effect.
- **D-07:** **Ordering of copies** within a direction: deterministic (e.g. **lexicographic** order of normalized relative path strings, `/` separators) so tests are stable.

### 3. Bidirectional ordering (gray area 3)
- **D-08:** **Two-phase algorithm for bidirectional subscriptions:** (1) **Analyze** — walk both trees (respecting hidden skip), build the set of relative paths to consider, detect **conflicts** (per D-01–D-03). If any conflict → **abort** entire subscription (or entire engine run — planner chooses granularity; default recommendation: collect all conflicts for all subscriptions in one result, no partial writes after conflict — see D-12). (2) **Apply** — only if **zero** conflicts for that subscription, perform copies: first all **publisher → workspace** needed operations, then all **workspace → publisher** needed operations (both in deterministic path order). Rationale: keeps “publish first” intuitive for tooling; planner may adjust if tests show a cleaner split as long as order is **documented and fixed**.
- **D-09:** **One-way directions:** Single pass in the allowed direction only; same deterministic path order.

### 4. Result / conflict API (gray area 4)
- **D-10:** Return a **structured result** object (names TBD) containing at minimum: **per-subscription** identity (`subscriptionId` string), **counts** (copied / skipped / failed / conflicts or blocked), and a **list of conflict (or blocked) records** with: `relativePath` (POSIX-style string relative to the subscription’s paired roots), **kind** enum (e.g. `CONTENT_MISMATCH`, `TYPE_MISMATCH`, `SYMLINK_UNSUPPORTED`), and optional **message** for logs / Phase 7 `--verbose`.
- **D-11:** **Stable for Phase 7:** Field names and enum values should be suitable for JSON serialization later (camelCase in Java, consistent with existing commands) — exact JSON shape remains Phase 7; Phase 6 defines the **semantic** contract.
- **D-12:** **Atomicity:** If conflicts exist for a subscription, **no** filesystem mutations for that subscription’s apply phase (analyze-only outcome). Planner defines whether multiple subscriptions are “all-or-nothing” or “best effort per subscription”; **recommend** best-effort per subscription with a single aggregated result listing each subscription’s outcome.

### Claude's Discretion
- Exact class names (`SyncService`, `SyncEngine`, `DefaultSyncService`, etc.) and package (`org.saltations.sync` suggested).
- Whether `Files.walkFileTree` vs `Files.walk` + filters; internal helpers for hidden filtering reusing `PatternPathUtils`.
- Exact exception vs result type for “config load” errors — follow existing `ConfigService` patterns.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and roadmap
- `.planning/REQUIREMENTS.md` — SYN-01 through SYN-05 (sync engine scope)
- `.planning/ROADMAP.md` — Phase 6 goal, design notes, conflict default **abort**, hidden parity with `GeneratorService`
- `.planning/PROJECT.md` — local filesystem only; YAML config; no network/Git

### Prior phase context
- `.planning/phases/05-subscription-model-cli/05-CONTEXT.md` — `SubscriptionEntry`, directions, nested under `ProjectEntry`, validation patterns
- `.planning/phases/04-workspace-generation/04-CONTEXT.md` — workspace-relative paths, deterministic ordering preference

### Code entry points
- `src/main/java/org/saltations/pattern/PatternPathUtils.java` — hidden segment rule (reuse for sync walks)
- `src/main/java/org/saltations/generate/GeneratorService.java` — reference for hidden filtering pattern
- `src/main/java/org/saltations/model/SubscriptionEntry.java` — fields and direction enum
- `src/main/java/org/saltations/model/ProjectEntry.java`, `PublisherEntry.java` — path resolution inputs
- `src/main/java/org/saltations/config/ConfigService.java` — loading config for service tests

**External specs:** No external specs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable assets
- **`PatternPathUtils.hasHiddenPathSegment(root, path)`** — same hidden rule as pattern walk / generate; use for both publisher-root and workspace-root walks from subscription `sourcePath` / `destinationPath` roots.
- **`GeneratorService`** — example of `Files.walk` + regular-file filter + hidden filter; sync should mirror that filter philosophy for SYN-05.
- **`SubscriptionEntry`**, **`ProjectEntry`**, **`PublisherEntry`**, **`ConfigService`** — model and loading for integration tests.

### Established patterns
- JUnit 5 + `@TempDir` for isolated trees; Micronaut `@Singleton` services with `@Inject`.

### Integration points
- Phase 7: `vira sync` will call this service, map result to stdout summary and exit codes.

</code_context>

<specifics>
## Specific Ideas

- User requested discussion coverage for gray areas **1–4** (conflict detection, tree semantics, bidirectional ordering, result API) in one pass; decisions above consolidate those topics.

</specifics>

<deferred>
## Deferred Ideas

- **Conflict strategy flags** (overwrite / last-write-wins) — Phase 7 / optional Phase 6 tail per ROADMAP, not required for minimum Phase 6.
- **Delete / mirror** semantics — post–v2.0 or later phase unless requirements change.
- **Symlink replication** — deferred; current decision is explicit non-follow / block.

### Reviewed Todos (not folded)
- None (`todo match-phase` returned 0 for phase 6).

</deferred>

---

*Phase: 06-sync-engine*  
*Context gathered: 2026-04-04*
