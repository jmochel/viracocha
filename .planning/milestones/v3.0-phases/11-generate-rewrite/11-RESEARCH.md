# Phase 11: Generate Rewrite - Research

**Researched:** 2026-05-10
**Domain:** Java NIO2 file traversal, Apache Freemarker template expansion, picocli CLI — GeneratorService v3 implementation
**Confidence:** HIGH

## Summary

Phase 11 is a focused implementation phase: fill in the `GeneratorService.generate()` stub using exclusively existing infrastructure. Every dependency already exists and is tested: `ConfigService`, `PathExpander`, `GlobMatcher`, `HiddenPathFilter`, `GenerationResult`, `GenerateCommand`, and the model POJOs (`ViracochaConfig`, `DestinationEntry`, `MappingEntry`, `SourceEntry`). The codebase is green (136 tests, 0 failures). The only new code is the traversal loop inside `GeneratorService`.

The core algorithm is: load config → find destination by name → expand tilde in destination path → check destination directory exists (prompt if not) → for each mapping, resolve source, walk source directory (filtered by `recurse`, `glob`, `HiddenPathFilter`) → for each qualifying file, compute destination path (expanding path segments if `templates: true`) → skip if destination file already exists, byte-copy or Freemarker-expand content otherwise. Accumulate counts and verbose lines in a `GenerationResult`.

Test strategy mirrors Phase 10/Phase 9: plain JUnit 5 with inline `XdgPaths` stubs and `@TempDir` for both `GeneratorServiceTest` (service unit/integration tests) and `GenerateCommandTest` (command integration tests through `CommandLine.execute()`). One dedicated integration test with a binary fixture file is required per STATE.md concern.

**Primary recommendation:** Implement `GeneratorService.generate()` with `Files.walk()` + sorted stream + `Files.copy()` for binary and Freemarker `StringTemplateLoader` for template content. Reuse `PathExpander.expandSegment()` for path segment expansion. No new dependencies needed.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** When `recurse: true` and a glob is set, pass the full relative path from source root to `GlobMatcher.matches()` (e.g., `docs/api/readme.md`). This enables `**/*.md` patterns to match across subdirectories.
- **D-02:** When `recurse: false` and a glob is set, walk only the immediate source directory (depth 0). Use the relative path (= filename at depth 0) consistently for uniform API.
- **D-03:** `glob: null` means no filter — all files are selected regardless of `recurse` setting.
- **D-04:** `HiddenPathFilter.hasHiddenPathSegment()` applies to all source directory walks.
- **D-05:** When the top-level destination path does not exist, prompt the user: `Destination /path/to/dest does not exist. Create it? [y/N]` Written to stdout, waits for user input.
- **D-06:** If user responds `y` or `Y`, create the top-level destination directory using `Files.createDirectories` then proceed.
- **D-07:** If user responds anything other than `y`/`Y` (including Enter/empty), exit with code 0.
- **D-08:** In `--dry-run` mode, skip the prompt. Report `"Would create: <path>"` on stdout and continue the dry-run simulation.
- **D-09:** Sub-directories inside an existing destination are auto-created silently. Only the top-level destination root triggers confirmation.
- **D-10:** Expand `~` at the start of a destination path to `System.getProperty("user.home")` in `GeneratorService` before any filesystem operations. Stored path strings are not modified — expansion is runtime-only.
- **D-11:** For sources with `templates: false` — byte-copy all files using `Files.copy()`. No string processing.
- **D-12:** For sources with `templates: true` — use `PathExpander.expandSegment()` on each path segment, and Freemarker template processing on file content. Both use the destination's `parameters` map.
- **D-13:** Per-mapping parameter overrides (`MappingEntry.params`) are NOT used for template expansion in this phase. Destination-level parameters only.
- **D-14:** `--destination-name` is effectively required (exit 2 with `"Missing required option: '--destination-name'"` if omitted). Preserve current `GenerateCommand` behavior unchanged.
- **D-15:** Summary line always written to stdout: `"Generated: N files, Skipped: M files, Failed: K files"` — existing format, unchanged.
- **D-16:** `--verbose`: one line per file action: `"Created <dest-path>"`, `"Skipped <dest-path>"`, `"Failed <dest-path>"`. Written to stdout before the summary line.
- **D-17:** `--dry-run`: emit planned `"Would create <dest-path>"` lines (and `"Would create: <dest-path>"` for destination directory if applicable), but write no files. Summary line still printed at end.

### Claude's Discretion

