# Phase 12: Sync Rewrite - Research

**Researched:** 2026-05-10
**Domain:** Java NIO file sync, timestamp-based conflict detection, picocli command rewrite
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01: Conflict detection via `Files.getLastModifiedTime()`**
- `source.mtime > dest.mtime` → source is newer → copy (update destination)
- `dest.mtime > source.mtime` → destination is newer (locally modified) → conflict, abort with exit 1
- `dest.mtime == source.mtime` → already in sync → skip
- dest file does not exist → copy (new file)

**D-02:** `Files.copy()` does NOT copy mtime by default. After a prior sync, `dest.mtime` reflects the copy time. If the source is updated later, `source.mtime > dest.mtime` naturally triggers a copy.

**D-03: Content-equal check:** Before any copy, use `Files.mismatch()` to verify files actually differ. If content is identical despite mtime differences, treat as skip (no copy needed).

**D-04: Sync skips mappings where `source.templates == true`.** Template expansion is generate-only. `DefaultSyncService` only processes non-template sources.

**D-05: Silent skipping** of template mappings — no warning or output line. Summary counts reflect only non-template mappings processed.

**D-06: Create `SyncResult` record** modeled after `GenerationResult`:
```java
public record SyncResult(int copied, int skipped, int failed, int conflicts,
                          List<String> verboseLines, List<SyncConflictRecord> conflictRecords)
```
with a static `SyncResult.empty()` factory.

**D-07: Delete `SyncEngineResult` and `SyncSubscriptionResult`** — v2 subscription-model artifacts.

**D-08: `SyncConflictRecord` fields:** `(String relativePath, SyncConflictKind kind, String message)`. Remove `subscriptionId` field. Use `SyncConflictKind.CONTENT_MISMATCH` for timestamp-detected conflicts.

**D-09: Redesign `SyncService` interface:**
```java
SyncResult sync(String destinationName, boolean dryRun, boolean verbose) throws IOException;
```
Remove all `syncProject(...)` methods.

**D-10:** Remove `--mapping-id` option from `SyncCommand` entirely.

**D-11: `--destination-name` is required** (exit 2 if omitted, same pattern as `GenerateCommand`).

**D-12: Exit codes:** 0 on success (including all-skipped), 1 on conflict or IO error, 2 on missing required option.

**D-13: Summary line:** `"Copied: %d, Skipped: %d, Failed: %d, Conflicts: %d"`

**D-14: `--verbose` per-file lines:** `"Copied <dest-path>"`, `"Skipped <dest-path>"`, `"Conflict <dest-path>"`. Written to stdout before summary line.

**D-15: `--json`:** Serialize `SyncResult` to JSON via Jackson `ObjectMapper`. Print to stdout. Exit code still 0/1 based on conflicts.

**D-16: Conflict detail lines on stderr:** `"CONFLICT <relative-path> <kind>"` — matching existing `SyncCommand` pattern.

### Claude's Discretion

- Whether to store `SyncConflictRecord.relativePath` as a POSIX-style string or a relative `Path` object internally.
- Exact ordering of file processing within a mapping (sorted paths recommended for test determinism, consistent with GeneratorService).
- Exception wrapping strategy for `IOException` within the traversal loop.
- Whether `SyncResult.empty()` returns a mutable or immutable instance.

### Deferred Ideas (OUT OF SCOPE)

- **Per-mapping filter (`--mapping-id`):** No SYN requirement; removed for v3 clean break.
- **`--destination-name` optional (sync all destinations):** Chosen required for consistency with generate.
- **Template source sync:** Skipped entirely for Phase 12.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SYN-01 | `vira sync` copies changed source files to the destination for all mappings with `sync: true` | D-01 timestamp logic; GeneratorService traversal pattern (walk destinations → mappings → filter `sync: true`) |
| SYN-02 | `vira sync` detects conflicts (destination file newer than source) and aborts with exit 1 | D-01 mtime comparison; `Files.mismatch()` (D-03); D-12 exit codes |
| SYN-03 | `vira sync` accepts `--destination-name` to target a single destination | D-11 required option, exit 2 if omitted |
| SYN-04 | `vira sync` supports `--dry-run` | D-14 dry-run reporting pattern from GeneratorService |
| SYN-05 | `vira sync` supports `--verbose` | D-14 per-file lines before summary |
| SYN-06 | `vira sync` supports `--json` for machine-readable output | D-15 Jackson ObjectMapper on SyncResult |
| SYN-07 | `vira sync` prints a summary line with copied/skipped/failed/conflict counts | D-13 format string |

