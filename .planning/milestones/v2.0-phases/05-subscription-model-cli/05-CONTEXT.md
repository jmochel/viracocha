# Phase 5: Subscription model & CLI - Context

**Gathered:** 2026-04-04  
**Status:** Ready for planning

<domain>
## Phase Boundary

Introduce **subscription** entities persisted in central YAML, bound to an existing **project** and **catalog**, with CLI to add, list, show, and remove subscriptions. Each subscription records catalog-relative source path, workspace-relative path (subscription subtree), sync direction, and a stable **id** for later `vira sync` targeting (Phase 7).

This phase does **not** implement filesystem sync or `vira sync` — that is Phases 6–7.

Delivers: CFG-01, CFG-02, SUB-01 through SUB-07 per `.planning/REQUIREMENTS.md`.

</domain>

<decisions>
## Implementation Decisions

### Command surface (gray area: top-level vs under `project`)
- **D-01:** Use a **top-level** command group `vira subscription` with leaf commands `add`, `list`, `show`, `remove` — matches `.planning/ROADMAP.md` Phase 5 design notes and keeps subscription CRUD discoverable alongside `catalog` / `archetype` / `project`.
- **D-02:** `add` requires **`--project <name>`** and **`--catalog <name>`** (registered names), plus **`--source`** (path relative to catalog root), **`--workspace`** (path relative to project workspace), and **`--direction`** (see D-06). Mirrors the explicit-flag style of `project add-mapping`.

### Data model & YAML
- **D-03:** Nest subscriptions under each project: add `List<SubscriptionEntry> subscriptions` (default empty) to `ProjectEntry` — workspace anchor stays the project; no root-level `subscriptions[]` array in `ViracochaConfig`.
- **D-04:** `SubscriptionEntry` fields: **`id`** (string, unique within the whole config — see D-05), **`catalogName`**, **`sourcePath`**, **`workspacePath`**, **`direction`** (see D-06). Optional: store `name` label later — **not** in v2.0 Phase 5 unless planner adds it for UX.
- **D-05:** **`id`** is generated on `add` as a **UUID string** (e.g. Java `UUID.randomUUID().toString()`) so `show`/`remove` and Phase 7 `--subscription` stay stable if paths are edited later.

### Direction values (CLI + YAML)
- **D-06:** Three directions aligned with ROADMAP: **`CATALOG_TO_WORKSPACE`**, **`WORKSPACE_TO_CATALOG`**, **`BIDIRECTIONAL`**. YAML stores these enum names (or identical string values). CLI accepts **kebab-case** long values: `catalog-to-workspace`, `workspace-to-catalog`, `bidirectional` (picocli `enum` or custom converter).

### Output format (carry forward Phase 3)
- **D-07:** **`subscription list`**: plain text, aligned columns (id, project, catalog, direction, workspacePath summary) — same spirit as `project list` / `catalog list` per `03-CONTEXT.md` D-03.
- **D-08:** **`subscription show`**: multi-line key-value block; **`--json`** for machine-readable output — same convention as `03-CONTEXT.md` D-04/D-05.

### Validation and errors
- **D-09:** **Project** must exist; **catalog** must exist (by name in `ViracochaConfig.catalogs`). If either missing → clear message, exit 1, no save (mirror `AddMappingCommand` / Phase 3 D-07).
- **D-10:** Reject path strings containing **`..`** segments or absolute paths where relative paths are required; normalize trimming and separator style consistently with existing commands (Claude's discretion on exact normalization if tested).
- **D-11:** **Duplicate detection:** reject `add` when an existing subscription on the same project has the same **`catalogName` + `sourcePath` + `workspacePath`** (case-sensitive string compare after trim) — satisfies SUB-07 “explicit rules”.
- **D-12:** Optional existence check: if catalog root path from config does not exist on disk, **warn** to stderr but **allow** add (forward-compatible); if stricter behavior is needed, planner can tighten to fail — default here is warn-only.

### Tests
- **D-13:** JUnit 5 + `@MicronautTest` for subscription commands; `@TempDir` for isolated config — same as `03-CONTEXT.md` D-11.

### Claude's Discretion
- Exact JSON property names for `--json` (prefer **camelCase** for new fields to align with Jackson defaults).
- Package layout for `SubscriptionCommand` and `SubscriptionEntry` (e.g. `org.saltations.subscription`).
- Whether `list` is global (`vira subscription list`) or requires `--project` — **recommend** optional `--project` filter: no flag = list all subscriptions across projects; with flag = filter.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and roadmap
- `.planning/REQUIREMENTS.md` — CFG-01, CFG-02, SUB-01 through SUB-07
- `.planning/ROADMAP.md` — Phase 5 goal, design notes, success criteria
- `.planning/PROJECT.md` — v2.0 milestone scope; YAML-only config; local filesystem

### Prior phase context (carry forward)
- `.planning/phases/01-foundation/01-CONTEXT.md` — Callable exit codes, config guard, JSONL logging
- `.planning/phases/02-publishers-and-patterns/02-CONTEXT.md` — list/show/`--json`, duplicate handling
- `.planning/phases/03-projects-and-mappings/03-CONTEXT.md` — project commands, `add-mapping` validation pattern, D-03–D-05 output conventions

### Code entry points
- `src/main/java/org/saltations/model/ProjectEntry.java` — add subscriptions list
- `src/main/java/org/saltations/model/CatalogEntry.java` — catalog name/path reference
- `src/main/java/org/saltations/config/ConfigService.java` — load/save
- `src/main/java/org/saltations/ViracochaCommand.java` — register `SubscriptionCommand`
- `src/main/java/org/saltations/project/AddMappingCommand.java` — reference for load → find project → validate → mutate → save

**External specs:** No external specs — requirements are fully captured in decisions above and listed docs.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`ConfigService`** + **`ViracochaConfig`** — same load/mutate/save loop as `AddMappingCommand`.
- **`ProjectEntry`**, **`MappingEntry`** — POJO + Lombok pattern for `SubscriptionEntry`.
- **`CatalogCommand`** / **`ProjectCommand`** — picocli nested command group pattern for `SubscriptionCommand`.

### Established Patterns
- Config not initialized → `ConfigNotInitializedException` on load; message to stderr; exit 1.
- Success messages to stdout; validation errors to stderr.
- List/show commands support `--json` where applicable (Phase 2/3).

### Integration Points
- Add `SubscriptionCommand.class` to `ViracochaCommand.subcommands`.
- Extend YAML serialization: new list on `ProjectEntry`; ensure empty list omits or serializes as `[]` consistently with `mappings`.

</code_context>

<specifics>
## Specific Ideas

- ROADMAP explicitly preferred **`vira subscription`** over nesting under `project`; Phase 3 still used **`--project`** on `add-mapping` — subscription `add` should keep **`--project`** for consistency.
- Root command description in `ViracochaCommand` already mentions bidirectional sync — subscriptions make that config concrete in v2.0.

</specifics>

<deferred>
## Deferred Ideas

- **Sync engine behavior** (conflict policy, copy algorithm) — Phases 6–7, not Phase 5.
- **`vira catalog list` subscriber counts** (PUB-v2-01) — optional enhancement; not required for Phase 5 minimum.
- **Watch mode** — post–v2.0 per PROJECT.md.

**Reviewed todos:** None (todo match-phase returned 0 for phase 5).

</deferred>

---

*Phase: 05-subscription-model-cli*  
*Context gathered: 2026-04-04*