- Freemarker `Configuration` lifecycle — a new instance per generate run (or shared) is an implementation detail.
- Exact ordering of file processing within a mapping (sorted paths recommended for test determinism).
- Whether dry-run summary uses the same `"Generated: / Skipped: / Failed:"` format or a `"Would generate:"` variant.
- Exception wrapping strategy for `TemplateException` and `IOException` within the traversal loop.

### Deferred Ideas (OUT OF SCOPE)

- `--destination-name` as optional (generate all destinations) — not selected; current behavior preserved.
- Per-mapping parameter overrides — `MappingEntry.params` field exists but is explicitly out of scope per REQUIREMENTS.md.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| GEN-01 | `vira generate` traverses destinations → mappings → sources, applies glob/recurse filtering, and writes files to the destination path | `GeneratorService.generate()` uses `Files.walk()` with depth control; `GlobMatcher.matches()` for filtering; `HiddenPathFilter.hasHiddenPathSegment()` to skip dotfiles |
| GEN-02 | `vira generate` skips destination files that already exist | `Files.exists(destFile)` check before any write; increment `skipped` counter |
| GEN-03 | `vira generate` expands Freemarker templates in both path segments and file content for sources with `templates: true` | `PathExpander.expandSegment()` per path segment; Freemarker `StringTemplateLoader` for content; destination `parameters` map as data model |
| GEN-04 | `vira generate` uses binary byte copy (not string read) for sources with `templates: false`, preserving non-text files | `Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING)` — but only reaches copy because `Files.exists()` check prevents overwrite; or `Files.copy(sourcePath, destPath)` with no REPLACE option (fails if exists) |
| GEN-05 | `vira generate` accepts `--destination-name` to target a single destination | Already wired in `GenerateCommand`; `GeneratorService.generate(destinationName, ...)` receives the name |
| GEN-06 | `vira generate` supports `--dry-run` (reports actions without writing files) | `dryRun` parameter; skip filesystem writes, emit `"Would create ..."` lines, still count and return `GenerationResult` |
| GEN-07 | `vira generate` supports `--verbose` (prints per-file action lines) | `verbose` parameter; `GeneratorService` populates `verboseLines` list in `GenerationResult`; `GenerateCommand` already prints them |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Apache Freemarker | 2.3.34 | Template expansion for file content and path segments | Already in pom.xml; `PathExpander` uses it for segment expansion |
| JDK NIO2 (`java.nio.file`) | 21 | File traversal, copy, directory creation | Standard library; `Files.walk()`, `Files.copy()`, `Files.createDirectories()` |
| picocli | (via Micronaut BOM) | CLI command execution; `spec.commandLine().getOut()` for output | Already wired in `GenerateCommand` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `GlobMatcher` (project) | — | Glob pattern filtering on relative paths | Called per file during source walk when `mapping.getGlob() != null` |
| `HiddenPathFilter` (project) | — | Skip dotfile paths during source walk | Always applied during `Files.walk()` |
| `PathExpander` (project) | — | Expand Freemarker `${var}` in path segments | Applied per path segment when `source.isTemplates()` is true |