</phase_requirements>

## Summary

Phase 12 rewrites the v2 `DefaultSyncService` stub into a fully functional v3 sync engine. The traversal pattern is already proven in `GeneratorService` (Phase 11): destinations → mappings (filter `sync: true`) → sources (filter `templates: false`), applying glob/recurse/HiddenPathFilter. The key behavioral difference from generate is that sync **updates** existing destination files rather than skipping them, using timestamp comparison as the change-detection signal.

The model surgery is straightforward: delete two v2 classes (`SyncEngineResult`, `SyncSubscriptionResult`), adapt `SyncConflictRecord` to remove `subscriptionId`, create a new `SyncResult` record modeled on `GenerationResult`, and redesign the `SyncService` interface. The `SyncCommand` shell already has most of the right options; it needs `--mapping-id` removed and its result processing updated to use `SyncResult`.

The Java NIO APIs needed (`Files.getLastModifiedTime()`, `Files.mismatch()`, `Files.copy()` with `REPLACE_EXISTING`) are stable standard library features with no external dependencies. Jackson's `ObjectMapper` is already used in `ConfigService` and `SyncCommand` — no new dependency needed.

**Primary recommendation:** Implement in waves: (Wave 0) test stubs, (Wave 1) model surgery + SyncResult + SyncService interface, (Wave 2) DefaultSyncService traversal logic, (Wave 3) SyncCommand integration + JSON output.

## Standard Stack

### Core (All Already Present in Project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `java.nio.file.Files` | JDK 21 | `getLastModifiedTime()`, `mismatch()`, `copy()`, `walk()` | Standard library — no addition needed |
| `java.nio.file.attribute.FileTime` | JDK 21 | Timestamp comparison (`compareTo`) | Standard library |
| `com.fasterxml.jackson.databind.ObjectMapper` | 2.x (via micronaut-serde-jackson) | JSON serialization of `SyncResult` | Already used in project; no new dependency |
| JUnit 5 (Jupiter) | 5.x | Test framework | Project standard |
| `picocli` | 4.x | CLI command parsing | Project standard |

### No New Dependencies Required

The sync rewrite uses only APIs already present in the JDK 21 and the existing project classpath. No `npm install` or `mvn dependency add` equivalent is needed.

**Version verification:** All versions confirmed from `pom.xml` and existing codebase — no npm registry check needed for a pure Java project.

## Architecture Patterns

### Recommended Project Structure

The sync package follows the existing pattern:

```
src/main/java/org/saltations/sync/
├── SyncService.java           # Redesigned interface (v3 signature)
├── DefaultSyncService.java    # Full v3 implementation (replaces stub)
├── SyncCommand.java           # Updated CLI shell
├── SyncResult.java            # New record (replaces SyncEngineResult + SyncSubscriptionResult)
├── SyncConflictRecord.java    # Adapted (remove subscriptionId)
└── SyncConflictKind.java      # Unchanged (reuse as-is)
```

Files to DELETE:
```
src/main/java/org/saltations/sync/SyncEngineResult.java    # DELETE
src/main/java/org/saltations/sync/SyncSubscriptionResult.java  # DELETE
```

```
src/test/java/org/saltations/sync/
├── DefaultSyncServiceTest.java  # Wave 0 stub / Wave 2 full
└── SyncCommandTest.java         # Wave 0 stub / Wave 3 full
```

### Pattern 1: Timestamp-Based Change Detection

**What:** Compare `source.mtime` vs `dest.mtime` to determine if a file needs updating.
**When to use:** For every source file that passes glob/recurse/HiddenPathFilter.

