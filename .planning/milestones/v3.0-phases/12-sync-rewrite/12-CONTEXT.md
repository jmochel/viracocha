# Phase 12: Sync Rewrite - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewrite `DefaultSyncService` for the v3 sources/destinations/mappings model: walk destinations → mappings (where `sync: true`) → sources (where `templates: false`), apply glob/recurse filtering, detect conflicts via file modification timestamps, copy changed source files to destinations, and wire dry-run, verbose, and JSON output into the existing `SyncCommand` shell. Source-to-destination direction only. Template expansion does NOT happen in sync.

</domain>

<decisions>
## Implementation Decisions

### Conflict Detection Strategy
- **D-01:** Use **timestamp-based conflict detection** (`Files.getLastModifiedTime()`):
  - `source.mtime > dest.mtime` → source is newer → **copy** (update destination)
  - `dest.mtime > source.mtime` → destination is newer (locally modified) → **conflict, abort with exit 1**
  - `dest.mtime == source.mtime` → already in sync → **skip**
  - dest file does not exist → **copy** (new file)
- **D-02:** Java's `Files.copy()` does NOT copy mtime by default, so after a generate or sync run, `dest.mtime` reflects the time of the copy operation. This makes timestamp detection naturally correct: if source is updated after a prior sync, `source.mtime > dest.mtime` and sync copies the update.
- **D-03:** Content-equal check: before any copy, use `Files.mismatch()` to verify files actually differ. If content is identical despite mtime differences, treat as **skip** (no copy needed). This avoids unnecessary writes on systems where mtime may have been touched without content change.

### Template Source Handling
- **D-04:** Sync **skips mappings where `source.templates == true`**. Template expansion is a generate-only operation. `DefaultSyncService` only processes mappings referencing sources with `templates: false`.
- **D-05:** This is silent skipping — no warning or output line for skipped template mappings. The sync summary counts reflect only the non-template mappings that were processed.

### Result Model
- **D-06:** Create a new **`SyncResult` record** modeled after `GenerationResult`:
  ```java
  public record SyncResult(int copied, int skipped, int failed, int conflicts,
                            List<String> verboseLines, List<SyncConflictRecord> conflictRecords)
  ```
  with a static `SyncResult.empty()` factory.
- **D-07:** Delete `SyncEngineResult` and `SyncSubscriptionResult` — they are v2 subscription-model artifacts. `SyncConflictRecord` and `SyncConflictKind` may be reused or adapted (remove the `subscriptionId` field from `SyncConflictRecord`, keeping `relativePath`, `kind`, `message`).
- **D-08:** `SyncConflictRecord` for conflict reporting: `(String relativePath, SyncConflictKind kind, String message)`. Field `kind` uses existing `SyncConflictKind.CONTENT_MISMATCH` for timestamp-detected conflicts.

### SyncService Interface
- **D-09:** Redesign `SyncService` interface to drop the v2 subscription signature:
  ```java
  SyncResult sync(String destinationName, boolean dryRun, boolean verbose) throws IOException;
  ```
  Remove the old `syncProject(...)` methods entirely.

### SyncCommand Options
- **D-10:** Remove `--mapping-id` option from `SyncCommand` entirely — no SYN requirement covers per-mapping filtering and it was a v2 subscription artifact.
- **D-11:** Keep `--dest` as **required** (exit 2 if omitted, same pattern as `GenerateCommand`). This is consistent across all destination-targeting commands.
- **D-12:** Exit codes: 0 on success (including all-skipped), 1 on conflict or IO error, 2 on missing required option.

### Output Format
- **D-13:** Summary line (SYN-07): `"Copied: %d, Skipped: %d, Failed: %d, Conflicts: %d"` — matches existing format already in `SyncCommand`.
- **D-14:** `--verbose` per-file lines: `"Copied <dest-path>"`, `"Skipped <dest-path>"`, `"Conflict <dest-path>"`. Written to stdout before summary line.
- **D-15:** `--json` (SYN-06): serialize `SyncResult` to JSON via Jackson `ObjectMapper`. Print to stdout. Exit code still 0/1 based on conflicts.
- **D-16:** Conflict detail lines (on stderr): `"CONFLICT <relative-path> <kind>"` — matching the existing `SyncCommand` pattern.

### Claude's Discretion
- Whether to store `SyncConflictRecord.relativePath` as a POSIX-style string or a relative `Path` object internally.
- Exact ordering of file processing within a mapping (sorted paths recommended for test determinism, consistent with GeneratorService).
- Exception wrapping strategy for `IOException` within the traversal loop.
- Whether `SyncResult.empty()` returns a mutable or immutable instance.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements for this phase
- `.planning/REQUIREMENTS.md` — SYN-01 through SYN-07 (authoritative acceptance text for sync rewrite)
- `.planning/ROADMAP.md` — Phase 12 goal, success criteria, dependencies