No new dependencies are needed. All tools are already on the classpath.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/org/saltations/generate/
├── GenerateCommand.java      # CLI shell — no changes needed
├── GenerationResult.java     # Record — no changes needed
├── GeneratorService.java     # IMPLEMENT: v3 traversal logic goes here
└── PathExpander.java         # Existing singleton — use as-is
```

### Pattern 1: Source Directory Walk (recurse flag controls depth)
**What:** Walk source directory with `Files.walk()`, filter hidden paths, filter by glob.
**When to use:** For every mapping in the target destination.
```java
// Source: JDK 21 java.nio.file.Files.walk docs
int maxDepth = mapping.isRecurse() ? Integer.MAX_VALUE : 1;
try (Stream<Path> stream = Files.walk(sourceRoot, maxDepth)) {
    List<Path> files = stream
        .filter(Files::isRegularFile)
        .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(sourceRoot, p))
        .filter(p -> {
            String glob = mapping.getGlob();
            if (glob == null) return true;
            Path rel = sourceRoot.relativize(p);
            return GlobMatcher.matches(glob, rel);
        })
        .sorted()  // deterministic ordering for testability
        .collect(Collectors.toList());
    // ... process each file
}
```

### Pattern 2: Destination Path Computation (with optional template expansion)
**What:** Mirror source relative path under destination root, expanding path segments for template sources.
**When to use:** After selecting a qualifying source file.
```java
// Source: verified against PathExpander.expandSegment() implementation
Path relPath = sourceRoot.relativize(sourcePath);
Path destPath = destRoot;
Map<String, String> params = dest.getParameters();
for (int i = 0; i < relPath.getNameCount(); i++) {
    String seg = relPath.getName(i).toString();
    String expanded = source.isTemplates()
        ? pathExpander.expandSegment(seg, params)
        : seg;
    destPath = destPath.resolve(expanded);
}
```

### Pattern 3: File Write Dispatch (templates vs binary)
**What:** Choose between Freemarker content expansion and `Files.copy()` based on `source.isTemplates()`.
**When to use:** After computing destination path and confirming it does not exist.
```java
// Source: verified against PathExpander.expandSegment() and JDK Files.copy docs
Files.createDirectories(destPath.getParent());
if (source.isTemplates()) {
    String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
    String expanded = pathExpander.expandSegment(content, dest.getParameters());
    Files.writeString(destPath, expanded, StandardCharsets.UTF_8);
} else {
    Files.copy(sourcePath, destPath); // no REPLACE_EXISTING — destination known to not exist
}
```
NOTE: `Files.copy()` without `REPLACE_EXISTING` is appropriate here because the skip-existing check already confirmed the destination does not exist. This avoids accidental overwrites if logic diverges.

### Pattern 4: Tilde Expansion
**What:** Expand leading `~` to `System.getProperty("user.home")` at runtime.
**When to use:** Once per `generate()` call, before any filesystem operations on the destination path.
```java
// Source: D-10 decision — runtime-only, does not mutate stored config
String rawPath = dest.getPath();
String resolvedPath = rawPath.startsWith("~")
    ? System.getProperty("user.home") + rawPath.substring(1)
    : rawPath;
Path destRoot = Path.of(resolvedPath);
```

### Pattern 5: Missing Destination Directory Prompt
**What:** If top-level destination does not exist, write prompt to stdout, read from `System.in`, create or exit 0.
**When to use:** After tilde expansion, before the traversal loop.
```java
// Source: D-05 through D-09 decisions; flush is required to prevent buffering
if (!Files.exists(destRoot)) {
    if (dryRun) {
        out.println("Would create: " + destRoot);
    } else {
        out.print("Destination " + destRoot + " does not exist. Create it? [y/N] ");
        out.flush(); // critical: flush before reading stdin
        String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (answer == null || !answer.equalsIgnoreCase("y")) {
            return GenerationResult.empty();
        }
        Files.createDirectories(destRoot);
    }
}
```

### Pattern 6: Test Structure (CommandLine integration test)
**What:** Plain JUnit 5, inline `XdgPaths` stub, `@TempDir` for both source directories and config. Wire `GenerateCommand` via `new CommandLine(cmd)`.
**When to use:** For all `GenerateCommand` integration tests.
```java
// Source: established in Phase 9 SourceListCommandTest and Phase 10 DestinationServiceTest
@TempDir Path tempDir;