```java
// Source: JDK 21 java.nio.file.Files / FileTime (standard library)
FileTime srcMtime = Files.getLastModifiedTime(sourcePath);
Path destPath = destRoot.resolve(relPath);

if (!Files.exists(destPath)) {
    // New file — always copy
    copyFile(sourcePath, destPath, dryRun, ...);
} else {
    FileTime dstMtime = Files.getLastModifiedTime(destPath);
    int cmp = srcMtime.compareTo(dstMtime);

    if (cmp == 0) {
        // Same mtime — already in sync, but verify content just in case
        // (per D-03: use Files.mismatch() to avoid unnecessary write)
        if (Files.mismatch(sourcePath, destPath) == -1L) {
            skipped++;  // identical content
        } else {
            // Content differs despite same mtime — treat as source-newer (copy)
            copyFile(sourcePath, destPath, dryRun, ...);
        }
    } else if (cmp > 0) {
        // Source is newer — check content first (D-03)
        if (Files.mismatch(sourcePath, destPath) == -1L) {
            skipped++;  // mtime advanced but content unchanged
        } else {
            copyFile(sourcePath, destPath, dryRun, ...);
        }
    } else {
        // Destination is newer — conflict (D-01)
        // Verify content actually differs before flagging (D-03)
        if (Files.mismatch(sourcePath, destPath) == -1L) {
            skipped++;  // destination is newer but content identical — safe to skip
        } else {
            conflicts++;
            conflictRecords.add(new SyncConflictRecord(
                relPath.toString().replace('\\', '/'),
                SyncConflictKind.CONTENT_MISMATCH,
                "destination newer than source"));
        }
    }
}
```

### Pattern 2: File Copy with REPLACE_EXISTING

**What:** Use `Files.copy()` with `StandardCopyOption.REPLACE_EXISTING` for sync overwrites.
**When to use:** When source is newer and content differs.

```java
// Source: JDK 21 java.nio.file.Files (standard library)
// Note: Files.copy() does NOT copy mtime by default (D-02)
// This is intentional: dest.mtime becomes "time of sync" so future
// comparisons correctly detect subsequent source changes.
Files.createDirectories(destPath.getParent());
Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
```

### Pattern 3: SyncResult Record (Modeled on GenerationResult)

**What:** Immutable record holding aggregate counts and per-file verbose lines.
**When to use:** Return from `DefaultSyncService.sync()`.

```java
// Source: GenerationResult pattern (already in project)
// src/main/java/org/saltations/generate/GenerationResult.java
public record SyncResult(
    int copied,
    int skipped,
    int failed,
    int conflicts,
    List<String> verboseLines,
    List<SyncConflictRecord> conflictRecords
) {
    public static SyncResult empty() {
        return new SyncResult(0, 0, 0, 0, List.of(), List.of());
    }
}
```

### Pattern 4: Traversal Structure (Mirror of GeneratorService)

**What:** destinations → mappings (sync: true only) → sources (templates: false only), apply glob/recurse/HiddenPathFilter.
**When to use:** The outer traversal loop in `DefaultSyncService.sync()`.