### Key source files to implement or modify
- `src/main/java/org/saltations/sync/DefaultSyncService.java` — stub to fully implement (v3 traversal logic goes here)
- `src/main/java/org/saltations/sync/SyncService.java` — interface to redesign for v3 method signature
- `src/main/java/org/saltations/sync/SyncCommand.java` — existing CLI shell; remove --mapping-id, update to use new SyncResult, adjust for required --dest
- `src/main/java/org/saltations/sync/SyncEngineResult.java` — DELETE (v2 artifact)
- `src/main/java/org/saltations/sync/SyncSubscriptionResult.java` — DELETE (v2 artifact)
- `src/main/java/org/saltations/sync/SyncConflictRecord.java` — ADAPT (remove subscriptionId field)
- `src/main/java/org/saltations/sync/SyncConflictKind.java` — reuse as-is

### Utility infrastructure (read before using)
- `src/main/java/org/saltations/infra/GlobMatcher.java` — static `matches(String glob, Path path)`; pass relative path from source root (same as generate)
- `src/main/java/org/saltations/infra/HiddenPathFilter.java` — static `hasHiddenPathSegment(Path root, Path path)`; apply during source directory walk

### Reference implementation (generate == sync traversal pattern)
- `src/main/java/org/saltations/generate/GeneratorService.java` — v3 traversal logic (destinations → mappings → sources, glob/recurse filtering, HiddenPathFilter); sync follows the same traversal structure but replaces copy-logic with timestamp-based conflict detection
- `src/main/java/org/saltations/generate/GenerationResult.java` — template for the new `SyncResult` record structure

### Model POJOs
- `src/main/java/org/saltations/model/SourceEntry.java` — name, path, templates, parameters
- `src/main/java/org/saltations/model/DestinationEntry.java` — name, path, parameters, mappings
- `src/main/java/org/saltations/model/MappingEntry.java` — sourceRef, glob, recurse, sync, params

### Prior phase decisions that carry forward
- `.planning/phases/08-model-config-foundation/08-CONTEXT.md` — D-06 (MappingEntry defaults: glob=null, recurse=false, sync=false)
- `.planning/phases/11-generate-rewrite/11-CONTEXT.md` — D-01–D-04 (GlobMatcher full relative path; HiddenPathFilter on all walks), D-10 (tilde expansion at runtime)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GeneratorService` (`generate/`): Full v3 traversal already implemented — destinations → mappings → sources, glob/recurse filtering, HiddenPathFilter. `DefaultSyncService` follows the same traversal pattern but substitutes timestamp-based copy logic for the generate copy logic.
- `GlobMatcher` (`infra/`): `matches(String glob, Path path)` — same usage as in GeneratorService.
- `HiddenPathFilter` (`infra/`): `hasHiddenPathSegment(Path root, Path path)` — same usage as in GeneratorService.
- `ConfigService` (`config/`): Already injected into `DefaultSyncService` stub.
- `SyncConflictKind` (`sync/`): `CONTENT_MISMATCH`, `TYPE_MISMATCH`, `SYMLINK_UNSUPPORTED` — reusable as-is; `CONTENT_MISMATCH` covers timestamp-detected conflicts.
- `SyncCommand` (`sync/`): CLI shell is partially aligned with v3 — has `--dest`, `--dry-run`, `--verbose`, `--json`; needs `--mapping-id` removed and result type updated.

### Established Patterns
- `@Singleton` + `@Inject` constructor injection on all services
- `Callable<Integer>`, exit 0 on success, exit 1 on IO/business error, exit 2 on missing required option
- `@MicronautTest` + `@TempDir` for integration tests; plain JUnit 5 for unit tests
- Sorted file traversal for test determinism (established in Phase 4, continued in Phase 11)
- `spec.commandLine().getOut()` / `.getErr()` for command output — consistent with all commands

### Integration Points
- `DefaultSyncService.sync(String destinationName, boolean dryRun, boolean verbose)` — new entry point; loads config, resolves destination, iterates `sync: true` mappings, resolves non-template sources, walks source trees, applies timestamp-based copy/conflict logic
- `SyncCommand.call()` — calls `syncService.sync(...)`, processes `SyncResult`, formats output
- `ViracochaConfig.destinations` — list of `DestinationEntry`; filter by name to find target
- `ViracochaConfig.sources` — list of `SourceEntry`; look up by `sourceRef`; filter out `templates: true`
- Destination path: resolve tilde before filesystem operations (Phase 11 D-10 pattern)

</code_context>

<specifics>
## Specific Ideas

- Timestamp comparison is the primary conflict signal; use `Files.mismatch()` as a secondary check to avoid unnecessary writes when content is already identical despite mtime differences.
- Sorted traversal order within mappings is preferred for test determinism (same as GeneratorService).
- `SyncConflictRecord.relativePath` should be a POSIX-style string (use `path.toString().replace('\\', '/')` on Windows) for consistent JSON output.

</specifics>

<deferred>
## Deferred Ideas

- **Per-mapping filter (`--mapping-id`)**: No SYN requirement; removed for v3 clean break. Add in a future phase if needed.
- **`--dest` optional (sync all destinations)**: SC3 wording could support this, but user chose required for consistency with generate. Revisit in a future phase if all-destination sync is needed.
- **Template source sync**: Skipped entirely for Phase 12. If template re-expansion on sync is needed, that's a separate phase decision.

</deferred>

---

*Phase: 12-sync-rewrite*
*Context gathered: 2026-05-10*
