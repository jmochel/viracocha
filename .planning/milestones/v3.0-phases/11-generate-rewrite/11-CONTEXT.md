# Phase 11: Generate Rewrite - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement `GeneratorService` for the v3 sources/destinations/mappings model: traverse destinations → mappings → sources, apply glob/recurse filtering, expand Freemarker templates in path segments and file content for `templates: true` sources, byte-copy all files from `templates: false` sources, and wire skip-existing, dry-run, and verbose into the existing `GenerateCommand` shell. No sync logic, no per-mapping parameter overrides, no new CLI options beyond what `GenerateCommand` already declares.

</domain>

<decisions>
## Implementation Decisions

### Glob Pattern Matching Scope
- **D-01:** When `recurse: true` and a glob is set, pass the **full relative path from source root** to `GlobMatcher.matches()` (e.g., `docs/api/readme.md`, not just `readme.md`). This enables `**/*.md` patterns to match across subdirectories.
- **D-02:** When `recurse: false` and a glob is set, walk only the immediate source directory (depth 0). The relative path of each top-level file IS its filename, so glob matching behaves identically whether framed as "filename" or "relative path" — use the relative path consistently for a uniform API.
- **D-03:** `glob: null` means no filter — all files are selected regardless of `recurse` setting.
- **D-04:** `HiddenPathFilter.hasHiddenPathSegment()` applies to all source directory walks. Files or directories with any dotfile path segment (e.g., `.git/`, `.claude/`) are skipped. This is consistent with the v1/v2 generator behavior.

### Missing Destination Directory
- **D-05:** When the top-level destination path does **not** exist on the filesystem, prompt the user:
  ```
  Destination /path/to/dest does not exist. Create it? [y/N]
  ```
  Written to stdout, waits for user input.
- **D-06:** If user responds `y` or `Y`, create the top-level destination directory (using `Files.createDirectories`) then proceed with generation.
- **D-07:** If user responds anything other than `y`/`Y` (including Enter/empty), exit with code **0** — user chose not to proceed, not an error condition.
- **D-08:** In `--dry-run` mode, skip the prompt entirely. Report `"Would create: <path>"` on stdout and continue the dry-run simulation.
- **D-09:** Sub-directories **inside** an existing destination are auto-created silently without any prompting. Only the top-level destination root triggers the confirmation.

### Tilde Expansion
- **D-10:** Expand `~` at the start of a destination path to `System.getProperty("user.home")` in `GeneratorService` before any filesystem operations. Stored path strings are not modified — expansion is runtime-only. (Phase 10 D-07 deferred this here.)

### File Copy Strategy
- **D-11:** For sources with `templates: false` — byte-copy all files using `Files.copy()`. No string processing. Preserves binary files intact (GEN-04).
- **D-12:** For sources with `templates: true` — use `PathExpander.expandSegment()` on each path segment, and Freemarker template processing on file content. Both path expansion and content expansion use the destination's `parameters` map as the data model.
- **D-13:** Per-mapping parameter overrides (`MappingEntry.params`) are **not** used for template expansion in this phase. Destination-level parameters only. (Consistent with REQUIREMENTS.md Out of Scope: "Per-mapping parameter overrides".)

### `--dest` Behavior
- **D-14:** Keep `--dest` as effectively required (current `GenerateCommand` behavior): exit 2 with `"Missing required option: '--dest'"` if omitted. This was not selected for discussion — preserve current behavior.

### Output Format
- **D-15:** Summary line always written to stdout: `"Generated: N files, Skipped: M files, Failed: K files"` — existing format, unchanged.
- **D-16:** `--verbose`: one line per file action: `"Created <dest-path>"`, `"Skipped <dest-path>"`, `"Failed <dest-path>"`. Written to stdout before the summary line.
- **D-17:** `--dry-run`: emit planned `"Would create <dest-path>"` lines (and the "Would create destination directory" line if applicable), but write no files. Summary line still printed at end.

### Claude's Discretion
- Freemarker `Configuration` lifecycle — a new instance per generate run (or shared) is an implementation detail.
- Exact ordering of file processing within a mapping (sorted paths recommended for test determinism, consistent with Phase 4 specifics note).
- Whether dry-run summary uses the same `"Generated: / Skipped: / Failed:"` format or a `"Would generate:"` variant.
- Exception wrapping strategy for `TemplateException` and `IOException` within the traversal loop.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements for this phase
- `.planning/REQUIREMENTS.md` — GEN-01 through GEN-07 (authoritative acceptance text for generate rewrite)
- `.planning/ROADMAP.md` — Phase 11 goal, success criteria, dependencies