```java
// Source: GeneratorService.generate() (src/main/java/org/saltations/generate/GeneratorService.java)
ViracochaConfig config = configService.load();
DestinationEntry dest = config.getDestinations().stream()
    .filter(d -> d.getName().equals(destinationName))
    .findFirst()
    .orElseThrow(() -> new IllegalArgumentException(
        "Destination '" + destinationName + "' not found."));

// Tilde expansion (Phase 11 D-10 pattern)
String rawPath = dest.getPath();
String resolvedPath = rawPath.startsWith("~")
    ? System.getProperty("user.home") + rawPath.substring(1)
    : rawPath;
Path destRoot = Path.of(resolvedPath);

for (MappingEntry mapping : dest.getMappings()) {
    if (!mapping.isSync()) continue;  // SYN-01: only sync: true mappings

    SourceEntry source = config.getSources().stream()
        .filter(s -> s.getName().equals(mapping.getSourceRef()))
        .findFirst()
        .orElseThrow(...);

    if (source.isTemplates()) continue;  // D-04: skip template sources silently

    Path sourceRoot = Path.of(source.getPath());
    int maxDepth = mapping.isRecurse() ? Integer.MAX_VALUE : 1;

    List<Path> files;
    try (Stream<Path> stream = Files.walk(sourceRoot, maxDepth)) {
        files = stream
            .filter(Files::isRegularFile)
            .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(sourceRoot, p))
            .filter(p -> {
                String glob = mapping.getGlob();
                if (glob == null) return true;
                return GlobMatcher.matches(glob, sourceRoot.relativize(p));
            })
            .sorted()  // deterministic order for tests
            .collect(Collectors.toList());
    }
    // ... per-file timestamp logic
}
```

### Pattern 5: SyncCommand Integration (JSON output)

**What:** Use `new ObjectMapper()` for JSON serialization of `SyncResult`.
**When to use:** When `--json` flag is set.

```java
// Source: existing SyncCommand.call() pattern, SyncCommand.java line 59
// ObjectMapper is already imported — instantiate locally (no injection needed)
if (json) {
    ObjectMapper om = new ObjectMapper();
    spec.commandLine().getOut().println(om.writeValueAsString(result));
    return result.conflicts() > 0 || result.failed() > 0 ? 1 : 0;
}
```

### Pattern 6: Test Harness (Mirrors GenerateCommandTest / GeneratorServiceTest)

**What:** Plain JUnit 5 with `@TempDir`, inline `XdgPaths` stub, picocli `CommandLine` rooted at `SyncCommand`.
**When to use:** All sync tests. No `@MicronautTest` needed.

```java
// Source: GenerateCommandTest.java (src/test/java/org/saltations/generate/GenerateCommandTest.java)
@TempDir Path tempDir;

@BeforeEach
void setUp() throws Exception {
    XdgPaths xdgPaths = new XdgPaths() {
        @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
        @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
        @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
    };
    configService = new ConfigService(xdgPaths);
    configService.init();
    // ... wire up SyncService, SyncCommand
    cmd = new SyncCommand(syncService);
    commandLine = new CommandLine(cmd);
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    commandLine.setOut(new PrintWriter(stdout, true));
    commandLine.setErr(new PrintWriter(stderr, true));
}
```

### Anti-Patterns to Avoid

- **Copying `mtime` with `COPY_ATTRIBUTES`:** Do NOT use `StandardCopyOption.COPY_ATTRIBUTES` in sync. D-02 requires dest.mtime to reflect copy time so future comparisons work correctly.
- **Skipping `Files.mismatch()` check:** D-03 requires a content check before copying even when mtime says source is newer. This avoids unnecessary writes on systems that touch timestamps without changing content.
- **Checking `Files.mismatch() != -1L` for conflict without mtime first:** Mismatch alone is not the conflict signal. Conflict requires `dest.mtime > src.mtime AND content differs`.
- **Using `@MicronautTest`:** All generate tests use plain JUnit 5. Sync tests should follow the same pattern for speed and isolation.
- **Iterating `sub.getVerboseLines()` from `SyncSubscriptionResult`:** Those v2 classes are deleted. `SyncResult.verboseLines()` is the v3 equivalent.
- **Rooting `CommandLine` at `ViracochaCommand` in tests:** Tests root at `SyncCommand` directly, so `execute()` args are options only — NOT `"sync", "--destination-name"`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| File content comparison | Custom byte-read loop | `Files.mismatch(path1, path2)` | Returns -1L if identical, else index of first diff; handles large files efficiently with BufferedInputStream internally |
| Timestamp comparison | String/long comparison | `FileTime.compareTo(FileTime)` | Handles precision differences across OS; returns negative/zero/positive like Comparable |
| Recursive directory walk | Manual stack-based traversal | `Files.walk(root, maxDepth)` | Handles symlinks, depth limiting, and lazy iteration |
| File overwrite copy | Read+write loop | `Files.copy(src, dest, REPLACE_EXISTING)` | Atomic where OS supports it; handles large files without loading into memory |
| JSON serialization of `SyncResult` | Manual string building | Jackson `ObjectMapper.writeValueAsString()` | Already in classpath; handles Java records natively |
| POSIX path normalization | Platform switch | `path.toString().replace('\\', '/')` | Simple string operation; already established in `SyncConflictRecord` context (CONTEXT.md specifics) |

