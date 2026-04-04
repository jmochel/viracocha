# Phase 4: Workspace Generation - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement `vira generate` so a user can materialize all mappings for a registered **project** into that project’s workspace directory: walk each mapping’s **pattern** tree, expand **Freemarker** in file bodies and in **path segments** (files and folders), merge **project-level** and **mapping-level** parameter maps (mapping wins on key clash), create missing directories, **never overwrite** existing files (skip-existing), and report a **summary** line on stdout; optional **dry-run** (no writes) and **verbose** (per-file action lines). Matches `.planning/REQUIREMENTS.md` GEN-01 through GEN-08 and ROADMAP Phase 4 success criteria.

Out of scope: subscriptions/sync, `--force` overwrite, network/Git — per PROJECT.md / v2.

</domain>

<decisions>
## Implementation Decisions

### Command surface and flags
- **D-01:** Root command: `vira generate --project-name <name>` (GEN-01). Options: `--dry-run` (GEN-07), `--verbose` (GEN-08). All commands remain `Callable<Integer>` with exit `0` = success, `1` = usage/config/business error — consistent with `01-CONTEXT.md` / Phase 1.
- **D-02:** Config guard: require initialized config (same pattern as other mutating/read commands via `ConfigService.load()`).

### Freemarker data model (GEN-02)
- **D-03:** Build a single `Map<String, Object>` (or string-only map if sufficient) per mapping invocation: start from **project-level** default parameters, then overlay **mapping-level** `MappingEntry.parameters`; mapping keys override project keys on collision.
- **D-04:** **Schema:** Add optional `Map<String, String> parameters` (defaults) to `ProjectEntry` if not already present — PROJECT.md calls for project-level defaults; YAML field name should match existing style (camelCase in Java / snake-friendly in YAML as elsewhere). Planner must migrate any existing configs without the field (empty map).

### Path and content expansion (GEN-03, GEN-04)
- **D-05:** Introduce a dedicated **PathExpander** (name flexible) that expands `${...}` / Freemarker expressions in **relative path segments** (each mapping’s destination root + pattern-relative path) using the merged model — Freemarker does not do this by itself; STATE.md flags this as a prerequisite. Unit-test PathExpander **before** wiring full generation.
- **D-06:** File **content** expansion uses Freemarker templates as today’s patterns imply; encoding UTF-8 unless Claude’s discretion picks a standard default.

### Skip-existing and directories (GEN-04, GEN-05)
- **D-07:** If target file already exists → **skip** (no overwrite, no failure); count toward **Skipped** in summary (ROADMAP #3). Create parent directories as needed before writing (GEN-04).
- **D-08:** If a **directory** path is “occupied” by a file (or vice versa) → treat as **Failed** with clear stderr message; count toward **Failed**.

### User-visible output (GEN-06, GEN-08, ROADMAP)
- **D-09:** Always print one **summary** line to stdout: `Generated: N files, Skipped: M files, Failed: K files` (exact wording per GEN-06).
- **D-10:** **`--verbose`:** one line per file action: prefix `Created` / `Skipped` / `Failed` plus path (project-relative or workspace-relative — pick one in implementation and document in tests; **recommended:** path under workspace root).
- **D-11:** **`--dry-run`:** same logical actions as live run but **no** filesystem mutations; still emit summary (and verbose lines if `--verbose`). Planner should define whether dry-run prints paths only or mirrors verbose format — **default:** print planned **Created** lines without writing.

### Logging (LOG-01 / LOG-02)
- **D-12:** User-facing progress remains on **stdout**; structured JSONL events for generation may be added if low-cost, but must not break LOG-02 (no log spam on stderr for normal operation).

### Tests
- **D-13:** JUnit 5 + `@MicronautTest` for command integration; `@TempDir` for workspaces and config; dedicated tests for PathExpander and skip-existing/dry-run behavior.

### Claude's Discretion
- Package layout for `GenerateCommand`, `GeneratorService`, `PathExpander`.
- Freemarker `Configuration` details (template loading from pattern path, exception wrapping).
- Whether dry-run simulates directory creation in output only vs full dry walk edge cases.
- Exact JSONL event shapes if generation events are logged.

</decisions>

<specifics>
## Specific Ideas

- Prior **Accumulated Context** in STATE.md: build and test **PathExpander** in isolation first — still the right sequencing.
- Generation should feel predictable in CI: deterministic ordering of processed files appreciated (e.g., sorted paths) for testability — not a hard requirement unless tests need it.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and roadmap
- `.planning/REQUIREMENTS.md` — GEN-01 through GEN-08 (authoritative acceptance text)
- `.planning/ROADMAP.md` — Phase 4 goal, success criteria, dependencies
- `.planning/PROJECT.md` — merge semantics (project vs mapping params), skip-existing product principle

### Prior phase context
- `.planning/phases/01-foundation/01-CONTEXT.md` — exit codes, Callable pattern, config tests
- `.planning/phases/02-publishers-and-patterns/02-CONTEXT.md` — Freemarker extraction, pattern roots
- `.planning/phases/03-projects-and-mappings/03-CONTEXT.md` — `ProjectEntry` / `MappingEntry`, list/show conventions

### Codebase maps
- `.planning/codebase/STACK.md` — Freemarker, Micronaut, picocli versions
- `.planning/codebase/ARCHITECTURE.md` — CLI / DI boundaries
- `.planning/codebase/CONVENTIONS.md` — naming and test layout

### Implementation entry points (scout)
- `src/main/java/org/saltations/model/ProjectEntry.java` — add project-level default parameters for GEN-02
- `src/main/java/org/saltations/model/MappingEntry.java` — mapping-level parameters
- `src/main/java/org/saltations/pattern/FreemarkerVariableExtractor.java` — pattern tree semantics
- `src/main/java/org/saltations/config/ConfigService.java` — load/save
- `src/main/java/org/saltations/ViracochaCommand.java` — register generate subcommand

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable assets
- `FreemarkerVariableExtractor` — pattern tree walk; generation will walk similarly but emit output files.
- `ConfigService`, `ViracochaConfig`, typed `ProjectEntry` / `MappingEntry` / `PatternEntry`.
- Picocli command groups + `Callable<Integer>` + `CommandSpec` stdout patterns from Phases 1–3.

### Established patterns
- Config-not-initialized and load-before-mutate guards.
- Plain stdout for user tables; `--json` on list/show commands in prior phases (generate focuses on summary lines per GEN-06).
- Skip-existing is a **core product constraint** in PROJECT.md.

### Integration points
- New `GenerateCommand` (or equivalent) under `ViracochaCommand` subcommands.
- Generator service depends on `ConfigService` + filesystem + Freemarker.

</code_context>

<deferred>
## Deferred Ideas

- **`--force` overwrite** — v2 (UX-03 in REQUIREMENTS v2)
- **Publisher sync / subscriptions** — v2
- **GraalVM native image** — out of scope

</deferred>

---

*Phase: 04-workspace-generation*
*Context gathered: 2026-04-04*
