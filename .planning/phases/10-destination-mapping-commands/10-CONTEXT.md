# Phase 10: Destination & Mapping Commands - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement `vira destination add/list/show/remove` and `vira destination add-mapping/list-mappings/remove-mapping` commands. Introduce a `GlobMatcher` utility (infra/) wrapping JDK `FileSystem.getPathMatcher`. No changes to generate or sync in this phase.

</domain>

<decisions>
## Implementation Decisions

### DestinationCommand Structure
- **D-01:** A new `DestinationCommand` group command (`vira destination`, alias `dest`) is created with subcommands: `DestinationAddCommand`, `DestinationListCommand`, `DestinationShowCommand`, `DestinationRemoveCommand`, `DestinationAddMappingCommand`, `DestinationListMappingsCommand`, `DestinationRemoveMappingCommand`. `ViracochaCommand` adds `DestinationCommand.class` to its `subcommands`.
- **D-02:** `DestinationCommand` alias is `dest` — consistent with `SourceCommand` alias `src`.

### Service Layer
- **D-03:** Introduce a `DestinationService` singleton in a new `destination/` package. Commands are thin wrappers. Methods:
  - `addDestination(String name, String path)` — validates `..` traversal, rejects duplicate names, persists config, returns `DestinationEntry`
  - `listDestinations()` — returns `List<DestinationEntry>`
  - `getDestination(String name)` — returns `Optional<DestinationEntry>`
  - `removeDestination(String name)` — returns true if found and removed
  - `addMapping(String destName, String sourceRef, String glob, boolean recurse, boolean sync)` — validates sourceRef exists in config, appends `MappingEntry`, persists
  - `listMappings(String destName)` — returns `List<MappingEntry>` for the named destination
  - `removeMapping(String destName, int index)` — removes mapping by 0-based index, returns true if destination found and index in bounds

### Path Validation for `destination add`
- **D-04:** `destination add` validates **only** the `..` traversal guard (raw string check before `Path.of()` — same technique as `SourceService`). No existence check. Destination paths may be workspace dirs that do not exist at registration time.
- **D-05:** Error for traversal: `"Path must not contain '..': <path>"` (DEST-06)
- **D-06:** Error for duplicate name: `"Destination '<name>' already exists."` (DEST-05)
- **D-07:** Store the path as-is (no normalization required since existence is not checked). Tilde paths (`~/workspace`) are stored as-is and expanded at generate/sync time (Phase 11/12 concern).

### `destination list` Output
- **D-08:** Plain output: name + path, aligned columns, no header — same pattern as `source list`.
- **D-09:** `--json` flag: JSONL (one JSON object per line per `DestinationEntry`), consistent with `source list --json`.

### `destination show` Output
- **D-10:** Plain-text multi-line block (in order):
  ```
  Name:      <name>
  Path:      <path>
  Parameters:
    <key>: <value>
    ...
  Mappings: (none)
  ```
  OR with mappings:
  ```
  Name:      <name>
  Path:      <path>
  Parameters:
    projectName: myapp
  Mapping 1:
    Source:  <sourceRef>
    Glob:    <pattern>   (or "(all files)" when glob is null)
    Recurse: <true|false>
    Sync:    <true|false>
  Mapping 2:
    ...
  ```
- **D-11:** `Parameters:` section (with indented `key: value` lines) is omitted entirely when the parameters map is empty.
- **D-12:** When a destination has no mappings, display `Mappings: (none)` on a single line — do not omit the section.
- **D-13:** When a mapping's `glob` is null (no filter), display `Glob:    (all files)`. Always show all mapping fields including `Recurse` and `Sync`.
- **D-14:** `--json` flag outputs a single JSON object for the full `DestinationEntry` (including nested parameters and mappings).

### `destination add-mapping` CLI
- **D-15:** CLI signature: `vira destination add-mapping DEST-NAME --source SOURCE-NAME [--glob PATTERN] [--recurse] [--sync]`
  - `DEST-NAME` is a positional `@Parameters(index="0")` argument.
  - `--source` is required (MAP-04 validates the referenced source name exists).
  - `--glob`, `--recurse`, `--sync` are optional; defaults are `null`, `false`, `false`.
- **D-16:** Success message: `"Mapping added to destination '<name>'."` (no index shown; user can run `destination show` to see the full list).
- **D-17:** Error if destination not found: `"Destination '<name>' not found."`
- **D-18:** Error if source not found: `"Source '<sourceRef>' not found."` (MAP-04)

### `destination list-mappings` CLI
- **D-19:** CLI: `vira destination list-mappings NAME` (positional). Prints numbered blocks in the same format as the mappings section of `destination show`. If no mappings: `"No mappings for destination '<name>'."` to stdout, exit 0.
- **D-20:** `--json` flag: JSONL of `MappingEntry` objects.

### `destination remove-mapping` CLI
- **D-21:** CLI: `vira destination remove-mapping NAME INDEX` (both positional, INDEX is an integer, 0-based).
- **D-22:** Success message: `"Mapping <INDEX> removed from destination '<name>'."`.
- **D-23:** Error if destination not found: `"Destination '<name>' not found."` Exit 1.
- **D-24:** Error if index out of range: `"Mapping index <N> out of range (destination has <M> mappings)."` Exit 1.