**Key insight:** The JDK 21 NIO API has all the building blocks. `Files.mismatch()` (added in Java 12) is the standard content-equality tool and eliminates any need for custom comparison logic.

## Runtime State Inventory

> Not applicable. This is a greenfield implementation phase (rewriting a stub). No rename/refactor of persisted identifiers is involved. The user-facing command name `vira sync` and config YAML structure are unchanged.

**Nothing found in any category** — verified by phase description and CONTEXT.md which shows only code-level changes (class rewrites, deletions, and interface redesign within the `sync` package).

## Common Pitfalls

### Pitfall 1: `Files.mismatch()` Return Value Semantics

**What goes wrong:** Code checks `Files.mismatch(a, b) == 0` instead of `== -1L`, missing all identical files.
**Why it happens:** Developers assume it returns 0 for "equal" like `compareTo()`.
**How to avoid:** `Files.mismatch()` returns `-1L` (negative one as a long) when files are identical. Any other return value is the byte offset of the first difference.
**Warning signs:** Tests where `skipped` count stays 0 even when source and destination have identical content.

### Pitfall 2: Forgetting `REPLACE_EXISTING` on Copy

**What goes wrong:** `Files.copy(src, dest)` throws `FileAlreadyExistsException` when the destination file already exists.
**Why it happens:** `GeneratorService` uses `Files.copy()` without `REPLACE_EXISTING` because generate uses skip-existing semantics. Sync must overwrite.
**How to avoid:** Always use `Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING)` in sync.
**Warning signs:** `FileAlreadyExistsException` in test output when sync runs on a pre-populated destination.

### Pitfall 3: Using `COPY_ATTRIBUTES` Breaks Future Conflict Detection

**What goes wrong:** Adding `StandardCopyOption.COPY_ATTRIBUTES` copies the source mtime to dest. On next run, `source.mtime == dest.mtime` for all files, so nothing gets synced even when source later changes.
**Why it happens:** D-02 is easy to misread as "we should preserve mtime."
**How to avoid:** D-02 explicitly states NOT to copy attributes. Let dest.mtime be the time of the sync operation.
**Warning signs:** After re-syncing an unchanged file, src and dest have the same mtime. If source is then modified, the timestamps won't differ because the gap was lost.

### Pitfall 4: Treating "Conflict" as Any Content Difference

**What goes wrong:** Flagging as conflict whenever `Files.mismatch() != -1L`, regardless of which side is newer.
**Why it happens:** The name "conflict" implies any disagreement.
**How to avoid:** Conflict = `dest.mtime > src.mtime AND content differs`. If `src.mtime > dest.mtime AND content differs`, that is a normal update — copy it. If content is identical regardless of mtime, skip.
**Warning signs:** Normal updates (source changed since last sync) being reported as conflicts.

### Pitfall 5: Template Mapping Skip Must Be by Source `templates` Flag, Not Mapping Flag

**What goes wrong:** Skipping based on a non-existent `mapping.templates` field, or always processing all mappings.
**Why it happens:** The `sync: true` flag on the mapping controls sync inclusion; the `templates: true` flag is on the `SourceEntry`, not the `MappingEntry`.
**How to avoid:** Check `mapping.isSync()` first (skip if false), then resolve source, then check `source.isTemplates()` (skip if true per D-04).
**Warning signs:** Template files being copied byte-for-byte (unexpanded `${variable}` in destination), or non-`sync: true` mappings being processed.

### Pitfall 6: Picocli `CommandLine` Root in Tests

