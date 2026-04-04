# Phase 3: Projects and Mappings - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

CLI commands to create, list, show, and unregister **projects** (named workspace roots), and to attach **mappings** from each project to registered **patterns** with a destination relative path and optional per-mapping parameter values. Validates pattern references against central config before persisting. No Freemarker expansion or filesystem writes to the workspace — that is Phase 4.

Delivers: `vira project create/list/show/unregister` and `vira project add-mapping` per REQUIREMENTS.md PROJ-01 through PROJ-06.

</domain>

<decisions>
## Implementation Decisions

### Command surface and flags
- **D-01:** Command names and flags follow `.planning/REQUIREMENTS.md` exactly: `project create --name <n> --path <p>`, `project list`, `project add-mapping --project <name> --pattern <pattern-name> --destination <rel-path> [--param key=value ...]`, `project show --name <name>`, `project unregister --name <name>`.
- **D-02:** Repeatable `--param` for `add-mapping` — each occurrence adds one key=value pair to the mapping (picocli `arity = 0..*` or equivalent pattern used in Phase 2 for similar cases).

### Output format (aligned with Phase 2)
- **D-03:** `project list` — plain text, aligned columns, no headers (name + workspace path); same spirit as `publisher list` / `pattern list` (see `02-CONTEXT.md` D-01).
- **D-04:** `project show` — multi-line key-value block: project name, absolute workspace path, then each mapping (pattern name, destination, parameters); optional `--json` for machine-readable output consistent with Phase 2 D-03.
- **D-05:** `project list` and `project show` support `--json` where listing multiple lines of structured data (same convention as Phase 2).

### Validation and errors
- **D-06:** Registering a project name that already exists → clear error, exit 1, no config change (mirror Phase 2 D-06 wording style: `Project '<name>' already exists.`).
- **D-07:** `add-mapping` when pattern name is not in `ViracochaConfig.patterns` → clear error, exit 1, no config change (PROJ-04).
- **D-08:** `unregister` when project name not found → clear error, exit 1 (mirror Phase 2 D-07).

### Data model
- **D-09:** Replace `ViracochaConfig.projects` `List<Object>` with `List<ProjectEntry>`; each project has a unique `name`, workspace `path` (absolute or as stored — follow existing config path normalization in `ConfigService`), and a list of mappings.
- **D-10:** Each mapping references a **pattern name** (string), a **destination** relative path (string under project workspace), and a **parameters** map (string → string) for per-mapping values; keys must be valid for YAML and for downstream Freemarker (Phase 4).

### Tests
- **D-11:** Every implemented task includes at least one JUnit 5 test; `@MicronautTest` for command/config integration; `@TempDir` for config — same as Phase 2 D-08.

### Claude's Discretion
- Exact column widths and JSON property names for `--json` (prefer camelCase to match Phase 2).
- Internal package layout for `ProjectCommand` and POJOs (`ProjectEntry`, `MappingEntry` or nested types).
- Whether destination is normalized (e.g., trim, `/` vs OS separator) as long as behavior is consistent and tested.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and roadmap
- `.planning/REQUIREMENTS.md` — PROJ-01 through PROJ-06
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, dependencies
- `.planning/PROJECT.md` — central config schema note (`projects[]`), project-level vs mapping params

### Prior phase context (carry forward)
- `.planning/phases/01-foundation/01-CONTEXT.md` — Callable exit codes, config guard, test conventions
- `.planning/phases/02-publishers-and-patterns/02-CONTEXT.md` — list/show/`--json` patterns, duplicate/not-found handling, `ConfigService` load-mutate-save

### Code entry points
- `src/main/java/org/saltations/model/ViracochaConfig.java` — upgrade `projects` to typed list
- `src/main/java/org/saltations/config/ConfigService.java` — persistence
- `src/main/java/org/saltations/ViracochaCommand.java` — add `ProjectCommand` to subcommands
- `src/main/java/org/saltations/publisher/PublisherCommand.java` (or equivalent path) — mirror command group structure for `ProjectCommand`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConfigService`, `ViracochaConfig`, `PatternEntry` / `PublisherEntry` — same persistence and POJO patterns for `ProjectEntry` and mapping types.
- Picocli command groups with leaf commands and `Callable<Integer>` return codes.

### Established Patterns
- Config not initialized guard via `load()` before mutating commands.
- Plain text stdout via `CommandSpec` / `CommandLine` out; errors to stderr; exit 1 on failure.

### Integration Points
- `ViracochaCommand` static `subcommands` array must include `ProjectCommand.class`.
- Validation of pattern name against `config.getPatterns()` (or equivalent accessor) inside `add-mapping`.

</code_context>

<specifics>
## Specific Ideas

No additional vision beyond REQUIREMENTS and Phase 2 output conventions — standard CLI parity across entity types.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 03-projects-and-mappings*
*Context gathered: 2026-04-04*