### GlobMatcher Utility
- **D-25:** `GlobMatcher` lives in `infra/` alongside `HiddenPathFilter` and `FreemarkerVariableExtractor` — it is a general-purpose utility that generate/sync (phases 11/12) will also use.
- **D-26:** `GlobMatcher` wraps `FileSystems.getDefault().getPathMatcher("glob:" + pattern)`. The `+` character must be treated as a literal in glob patterns (not a regex quantifier) — JDK `PathMatcher` with `glob:` prefix handles this correctly; unit tests must verify this explicitly (MAP-05).

### Error Messages
- **D-27:** Destination not found (show/remove/add-mapping/list-mappings/remove-mapping): `"Destination '<name>' not found."`
- **D-28:** All commands: exit 0 on success, exit 1 on any error (config not initialized, validation failure, not found, index out of range, IO error).

### Testing
- **D-29:** `@MicronautTest` + `@TempDir` for command integration tests. Pure-logic unit tests (e.g., `GlobMatcher`, `DestinationService` validation) can use plain JUnit 5 without `@MicronautTest`. Every new class must have at least one test.

### Claude's Discretion
- Exact column alignment implementation in `destination list`
- Whether `DestinationService.removeMapping` returns a boolean or throws a checked exception for out-of-range index
- Whether `addMapping` returns `MappingEntry` or void (choose what makes tests simplest)
- Exact `GlobMatcher` method signature (e.g., `boolean matches(String glob, Path path)` or a `PathMatcher` factory method)
- Mapping `params` field handling: `add-mapping` does not expose `--params` flag in this phase (per-mapping param overrides are out of scope per REQUIREMENTS.md Out of Scope table)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements for this phase
- `.planning/REQUIREMENTS.md` — DEST-01 through DEST-06 (destination CRUD), MAP-01 through MAP-05 (mapping management, GlobMatcher)

### Key source files to understand before modifying
- `src/main/java/org/saltations/model/DestinationEntry.java` — POJO with name, path, parameters, mappings fields
- `src/main/java/org/saltations/model/MappingEntry.java` — POJO with sourceRef, glob, recurse, sync, params fields
- `src/main/java/org/saltations/model/ViracochaConfig.java` — config root; `destinations` list lives here
- `src/main/java/org/saltations/model/SourceEntry.java` — needed to validate sourceRef exists in `config.sources`
- `src/main/java/org/saltations/config/ConfigService.java` — load/save; DestinationService injects this
- `src/main/java/org/saltations/ViracochaCommand.java` — add `DestinationCommand.class` to subcommands list
- `src/main/java/org/saltations/source/SourceCommand.java` — group command pattern to replicate for DestinationCommand (including alias)
- `src/main/java/org/saltations/source/SourceService.java` — service pattern to replicate for DestinationService
- `src/main/java/org/saltations/source/SourceAddCommand.java` — command pattern (Callable<Integer>, @Inject, @Spec, exit codes)
- `src/main/java/org/saltations/source/SourceShowCommand.java` — show command pattern to replicate
- `src/main/java/org/saltations/infra/HiddenPathFilter.java` — infra/ package where GlobMatcher should live

### Prior phase decisions that carry forward
- `.planning/phases/09-source-commands/09-CONTEXT.md` — D-11 (Callable<Integer>, exit codes, getOut/getErr), D-08 (service layer pattern), D-12–D-16 (error message format)
- `.planning/phases/08-model-config-foundation/08-CONTEXT.md` — D-08 (Lombok @Data @NoArgsConstructor @AllArgsConstructor), D-06 (MappingEntry field defaults)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SourceService` (`source/`): CRUD pattern (load-validate-save) to replicate for `DestinationService`
- `SourceCommand` (`source/`): group command with alias — replicate for `DestinationCommand` with alias `dest`
- `SourceShowCommand` (`source/`): multi-line key-value show pattern — extend for nested parameters/mappings
- `SourceListCommand` (`source/`): JSONL list pattern — replicate for destination list
- `ConfigService` (`config/`): inject into `DestinationService` for load/save
- `ConfigNotInitializedException` (`config/`): surface as exit 1 in all commands

### Established Patterns
- Thin command → service singleton: all validation and persistence in the service, commands just parse args and format output
- `@Singleton` on all commands and services; `@Inject` constructor injection
- `@Spec CommandSpec spec` for output via `spec.commandLine().getOut()` / `.getErr()`
- `@Parameters(index="0")` for positional destination name in show/remove/add-mapping/list-mappings/remove-mapping
- Lombok `@Data @NoArgsConstructor @AllArgsConstructor` on all model POJOs (no `@Builder`)
- JSONL for list, single JSON object for show — using Jackson `ObjectMapper` directly

### Integration Points
- `ViracochaCommand.@Command(subcommands = {...})` — add `DestinationCommand.class` here
- `ConfigService.load()` returns `ViracochaConfig`; `DestinationService` reads and writes `config.destinations`
- `DestinationService.addMapping` reads `config.sources` to validate `sourceRef` — dependency on source entries
- `GlobMatcher` (`infra/`) will be used by generate (Phase 11) and sync (Phase 12) — design API with those consumers in mind

</code_context>

<specifics>
## Specific Ideas

No specific implementation references beyond the decisions above — open to standard approaches.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 10-destination-mapping-commands*
*Context gathered: 2026-05-09*