**What goes wrong:** Test does `commandLine.execute("sync", "--destination-name", "dest")` and gets an error because "sync" is not a recognized subcommand when the root IS `SyncCommand`.
**Why it happens:** Tests for `GenerateCommand` root at `GenerateCommand` directly — `execute()` takes only flags.
**How to avoid:** Root `CommandLine` at `new CommandLine(syncCommand)` and call `execute("--destination-name", "dest")`.
**Warning signs:** Picocli `UnmatchedArgumentException` for "sync" in test output.

### Pitfall 7: `SyncConflictRecord` Jackson Serialization with Record

**What goes wrong:** Jackson cannot serialize a Java record if it lacks annotations or a no-arg constructor.
**Why it happens:** The existing `SyncConflictRecord` is a `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` Lombok class. If it is converted to a Java record, Jackson 2.x without `jackson-module-parameter-names` may fail to serialize/deserialize.
**How to avoid:** Keep `SyncConflictRecord` as a Lombok `@Data` class (with `@NoArgsConstructor`/`@AllArgsConstructor`) since that is the established pattern in this package. Only `SyncResult` itself is a Java record (like `GenerationResult`). Alternatively, keep `SyncConflictRecord` as-is and just remove the `subscriptionId` field.
**Warning signs:** `JsonMappingException: No suitable constructor found` at runtime.

## Code Examples

Verified patterns from existing project source:

### `Files.mismatch()` for Content Equality (JDK 12+)

```java
// Returns -1L if files are identical; first differing byte offset otherwise
long mismatch = Files.mismatch(sourcePath, destPath);
boolean identical = (mismatch == -1L);
```

### `FileTime` Comparison

```java
// Source: JDK 21 javadoc (java.nio.file.attribute.FileTime)
FileTime srcMtime = Files.getLastModifiedTime(sourcePath);
FileTime dstMtime = Files.getLastModifiedTime(destPath);
int cmp = srcMtime.compareTo(dstMtime);
// cmp < 0: source older; cmp == 0: same; cmp > 0: source newer
```

### POSIX Relative Path String (for SyncConflictRecord)

```java
// Per CONTEXT.md specifics section
Path relPath = sourceRoot.relativize(sourcePath);
String posixRelPath = relPath.toString().replace('\\', '/');
```

### SyncResult as Java Record (mirrors GenerationResult)

```java
// Source: GenerationResult.java pattern
// Mutable accumulators during traversal; convert to record at end
List<String> verboseLines = new ArrayList<>();
List<SyncConflictRecord> conflictRecords = new ArrayList<>();
int copied = 0, skipped = 0, failed = 0, conflicts = 0;
// ... traversal loop ...
return new SyncResult(copied, skipped, failed, conflicts,
    Collections.unmodifiableList(verboseLines),
    Collections.unmodifiableList(conflictRecords));
```

### Jackson JSON Serialization (already in SyncCommand)