### Key source files to implement or modify
- `src/main/java/org/saltations/generate/GeneratorService.java` — stub to fully implement (v3 traversal logic goes here)
- `src/main/java/org/saltations/generate/GenerateCommand.java` — existing CLI shell with --dest, --dry-run, --verbose; keep as-is unless a tweak is needed for the prompt
- `src/main/java/org/saltations/generate/GenerationResult.java` — record with generated/skipped/failed/verboseLines; use as-is
- `src/main/java/org/saltations/generate/PathExpander.java` — existing singleton; use for path segment expansion and file content expansion on template sources

### Utility infrastructure (read before using)
- `src/main/java/org/saltations/infra/GlobMatcher.java` — static `matches(String glob, Path path)`; pass relative path from source root
- `src/main/java/org/saltations/infra/HiddenPathFilter.java` — static `hasHiddenPathSegment(Path root, Path path)`; apply during source directory walk
- `src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java` — reference for understanding how source trees are walked (generation walk is similar)

### Model POJOs
- `src/main/java/org/saltations/model/SourceEntry.java` — name, path, templates, parameters
- `src/main/java/org/saltations/model/DestinationEntry.java` — name, path, parameters (Map<String,String>), mappings
- `src/main/java/org/saltations/model/MappingEntry.java` — sourceRef, glob, recurse, sync, params

### Prior phase decisions that carry forward
- `.planning/phases/08-model-config-foundation/08-CONTEXT.md` — D-06 (MappingEntry field defaults: glob=null, recurse=false, sync=false)
- `.planning/phases/10-destination-mapping-commands/10-CONTEXT.md` — D-07 (tilde path expansion is Phase 11 concern), D-25/D-26 (GlobMatcher in infra/)
- `.planning/phases/04-workspace-generation/04-CONTEXT.md` — Phase 4 established PathExpander, skip-existing, and summary output format (now reused for v3)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GenerateCommand` (`generate/`): CLI shell is complete — `--dest`, `--dry-run`, `--verbose` options wired; calls `generatorService.generate(name, dryRun, verbose)` and formats `GenerationResult`. No changes needed unless the destination-creation prompt requires a new code path.
- `GenerationResult` (`generate/`): Record `(generated, skipped, failed, verboseLines)` — accumulate counts and lines in GeneratorService and return this.
- `PathExpander` (`generate/`): `expandSegment(String segment, Map<String,String> model)` — call once per path segment during name expansion; call via Freemarker string template for file content.
- `GlobMatcher` (`infra/`): `matches(String glob, Path path)` — pass the relative path from source root (not full absolute path); supports `**` patterns correctly via JDK `PathMatcher`.
- `HiddenPathFilter` (`infra/`): `hasHiddenPathSegment(Path root, Path path)` — filter during `Files.walk()` traversal.
- `ConfigService` (`config/`): Already injected into GeneratorService stub — use to load `ViracochaConfig` at start of `generate()`.

### Established Patterns
- `@Singleton` + `@Inject` constructor injection on all services
- `spec.commandLine().getOut()` / `.getErr()` for output — for the destination-creation prompt, use `spec.commandLine().getOut()` for the prompt text and read from `System.in` (or inject an `InputStream` for testability)
- `Callable<Integer>`, exit 0 on success, exit 1 on IO/business error, exit 2 on missing required option — consistent with all prior commands
- `@MicronautTest` + `@TempDir` for integration tests; plain JUnit 5 for unit tests
- Sorted file traversal preferred for test determinism (see Phase 4 specifics)

### Integration Points
- `GeneratorService.generate(String destinationName, boolean dryRun, boolean verbose)` — the main entry point; loads config, resolves destination, iterates mappings, resolves sources, walks source trees, copies/expands files
- `ViracochaConfig.destinations` — list of `DestinationEntry`; filter by name to find the target destination
- `ViracochaConfig.sources` — list of `SourceEntry`; look up by name to resolve each mapping's `sourceRef`
- Destination path: resolve tilde → check existence → prompt if missing → proceed or exit 0

</code_context>

<specifics>
## Specific Ideas

- Phase 4 context noted: "Generation should feel predictable in CI: deterministic ordering of processed files appreciated (e.g., sorted paths) for testability." This still applies — sort files during source tree walk.
- STATE.md flagged: "Binary file copy (GEN-04) needs a dedicated integration test with a non-text file to verify no corruption." Include at least one integration test with a binary fixture file (e.g., a small PNG or a `.bin` file with known bytes).
- The destination-creation confirmation prompt should flush stdout before reading from stdin to avoid buffering issues in some terminals.

</specifics>

<deferred>
## Deferred Ideas

- **`--dest` as optional (generate all destinations)**: Discussed but not selected. The user chose not to revisit this — current behavior (required) is preserved. If generate-all is needed, that's a future phase.
- **Per-mapping parameter overrides**: `MappingEntry.params` field exists but is explicitly out of scope per REQUIREMENTS.md. Deferred to a future enhancement if needed.

</deferred>

---

*Phase: 11-generate-rewrite*
*Context gathered: 2026-05-10*
