# Phase 9: Source Commands - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement `vira source add/list/show/remove` commands. Users can register named local directory sources, optionally with Freemarker template variable extraction (`--templates` flag). No other entities (destinations, mappings, generate, sync) are touched in this phase.

</domain>

<decisions>
## Implementation Decisions

### Path Validation
- **D-01:** `source add` validates the path at registration time — fail fast, not lazily. Specifically:
  1. Reject any path containing `..` directory traversal sequences (SRC-06)
  2. Reject paths that do not exist on disk with a descriptive error: `"Path does not exist: <path>"`
  3. Reject paths that are not a directory with a descriptive error: `"Path is not a directory: <path>"`
  4. Store the path as an absolute, normalized path (use `Path.toAbsolutePath().normalize()` before storing)
- **D-02:** No additional path checks beyond the above (no readability check, no duplicate-path check). SRC-05 covers duplicate *names*; two different names can point to the same directory.

### source list Output
- **D-03:** `vira source list` prints name + path only, aligned columns, no header — same minimal pattern as Phase 2 publisher/pattern list.
- **D-04:** `--json` flag switches output to JSONL (one JSON object per line per source entry), consistent with Phase 2 D-03.

### source show Output
- **D-05:** `vira source show NAME` displays a multi-line key-value block with these fields (in order):
  ```
  Name:      <name>
  Path:      <path>
  Templates: <true|false>
  Parameters:
    <var1>
    <var2>
    ...
  ```
- **D-06:** `Parameters:` section (with one variable per indented line) is only shown when the source has extracted parameters. If `templates: false` or parameter list is empty, the `Parameters:` block is omitted entirely.
- **D-07:** `--json` flag outputs a single JSON object for the source entry, consistent with Phase 2 show commands.

### Service Layer
- **D-08:** Introduce a `SourceService` singleton in a new `source/` package. Commands are thin (parse args, call service, format output). Service methods:
  - `addSource(String name, String path, boolean templates)` — validates, extracts params if templates=true, persists config, returns `SourceEntry`
  - `listSources()` — loads config, returns `List<SourceEntry>`
  - `getSource(String name)` — loads config, returns `Optional<SourceEntry>`
  - `removeSource(String name)` — loads config, removes entry, saves config; returns true if found and removed
- **D-09:** `SourceService` is injected with `ConfigService` (for load/save) and `FreemarkerVariableExtractor` (from `infra/`). Both are `@Singleton` beans.

### Command Structure
- **D-10:** A new `SourceCommand` group command (`vira source`) is created, listing `SourceAddCommand`, `SourceListCommand`, `SourceShowCommand`, `SourceRemoveCommand` as subcommands. `ViracochaCommand` adds `SourceCommand.class` to its `subcommands`.
- **D-11:** All commands use `Callable<Integer>`, exit 0 on success, exit 1 on any error (config not initialized, validation failure, name not found). Output via `spec.commandLine().getOut()` / `.getErr()` — consistent with all prior commands.

### Error Messages
- **D-12:** Duplicate source name: `"Source '<name>' already exists."` (SRC-05)
- **D-13:** Path traversal: `"Path must not contain '..': <path>"` (SRC-06)
- **D-14:** Path doesn't exist: `"Path does not exist: <path>"`
- **D-15:** Path not a directory: `"Path is not a directory: <path>"`
- **D-16:** Source not found (show/remove): `"Source '<name>' not found."`

### Testing
- **D-17:** Every task must include at least one JUnit 5 test. `@MicronautTest` for command integration tests; `@TempDir` for config and source directories. Pure-logic unit tests (e.g., SourceService validation) can use plain JUnit 5 without `@MicronautTest`.

### Claude's Discretion
- Exact picocli parameter annotations for positional `NAME` in show/remove vs `--name` in add (follow REQUIREMENTS.md wording: `vira source show NAME` → positional; `vira source add --name --path` → named options)
- Whether `removeSource` throws a checked exception or returns a boolean; command handles missing-name case
- Exact column alignment implementation in list output
- Whether `addSource` returns `SourceEntry` or void (choose what makes tests simplest to write)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements for this phase
- `.planning/REQUIREMENTS.md` — SRC-01 through SRC-07 (source add/list/show/remove, templates extraction, duplicate rejection, traversal rejection)

### Key source files to understand before modifying
- `src/main/java/org/saltations/model/SourceEntry.java` — POJO with name, path, templates, parameters fields
- `src/main/java/org/saltations/model/ViracochaConfig.java` — config root; `sources` list lives here
- `src/main/java/org/saltations/config/ConfigService.java` — load/save; SourceService injects this
- `src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java` — `extractFromDirectory(Path)` returns sorted List<String>; SourceService injects this
- `src/main/java/org/saltations/ViracochaCommand.java` — add `SourceCommand.class` to subcommands list
- `src/main/java/org/saltations/config/ConfigCommand.java` — group command pattern to replicate for SourceCommand
- `src/main/java/org/saltations/config/InitCommand.java` — command pattern (Callable<Integer>, @Inject, @Spec, exit codes)

### Prior phase decisions that carry forward
- `.planning/phases/01-foundation/01-CONTEXT.md` — exit codes (Callable<Integer>), Lombok patterns, test requirements
- `.planning/phases/02-publishers-and-patterns/02-CONTEXT.md` — D-01 (list format), D-03 (--json), D-06/D-07 (error exit codes)
- `.planning/phases/08-model-config-foundation/08-CONTEXT.md` — D-02 (FreemarkerVariableExtractor in infra/), D-04 (SourceEntry fields), D-08 (Lombok @Data @NoArgsConstructor @AllArgsConstructor)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FreemarkerVariableExtractor` (`infra/`): `extractFromDirectory(Path)` — call from `SourceService.addSource()` when templates=true
- `ConfigService` (`config/`): `load()` / `save()` — inject into `SourceService`
- `SourceEntry` (`model/`): POJO ready; matches all required fields (name, path, templates, parameters)
- `ViracochaConfig.sources` (`model/`): `List<SourceEntry>` — the list SourceService operates on
- `ConfigNotInitializedException` (`config/`): commands should propagate this as exit 1

### Established Patterns
- Group command pattern: `ConfigCommand` → `{subcommands}` — replicate for `SourceCommand`
- `@Spec CommandSpec spec` for `spec.commandLine().getOut()` / `.getErr()` — all existing commands use this
- `@MicronautTest` + `@TempDir` for integration tests (config dir and source directories)
- Lombok `@Data @NoArgsConstructor @AllArgsConstructor` on all model POJOs

### Integration Points
- `ViracochaCommand.@Command(subcommands = {...})` — add `SourceCommand.class` here
- `ConfigService.load()` returns `ViracochaConfig`; `SourceService` reads and writes `config.sources`
- `FreemarkerVariableExtractor.extractFromDirectory(Path)` throws `IOException` on malformed expressions — surface as exit 1 in `SourceAddCommand`

</code_context>

<specifics>
## Specific Ideas

No specific implementation references beyond the decisions above — open to standard approaches for column alignment and JSON serialization.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 09-source-commands*
*Context gathered: 2026-05-09*