```java
// Source: SyncCommand.java line 59 — existing pattern
ObjectMapper om = new ObjectMapper();
spec.commandLine().getOut().println(om.writeValueAsString(result));
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| v2 `SyncEngineResult` + `SyncSubscriptionResult` (Lombok @Data) | `SyncResult` Java record (like `GenerationResult`) | Phase 12 | Simpler, immutable, no boilerplate |
| v2 `SyncService.syncProject(projectName, subscriptionId, ...)` | `SyncService.sync(destinationName, dryRun, verbose)` | Phase 12 | v3 destinations model; drops subscription concept |
| v2 `SyncCommand` with `--mapping-id` | `SyncCommand` without `--mapping-id`, `--destination-name` required | Phase 12 | Consistent with `GenerateCommand` |

**Deprecated/outdated:**
- `SyncEngineResult`: replaced by `SyncResult` record
- `SyncSubscriptionResult`: replaced by inline fields in `SyncResult`
- `SyncService.syncProject()`: replaced by `SyncService.sync()`
- `SyncCommand` option `--mapping-id`: removed (v2 artifact with no SYN requirement)
- `SyncConflictRecord.subscriptionId` field: removed (subscription model eliminated)

## Open Questions

1. **`SyncResult` Jackson Serialization of Java record**
   - What we know: `GenerationResult` is a Java record but is NOT serialized to JSON. `SyncResult` will be serialized via `--json`.
   - What's unclear: Does the existing Jackson version in the project (via `micronaut-serde-jackson`) support Java record serialization without additional modules?
   - Recommendation: Keep `SyncResult` as a Java record (immutable, clean). Jackson 2.12+ supports records natively for serialization. If deserialization is needed (it isn't for this use case — we only serialize OUT), it would require `jackson-module-parameter-names`. Since we only write JSON, not parse it, this is safe.

2. **Destination directory non-existence handling**
   - What we know: `GeneratorService` prompts user if destination doesn't exist and creates it. Sync operates on existing destinations (files must already be there to detect mtime).
   - What's unclear: What should sync do if the destination directory doesn't exist at all?
   - Recommendation: If `destRoot` doesn't exist, treat as a no-op (0 copied, 0 skipped, 0 conflicts) since there's nothing to conflict with. Alternatively, could treat missing destination files as "copy all" — this is consistent with D-01 ("dest file does not exist → copy"). The planner should choose: either fail fast with exit 1 and a clear message, or auto-create and copy all. Leaning toward auto-create (consistent with GeneratorService silent auto-create of subdirectories).

## Environment Availability

Step 2.6: SKIPPED — Phase 12 is a pure Java source code change with no external tool dependencies. JDK 21 and Maven are already confirmed available from the existing build (`./mvnw test` passes with 148 tests in BUILD SUCCESS).

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) 5.x |
| Config file | `pom.xml` — surefire plugin configured |
| Quick run command | `./mvnw test -pl . -Dtest="DefaultSyncServiceTest,SyncCommandTest" -q` |
| Full suite command | `./mvnw test -q` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SYN-01 | Copies changed source files (newer mtime, different content) to destination for `sync: true` mappings | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncCopiesChangedFilesToDestination" -q` | Wave 0 |
| SYN-01 | Skips files where content is identical despite mtime diff (D-03) | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncSkipsContentIdenticalFiles" -q` | Wave 0 |
| SYN-01 | Skips mappings with `sync: false` | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncIgnoresNonSyncMappings" -q` | Wave 0 |
| SYN-01 | Skips template sources silently (D-04) | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncSkipsTemplateSources" -q` | Wave 0 |
| SYN-02 | Detects conflict (dest newer, content differs) → conflicts count increments | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncDetectsConflictWhenDestNewer"` | Wave 0 |
| SYN-02 | No conflict when dest newer but content identical | unit | `./mvnw test -Dtest="DefaultSyncServiceTest#syncNoConflictWhenContentIdentical" -q` | Wave 0 |
| SYN-02 | Conflict causes exit code 1 at command level (D-12) | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandReturnsExitOneOnConflict" -q` | Wave 0 |
| SYN-03 | `--destination-name` required; missing → exit 2 | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandRequiresDestinationName" -q` | Wave 0 |
| SYN-03 | `--destination-name` routes to correct destination | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandWithDestinationNameRoutes" -q` | Wave 0 |
| SYN-04 | `--dry-run` reports what would be copied without writing | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandDryRunReportsWithoutWriting" -q` | Wave 0 |
| SYN-05 | `--verbose` prints per-file Copied/Skipped/Conflict lines | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandVerbosePrintsPerFileLines" -q` | Wave 0 |
| SYN-06 | `--json` outputs machine-readable SyncResult JSON | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandJsonOutputsMachineReadable" -q` | Wave 0 |
| SYN-07 | Summary line always printed: `"Copied: N, Skipped: N, Failed: N, Conflicts: N"` | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandSummaryLineAlwaysPrinted" -q` | Wave 0 |

### Sampling Rate

