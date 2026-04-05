# Phase 2: Catalogs and Patterns - Context

**Gathered:** 2026-03-28
**Status:** Ready for planning

<domain>
## Phase Boundary

CLI commands to register, list, show, and unregister named catalogs and named patterns. At pattern registration time, Freemarker variable names are automatically extracted from template file content and path segments, and stored in central config. No generation logic — that is Phase 4.

Delivers: `vira catalog register/list/show/unregister` and `vira pattern register/list/show/unregister`.

</domain>

<decisions>
## Implementation Decisions

### Output Format
- **D-01:** `publisher list` and `pattern list` output plain text with aligned columns, no headers. Example: `<name>  <path>` or `<name>  <path>  <param-count>`.
- **D-02:** `publisher show` and `pattern show` output a multi-line key-value block. Example: `Name: foo` / `Path: /some/path` / `Parameters: name, email, role`.
- **D-03:** All list and show commands support a `--json` flag that switches output to JSONL (one JSON object per line per entry, or a single JSON object for show).

### Freemarker Variable Extraction
- **D-04:** Extraction captures `${varName}` syntax only — top-level name before any `.` or `?`. Example: `${user.name}` → extracts `user`; `${title?upper_case}` → extracts `title`. Applies to file content AND file/folder name path segments.
- **D-05:** If any file in the pattern has a malformed Freemarker expression (e.g., `${unclosed`), registration fails fast — print a clear error, exit 1, do not modify config.

### Duplicate and Not-Found Handling
- **D-06:** Registering a name that already exists → print error (`Publisher/Pattern 'foo' already registered. Use unregister first.`), exit 1, do not modify config.
- **D-07:** Unregistering a name that does not exist → print error (`Publisher/Pattern 'foo' not found.`), exit 1.

### Test Requirements
- **D-08:** Every implemented task must include at least one JUnit 5 test verifying its behavior before the task is considered complete. Use `@MicronautTest` for integration tests (commands, config round-trips); plain JUnit 5 for pure-logic units (e.g., the Freemarker extractor). Test fixture config paths must point to `@TempDir` directories — never the real XDG config.

### Claude's Discretion
- Exact column alignment widths for plain text output
- Precise JSON field names for `--json` output (should be camelCase, e.g., `name`, `path`, `parameters`)
- Internal package structure for publisher/pattern commands and model POJOs
- Whether `CatalogCommand` and `PatternCommand` are in separate packages or a shared `commands` package
- Freemarker extraction regex implementation detail (character class for valid identifier chars)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

No external specs — requirements fully captured in decisions above and REQUIREMENTS.md.

### Reference implementations (patterns to replicate)
- `src/main/java/org/saltations/config/InitCommand.java` — command class pattern (`@Singleton`, `@Inject`, `@Spec CommandSpec`, `Callable<Integer>`)
- `src/main/java/org/saltations/config/ConfigService.java` — config load/save persistence pattern
- `src/main/java/org/saltations/model/ViracochaConfig.java` — root config model (lists must change from `List<Object>` to typed entries in Phase 2)
- `src/main/java/org/saltations/ViracochaCommand.java` — root command (must add `CatalogCommand` and `PatternCommand` to `subcommands`)
- `.planning/phases/01-foundation/01-CONTEXT.md` — all Phase 1 locked decisions carry forward

### Requirements
- `.planning/REQUIREMENTS.md` — PUB-01 through PUB-05, PAT-01 through PAT-06 (the full acceptance criteria)
- `.planning/ROADMAP.md` — Phase 2 success criteria

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConfigService.load()` / `ConfigService.save()`: load-mutate-save pattern for all register/unregister commands
- `@Spec CommandSpec`: use `spec.commandLine().getOut().println()` for stdout and `spec.commandLine().getErr().println()` for stderr — already established
- `XdgPaths`: config/data path resolution — do not inline env reads

### Established Patterns
- All command classes: `@Command` + `@Singleton` + `@Inject` constructor + `Callable<Integer>`
- All subcommands declared statically in `@Command(subcommands = {...})` — no dynamic registration
- Exit codes: 0 = success, 1 = any error (IO, validation, config-not-initialized)
- Config guard: call `configService.load()` at the start of every command that reads/mutates config; `ConfigNotInitializedException` propagates the CONF-03 error

### Integration Points
- `ViracochaConfig.publishers` and `ViracochaConfig.patterns` change from `List<Object>` to `List<CatalogEntry>` and `List<PatternEntry>` in Phase 2
- `ViracochaCommand.subcommands` must include `CatalogCommand.class` and `PatternCommand.class`
- `freemarker` dependency must be added to `pom.xml` (noted as Phase 2 prerequisite in STATE.md)

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches for command structure and model POJO naming.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-publishers-and-patterns*
*Context gathered: 2026-03-28*