@BeforeEach void setUp() throws Exception {
    XdgPaths xdgPaths = new XdgPaths() {
        @Override public Path configFile() { return tempDir.resolve("viracocha/config.yaml"); }
        @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
        @Override public Path dataDir()    { return tempDir.resolve("share/viracocha"); }
    };
    ConfigService configService = new ConfigService(xdgPaths);
    configService.init();
    // ... set up source dirs, register sources and destinations ...
    PathExpander expander = new PathExpander();
    GeneratorService service = new GeneratorService(configService, expander);
    GenerateCommand cmd = new GenerateCommand(service);
    commandLine = new CommandLine(cmd);
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    commandLine.setOut(new PrintWriter(stdout, true));
    commandLine.setErr(new PrintWriter(stderr, true));
}
```

### Anti-Patterns to Avoid
- **Creating a new Freemarker `Configuration` per file:** Expensive. Create once per `generate()` call (or use `PathExpander` which already accepts a segment string through `StringTemplateLoader`). Note: `PathExpander.expandSegment()` already creates a `Configuration` per call — acceptable for now per discretion item, but caller should be aware.
- **Using `Files.copy()` with `REPLACE_EXISTING` for the main copy path:** This would silently overwrite existing files, violating GEN-02. The skip-exists check gates the copy call; no `REPLACE_EXISTING` needed.
- **Passing absolute paths to `GlobMatcher.matches()`:** GlobMatcher expects the relative path from source root. Passing an absolute path will cause glob patterns to not match. Always `sourceRoot.relativize(sourcePath)` before passing to `GlobMatcher`.
- **Reading binary files as strings:** `Files.readString()` on a binary file corrupts bytes. Only use string reads for template sources; use `Files.copy()` for binary sources.
- **Forgetting to flush stdout before reading stdin for the destination prompt:** Buffered output will not appear before the prompt hangs waiting for input.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Glob pattern matching | Custom regex or wildcard logic | `GlobMatcher.matches()` (already in infra/) | Wraps JDK `PathMatcher`; handles `**`, `?`, `[`, `+` literal correctly; tested in Phase 10 |
| Hidden file detection | Manual `.startsWith(".")` per segment | `HiddenPathFilter.hasHiddenPathSegment(root, path)` | Checks all segments in relative path, not just filename |
| Freemarker variable expansion in segments | Custom `${var}` string replacement | `PathExpander.expandSegment(segment, model)` | Full Freemarker semantics including modifiers and error reporting |
| File traversal with depth limit | Recursive walk methods | `Files.walk(root, maxDepth)` | JDK built-in; honors max depth; returns `Stream<Path>` composable with filters |
| Directory creation (nested) | `mkdir()` chains | `Files.createDirectories(path)` | Creates all intermediate directories atomically; idempotent |

**Key insight:** Every infrastructure piece for this phase was deliberately built in prior phases (4, 8, 9, 10). The only work is the wiring — composing them in `GeneratorService.generate()`.

## Common Pitfalls

### Pitfall 1: Depth-0 Walk Includes the Root Directory Itself
**What goes wrong:** `Files.walk(sourceRoot, 1)` yields `sourceRoot` itself as the first entry (depth 0), then immediate children at depth 1. If not filtered with `Files::isRegularFile`, the root directory ends up in the file list.
**Why it happens:** `Files.walk` with maxDepth=1 means "root (depth 0) + its direct children (depth 1)". The root is always included.
**How to avoid:** Always add `.filter(Files::isRegularFile)` to the stream. This removes the root directory and any sub-directories from the file list.
**Warning signs:** `dest.getParent()` is null or points to wrong directory during path resolution.

### Pitfall 2: GlobMatcher Receives Absolute Path Instead of Relative
**What goes wrong:** `GlobMatcher.matches("**/*.md", absolutePath)` returns false because JDK `PathMatcher` uses the exact path string — a leading `/home/user/...` will not match a pattern like `**/*.md`.
**Why it happens:** Forgetting to relativize before filtering.
**How to avoid:** Always compute `Path rel = sourceRoot.relativize(p)` and pass `rel` to `GlobMatcher.matches()`.
**Warning signs:** All files are being filtered out even though the glob should match them.

### Pitfall 3: Freemarker Missing Variable Throws, Corrupts Run Count
**What goes wrong:** `PathExpander.expandSegment()` throws `IllegalArgumentException` when a template variable is in the path segment or file content but has no value in the destination's `parameters` map. If uncaught, this terminates the entire run mid-traversal.
**Why it happens:** `PathExpander` wraps `TemplateExceptionHandler.RETHROW_HANDLER` which re-throws on any undefined variable.
**How to avoid:** Catch `IllegalArgumentException` from `expandSegment()` calls inside the traversal loop. Increment the `failed` counter and add a verbose line. Log the error. Do not rethrow — process remaining files.
**Warning signs:** A single template file with a missing variable aborts generation for all remaining files.

### Pitfall 4: Binary File Corruption via String Read
**What goes wrong:** If the `templates: false` branch accidentally calls `Files.readString()` on a PNG or binary file, byte values outside UTF-8 sequences are replaced with replacement characters (`�`), corrupting the output.
**Why it happens:** Confusion between template and binary code paths.
**How to avoid:** For `templates: false` sources, always use `Files.copy(sourcePath, destPath)` — no string reading at all.
**Warning signs:** STATE.md flagged this: "Binary file copy (GEN-04) needs a dedicated integration test with a non-text file to verify no corruption." Include a binary fixture in tests.

### Pitfall 5: Destination Prompt Hangs Without Flush
**What goes wrong:** On some terminals/CI environments, `out.print("Destination ... [y/N] ")` does not appear before the program blocks reading `System.in`.
**Why it happens:** `PrintWriter` is line-buffered by default; partial lines without `\n` may not be flushed.
**How to avoid:** Call `out.flush()` explicitly after the prompt print, before reading stdin.
**Warning signs:** Test hangs; interactive terminal shows blank before prompt.

### Pitfall 6: Destination Not Found in Config Throws vs Returns
**What goes wrong:** If `destinationName` does not match any `DestinationEntry`, the service must signal this clearly. A null dereference in traversal is harder to debug than an explicit `IllegalArgumentException`.
**Why it happens:** `config.destinations.stream().filter(...)` returns an empty Optional.
**How to avoid:** If `Optional<DestinationEntry>` is empty, throw `IllegalArgumentException("Destination '<name>' not found.")`. `GenerateCommand` already catches `IllegalArgumentException` and returns exit 1.
**Warning signs:** NullPointerException during path resolution instead of a clear error message.

## Code Examples

Verified patterns from codebase inspection:

### Freemarker Content Expansion Using PathExpander
```java
// Source: PathExpander.expandSegment() — confirmed works for multiline file content too
// The segment parameter accepts full file content, not just short segments
String rawContent = Files.readString(sourcePath, StandardCharsets.UTF_8);
String expandedContent = pathExpander.expandSegment(rawContent, dest.getParameters());
Files.writeString(destPath, expandedContent, StandardCharsets.UTF_8);
```

### Silent Flush Pattern for Interactive Prompt (D-09)
```java
// Source: D-05 decision + Java BufferedReader.readLine() javadoc
PrintWriter out = spec.commandLine().getOut(); // use command's writer, not System.out
out.print("Destination " + destRoot + " does not exist. Create it? [y/N] ");
out.flush();
String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
```

### Binary Fixture in Test (required by STATE.md concern)
```java
// Pattern: write known bytes to temp file, run generate, read bytes back and assert equality
byte[] binaryData = new byte[]{0x00, (byte)0xFF, 0x1A, 0x2B, 0x3C};
Path binarySource = sourceDir.resolve("sample.bin");
Files.write(binarySource, binaryData);
// ... register source (templates:false), destination, mapping, run generate ...
byte[] copiedData = Files.readAllBytes(destDir.resolve("sample.bin"));
assertArrayEquals(binaryData, copiedData, "Binary file must be copied byte-for-byte without corruption");
```

### Sorted File Walk (deterministic test ordering)
```java
// Source: Phase 4 CONTEXT.md + Phase 10 CONTEXT.md specifics note
// Sort after collect to avoid closed-stream issues
List<Path> files = stream
    .filter(Files::isRegularFile)
    .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(sourceRoot, p))
    .sorted()  // Path.compareTo gives lexicographic determinism
    .collect(Collectors.toList());
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| v2 catalog/archetype/pattern model | v3 sources/destinations/mappings | Phase 8 redesign | `GeneratorService` stub replaces old generator entirely |
| Per-mapping parameter override merging | Destination-level parameters only | Phase 11 scope decision (D-13) | Simpler data model; `MappingEntry.params` field not used for template expansion |
| `PatternEntry` path as glob root | `SourceEntry.path` as walk root with `MappingEntry.glob` as filter | v3 redesign | Cleaner separation of "where to find files" from "which files to include" |

**Deprecated/outdated:**
- Old `GeneratorService.generate()` stub: throws `UnsupportedOperationException`. This is the sole target of Phase 11 — replace it entirely.
- `FreemarkerVariableExtractor` in `infra/`: still used by `SourceService` for `source add --templates`. Not used by `GeneratorService` directly, but serves as a reference for how directory walks are structured.

## Open Questions

1. **`GenerateCommand` stdin injection for testing the destination prompt**
   - What we know: `GenerateCommand` uses `spec.commandLine().getOut()` for prompt text. The prompt reads from `System.in` (a global static).
   - What's unclear: Direct `System.in` reading makes the destination-creation prompt path hard to test in isolation. Options are: (a) accept the static `System.in` read and test indirectly by not triggering the prompt path in most tests; (b) inject an `InputStream` parameter into `GeneratorService.generate()` for testability; (c) use `System.setIn()` in tests (works but less clean).
   - Recommendation: For Claude's Discretion — option (b) is cleanest for testability. A dedicated `testDestinationCreationPrompt` test can inject a `ByteArrayInputStream("y\n".getBytes())`. The CONTEXT.md code_context mentions "inject an InputStream for testability" as acceptable. The planner should specify an `InputStream` parameter or a separate overload.

2. **Freemarker Configuration per-call vs shared**
   - What we know: `PathExpander.expandSegment()` currently creates a new `Configuration` per call — verified by reading the source. This is under Claude's Discretion (CONTEXT.md).
   - What's unclear: For large source trees, creating a `Configuration` per file segment is wasteful. However, `Configuration` is not thread-safe and is designed to be reused within a single thread.
   - Recommendation: Create one `Configuration` instance per `generate()` call, pass it to a helper method. This is an implementation detail that can be refactored within the service without touching the public API.

## Environment Availability

Step 2.6: SKIPPED (no external dependencies identified — this phase is purely code/config changes using already-installed JDK 21 and Freemarker 2.3.34)

Verified baseline:
- JDK 21.0.2 available at `/home/jmochel/.sdkman/candidates/java/current/bin/java`
- Maven 3.8.7 available at `/usr/bin/mvn`
- Full test suite: 136 tests, 0 failures, 0 errors (confirmed with `./mvnw test`)

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) — `junit-jupiter-api` + `junit-jupiter-engine` via Micronaut BOM |
| Config file | none — Maven Surefire picks up JUnit 5 automatically |
| Quick run command | `./mvnw test -Dtest="GeneratorServiceTest,GenerateCommandTest" -q` |
| Full suite command | `./mvnw test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GEN-01 | Files traversed and written to destination | integration | `./mvnw test -Dtest="GeneratorServiceTest#*flat*,GeneratorServiceTest#*recurse*"` | Wave 0 |
| GEN-02 | Existing destination files are skipped | integration | `./mvnw test -Dtest="GeneratorServiceTest#*skipExisting*"` | Wave 0 |
| GEN-03 | Template sources expand path segments and content | integration | `./mvnw test -Dtest="GeneratorServiceTest#*template*"` | Wave 0 |
| GEN-04 | Binary files byte-copied intact | integration | `./mvnw test -Dtest="GeneratorServiceTest#*binary*"` | Wave 0 |
| GEN-05 | `--destination-name` routes to correct destination | integration | `./mvnw test -Dtest="GenerateCommandTest#*destinationName*"` | Wave 0 |
| GEN-06 | `--dry-run` reports actions without writing | integration | `./mvnw test -Dtest="GenerateCommandTest#*dryRun*"` | Wave 0 |
| GEN-07 | `--verbose` prints per-file lines | integration | `./mvnw test -Dtest="GenerateCommandTest#*verbose*"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./mvnw test -Dtest="GeneratorServiceTest,GenerateCommandTest" -q`
- **Per wave merge:** `./mvnw test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/org/saltations/generate/GeneratorServiceTest.java` — covers GEN-01 through GEN-04 (currently placeholder stub)
- [ ] `src/test/java/org/saltations/generate/GenerateCommandTest.java` — covers GEN-05 through GEN-07 (currently placeholder stub)
- [ ] Binary fixture file: `src/test/resources/fixtures/sample.bin` — required for GEN-04 binary copy test

## Sources

### Primary (HIGH confidence)
- Codebase inspection — `src/main/java/org/saltations/generate/GeneratorService.java` (stub), `GenerateCommand.java`, `GenerationResult.java`, `PathExpander.java` — read directly
- Codebase inspection — `src/main/java/org/saltations/infra/GlobMatcher.java`, `HiddenPathFilter.java`, `FreemarkerVariableExtractor.java` — read directly
- Codebase inspection — `src/main/java/org/saltations/model/DestinationEntry.java`, `MappingEntry.java`, `SourceEntry.java`, `ViracochaConfig.java` — read directly
- `./mvnw test` executed — 136 tests green, confirming baseline is stable
- `.planning/phases/11-generate-rewrite/11-CONTEXT.md` — locked decisions D-01 through D-17

### Secondary (MEDIUM confidence)
- `.planning/phases/04-workspace-generation/04-CONTEXT.md` — pattern established for `PathExpander`, skip-existing semantics, sorted traversal recommendation
- `.planning/phases/10-destination-mapping-commands/10-RESEARCH.md` — confirmed test structure pattern (plain JUnit 5 + `XdgPaths` stub + `@TempDir`)
- JDK 21 `java.nio.file.Files.walk()` docs — maxDepth behavior includes root at depth 0; verified against known implementation

### Tertiary (LOW confidence)
- None — all findings are based on direct codebase inspection or documented decisions.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all dependencies verified in pom.xml and running codebase
- Architecture: HIGH — all patterns derived from existing tested code in phases 9/10
- Pitfalls: HIGH for pitfalls 1-4 (derived from reading actual implementation); MEDIUM for pitfall 5 (buffering behavior is well-known but not reproduced locally)

**Research date:** 2026-05-10
**Valid until:** 2026-06-10 (stable domain — no external library churn expected)