- **Per task commit:** `./mvnw test -Dtest="DefaultSyncServiceTest,SyncCommandTest" -q`
- **Per wave merge:** `./mvnw test -q` (full 148+ test suite)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/org/saltations/sync/DefaultSyncServiceTest.java` — covers SYN-01, SYN-02
- [ ] `src/test/java/org/saltations/sync/SyncCommandTest.java` — covers SYN-02 exit code, SYN-03, SYN-04, SYN-05, SYN-06, SYN-07

*(No framework install needed — JUnit 5 already configured in pom.xml)*

## Project Constraints (from CLAUDE.md)

The following directives from `CLAUDE.md` apply to Phase 12 planning and implementation:

| Directive | Impact on Phase 12 |
|-----------|-------------------|
| **Tech Stack locked**: JDK 21, Micronaut DI, picocli, Lombok, Freemarker, jackson-dataformat-yaml, Logback | No new libraries. All sync logic uses JDK 21 NIO. Jackson already present. |
| **Config format: YAML only** | Not applicable — sync reads from config but does not write new config entries |
| **Regeneration must never overwrite existing workspace files (skip-existing)** | Does NOT apply to sync. Sync uses `REPLACE_EXISTING` semantics by design. The constraint is for `generate` only. |
| **Scope: Local filesystem only, no network, no Git** | Sync is local filesystem only — compliant |
| **Use GSD workflow before file edits** | Planning artifacts must be complete before implementation begins |
| **Naming patterns**: PascalCase classes, `Test` suffix for tests, `testXxx` method prefix | `DefaultSyncServiceTest.java`, `SyncCommandTest.java`; methods like `syncCopiesChangedFiles()` |
| **Constructor injection with `@Inject`** | `DefaultSyncService(@Inject ConfigService)` — established pattern |
| **`@Singleton` on service and command classes** | `DefaultSyncService` and `SyncCommand` both `@Singleton` |
| **`Callable<Integer>`** for commands, exit codes 0/1/2 | `SyncCommand implements Callable<Integer>`; D-12 exit codes |
| **`spec.commandLine().getOut()` / `.getErr()`** for command output | Already in `SyncCommand` — maintain this pattern |
| **Try-with-resources for `Files.walk()`** | `try (Stream<Path> stream = Files.walk(...))` — same as `GeneratorService` |
| **Sorted traversal for test determinism** | `.sorted()` on file stream — established in Phase 11 |

## Sources

### Primary (HIGH confidence)

- JDK 21 `java.nio.file.Files` API — `mismatch()`, `getLastModifiedTime()`, `copy()`, `walk()` — verified by reading existing `GeneratorService.java` which uses `Files.copy()`, `Files.walk()`, `Files.isRegularFile()`
- `GeneratorService.java` (project source) — canonical v3 traversal pattern; direct code read
- `GenerationResult.java` (project source) — canonical record model for `SyncResult`; direct code read
- `GenerateCommand.java` (project source) — canonical command pattern; direct code read
- `GenerateCommandTest.java` (project source) — canonical test pattern; direct code read
- `SyncCommand.java` (project source) — existing command shell; direct code read
- `SyncConflictRecord.java`, `SyncConflictKind.java` (project source) — direct code read
- `12-CONTEXT.md` (project planning) — locked decisions D-01 through D-16

### Secondary (MEDIUM confidence)

- `./mvnw test` output — 148 tests, BUILD SUCCESS — confirms baseline state
- Jackson 2.12+ Java record support — well-documented; confirmed by reading existing project use of `ObjectMapper` in `ConfigService.java` and `SyncCommand.java`

### Tertiary (LOW confidence)

- None — all findings are from project source files or JDK standard library documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already present in project; verified by reading pom.xml and existing source files
- Architecture patterns: HIGH — directly modeled on `GeneratorService` and `GenerateCommand` (fully implemented in Phase 11)
- Pitfalls: HIGH — derived from reading the existing code, JDK API semantics, and CONTEXT.md decisions
- Test patterns: HIGH — directly modeled on `GeneratorServiceTest` and `GenerateCommandTest`

**Research date:** 2026-05-10
**Valid until:** 2026-06-10 (stable standard library domain; project patterns are fixed)
