# Phase 10: Destination & Mapping Commands - Research

**Researched:** 2026-05-10
**Domain:** Java/picocli command implementation — destination CRUD and mapping management
**Confidence:** HIGH

## Summary

Phase 10 is a direct replication of the Phase 9 source command pattern. All the infrastructure already exists: `SourceService`, `SourceCommand`, `SourceAddCommand`, `SourceShowCommand`, `SourceListCommand`, `SourceRemoveCommand` are the precise templates to clone and adapt for destinations and mappings. The model POJOs (`DestinationEntry`, `MappingEntry`) are already fully defined with correct Lombok annotations and field defaults. `ConfigService` already reads and writes the `destinations` list via `ViracochaConfig`.

The new work in this phase is threefold: (1) create `DestinationService` replicating the `SourceService` CRUD pattern with destination-specific validation, (2) create seven command classes (`DestinationCommand` group + six subcommands), and (3) create `GlobMatcher` in `infra/` wrapping `FileSystems.getDefault().getPathMatcher("glob:" + pattern)`. The JDK glob `PathMatcher` treats `+` as a literal character (confirmed via official Java 21 docs — `+` is not in the set of glob special characters). All test infrastructure uses plain JUnit 5 with inline `XdgPaths` stubs and `@TempDir`, not `@MicronautTest`.

**Primary recommendation:** Clone-and-adapt from the source command package. Every pattern — service, command, test structure, output format, exception handling — is established and verified in Phase 9.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `DestinationCommand` group (`vira destination`, alias `dest`) with subcommands: `DestinationAddCommand`, `DestinationListCommand`, `DestinationShowCommand`, `DestinationRemoveCommand`, `DestinationAddMappingCommand`, `DestinationListMappingsCommand`, `DestinationRemoveMappingCommand`. `ViracochaCommand` adds `DestinationCommand.class` to its subcommands.
- **D-02:** `DestinationCommand` alias is `dest`.
- **D-03:** `DestinationService` singleton in new `destination/` package. Thin command wrappers. Methods: `addDestination`, `listDestinations`, `getDestination`, `removeDestination`, `addMapping`, `listMappings`, `removeMapping`.
- **D-04:** `destination add` validates only the `..` traversal guard (raw string check). No existence check on path.
- **D-05:** Traversal error: `"Path must not contain '..': <path>"`
- **D-06:** Duplicate name error: `"Destination '<name>' already exists."`
- **D-07:** Store path as-is (no normalization). Tilde paths stored as-is.
- **D-08:** Plain output: name + path, aligned columns, no header.
- **D-09:** `--json` flag: JSONL, one JSON object per line.
- **D-10:** Plain-text multi-line `destination show` output format with Name, Path, Parameters (optional), Mappings sections.
- **D-11:** `Parameters:` section omitted entirely when parameters map is empty.
- **D-12:** When no mappings: `Mappings: (none)`.
- **D-13:** When glob is null: `Glob:    (all files)`.
- **D-14:** `--json` flag on show: single JSON object for full `DestinationEntry`.
- **D-15:** CLI: `vira destination add-mapping DEST-NAME --source SOURCE-NAME [--glob PATTERN] [--recurse] [--sync]`
- **D-16:** Success message: `"Mapping added to destination '<name>'."`
- **D-17:** Error if destination not found: `"Destination '<name>' not found."`
- **D-18:** Error if source not found: `"Source '<sourceRef>' not found."`
- **D-19:** `list-mappings NAME`: numbered blocks, same format as show mappings. If none: `"No mappings for destination '<name>'."`
- **D-20:** `--json` on list-mappings: JSONL of `MappingEntry` objects.
- **D-21:** `remove-mapping NAME INDEX`: both positional, INDEX 0-based integer.
- **D-22:** Success: `"Mapping <INDEX> removed from destination '<name>'."`
- **D-23:** Error destination not found: `"Destination '<name>' not found."` Exit 1.
- **D-24:** Error index out of range: `"Mapping index <N> out of range (destination has <M> mappings)."` Exit 1.
- **D-25:** `GlobMatcher` lives in `infra/` alongside `HiddenPathFilter`.
- **D-26:** `GlobMatcher` wraps `FileSystems.getDefault().getPathMatcher("glob:" + pattern)`. Unit tests verify `+` is literal (MAP-05).
- **D-27:** Destination not found message: `"Destination '<name>' not found."`
- **D-28:** All commands: exit 0 on success, exit 1 on any error.
- **D-29:** `@MicronautTest` + `@TempDir` for command integration tests. Pure-logic unit tests (GlobMatcher, DestinationService validation) use plain JUnit 5. Every new class needs at least one test.

### Claude's Discretion

- Exact column alignment implementation in `destination list`
- Whether `DestinationService.removeMapping` returns a boolean or throws a checked exception for out-of-range index
- Whether `addMapping` returns `MappingEntry` or void
- Exact `GlobMatcher` method signature
- Mapping `params` field: `add-mapping` does not expose `--params` flag in this phase

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DEST-01 | User can add a named destination workspace path with `--name` and `--path` (`vira destination add`) | `DestinationAddCommand` clones `SourceAddCommand` pattern; service `addDestination` validates `..` and duplicate names |
| DEST-02 | User can list all registered destinations in plain or JSON output (`vira destination list [--json]`) | `DestinationListCommand` clones `SourceListCommand` JSONL pattern; aligned columns via `String.format` |
| DEST-03 | User can view a destination's full details — name, path, parameters, and all mappings (`vira destination show NAME`) | `DestinationShowCommand` extends `SourceShowCommand` pattern with nested parameters and mappings blocks |
| DEST-04 | User can remove a destination by name (`vira destination remove NAME`) | `DestinationRemoveCommand` clones `SourceRemoveCommand`; service returns boolean |
| DEST-05 | Destination add rejects duplicate destination names with a clear error | `addDestination` checks `config.destinations` for name equality before adding |
| DEST-06 | Destination add rejects paths containing `..` directory traversal sequences | Raw string `contains("..")` check before any `Path.of()` call — same technique as `SourceService` |
| MAP-01 | User can add a mapping to a destination specifying source name, optional glob, recurse, and sync | `DestinationAddMappingCommand` with `@Parameters(index="0")` dest name, `--source` (required), `--glob`, `--recurse`, `--sync` |
| MAP-02 | User can list all mappings for a destination (`vira destination list-mappings NAME`) | `DestinationListMappingsCommand` — numbered blocks from `listMappings(destName)` |
| MAP-03 | User can remove a mapping from a destination by index (`vira destination remove-mapping NAME INDEX`) | `DestinationRemoveMappingCommand` — two positional args; service validates index bounds |
| MAP-04 | Mapping add validates that the referenced source name exists in the config | `addMapping` reads `config.sources` to check `sourceRef` before appending `MappingEntry` |
| MAP-05 | `GlobMatcher` wraps JDK `FileSystem.getPathMatcher`; unit tests verify `+` is literal in glob patterns | JDK 21 glob: `+` is NOT a special character — it matches itself; confirmed via official Java 21 FileSystem docs |
</phase_requirements>

## Standard Stack

### Core (already in project — no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JDK 21 `java.nio.file.FileSystem` | 21.0.2 | `PathMatcher` via `getPathMatcher("glob:")` for `GlobMatcher` | Built-in, zero-dependency glob matching |
| picocli | managed by Micronaut 4.10.10 | `@Parameters(index="N")` for positional CLI args; `@Option` for flags | Already used in every command |
| Jackson `ObjectMapper` | managed by Micronaut 4.10.10 | JSON output — JSONL for list, single object for show | Already used in `SourceListCommand`, `SourceShowCommand` |
| Lombok `@Data @NoArgsConstructor @AllArgsConstructor` | managed by Micronaut 4.10.10 | POJOs for `DestinationEntry`, `MappingEntry` | Already on both model classes |
| JUnit 5 + `@TempDir` | managed by Micronaut 4.10.10 | Test isolation with temporary XdgPaths stub | Already used in all source command tests |

No new Maven dependencies are required for this phase.

**Installation:** None needed.

## Architecture Patterns

### Recommended Package Structure

```
src/main/java/org/saltations/
├── destination/              # New package (mirrors source/)
│   ├── DestinationCommand.java
│   ├── DestinationAddCommand.java
│   ├── DestinationListCommand.java
│   ├── DestinationShowCommand.java
│   ├── DestinationRemoveCommand.java
│   ├── DestinationAddMappingCommand.java
│   ├── DestinationListMappingsCommand.java
│   ├── DestinationRemoveMappingCommand.java
│   └── DestinationService.java
└── infra/
    ├── GlobMatcher.java      # New: wraps JDK PathMatcher
    ├── HiddenPathFilter.java  # Existing
    ├── FreemarkerVariableExtractor.java  # Existing
    └── XdgPaths.java          # Existing

src/test/java/org/saltations/
└── destination/              # New test package
    ├── DestinationServiceTest.java
    ├── DestinationAddCommandTest.java
    ├── DestinationListCommandTest.java
    ├── DestinationShowCommandTest.java
    ├── DestinationRemoveCommandTest.java
    ├── DestinationAddMappingCommandTest.java
    ├── DestinationListMappingsCommandTest.java
    └── DestinationRemoveMappingCommandTest.java
    infra/
    └── GlobMatcherTest.java  # New
```

### Pattern 1: DestinationService — CRUD + Mapping Operations

Replicates `SourceService` exactly, with additions for mapping management. Key differences from SourceService:

- `addDestination` does NOT check path existence (destinations may not exist at registration time per D-04/D-07)
- `addMapping` reads `config.sources` to validate `sourceRef` (cross-list dependency)
- `removeMapping` operates on an index, not a name

```java
// Source: existing SourceService.java pattern adapted for destinations
@Singleton
public class DestinationService {

    private final ConfigService configService;

    @Inject
    public DestinationService(ConfigService configService) {
        this.configService = configService;
    }

    public DestinationEntry addDestination(String name, String rawPath) throws IOException {
        // D-04/D-05: raw string traversal check BEFORE Path.of()
        if (rawPath.contains("..")) {
            throw new IllegalArgumentException("Path must not contain '..': " + rawPath);
        }
        ViracochaConfig config = configService.load();
        // D-06: duplicate name check
        boolean duplicate = config.getDestinations().stream()
            .anyMatch(d -> d.getName().equals(name));
        if (duplicate) {
            throw new IllegalArgumentException("Destination '" + name + "' already exists.");
        }
        // D-07: store path as-is
        DestinationEntry entry = new DestinationEntry(name, rawPath,
            new LinkedHashMap<>(), new ArrayList<>());
        config.getDestinations().add(entry);
        configService.save(config);
        return entry;
    }

    public void addMapping(String destName, String sourceRef, String glob,
                           boolean recurse, boolean sync) throws IOException {
        ViracochaConfig config = configService.load();
        // MAP-04: validate sourceRef exists
        boolean sourceExists = config.getSources().stream()
            .anyMatch(s -> s.getName().equals(sourceRef));
        if (!sourceExists) {
            throw new IllegalArgumentException("Source '" + sourceRef + "' not found.");
        }
        // Validate destination exists
        DestinationEntry dest = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination '" + destName + "' not found."));
        dest.getMappings().add(new MappingEntry(sourceRef, glob, recurse, sync,
            new LinkedHashMap<>()));
        configService.save(config);
    }

    public boolean removeMapping(String destName, int index) throws IOException {
        ViracochaConfig config = configService.load();
        Optional<DestinationEntry> opt = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destName))
            .findFirst();
        if (opt.isEmpty()) return false; // destination not found — caller prints D-27 error
        DestinationEntry dest = opt.get();
        if (index < 0 || index >= dest.getMappings().size()) {
            throw new IndexOutOfBoundsException(
                "Mapping index " + index + " out of range (destination has "
                + dest.getMappings().size() + " mappings).");
        }
        dest.getMappings().remove(index);
        configService.save(config);
        return true;
    }
    // listDestinations, getDestination, removeDestination: mirror SourceService
}
```

### Pattern 2: DestinationCommand Group — Mirror SourceCommand

```java
// Source: existing SourceCommand.java pattern
@Command(
    name = "destination",
    aliases = {"dest"},
    description = "Manage registered destination workspaces.",
    mixinStandardHelpOptions = true,
    subcommands = {
        DestinationAddCommand.class,
        DestinationListCommand.class,
        DestinationShowCommand.class,
        DestinationRemoveCommand.class,
        DestinationAddMappingCommand.class,
        DestinationListMappingsCommand.class,
        DestinationRemoveMappingCommand.class
    }
)
@Singleton
public class DestinationCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
```

### Pattern 3: ViracochaCommand — Add DestinationCommand to subcommands

```java
// Modify: src/main/java/org/saltations/ViracochaCommand.java
// Add import: org.saltations.destination.DestinationCommand
// Add to subcommands array: DestinationCommand.class
```

### Pattern 4: destination show — Nested Output Format

```java
// Source: SourceShowCommand.java adapted for nested parameters + mappings (D-10 to D-13)
out.println("Name:      " + entry.getName());
out.println("Path:      " + entry.getPath());
// D-11: Parameters block only when map is non-empty
if (!entry.getParameters().isEmpty()) {
    out.println("Parameters:");
    for (Map.Entry<String, String> e : entry.getParameters().entrySet()) {
        out.println("  " + e.getKey() + ": " + e.getValue());
    }
}
// D-12: Always show Mappings section
if (entry.getMappings().isEmpty()) {
    out.println("Mappings: (none)");
} else {
    for (int i = 0; i < entry.getMappings().size(); i++) {
        MappingEntry m = entry.getMappings().get(i);
        out.println("Mapping " + (i + 1) + ":");
        out.println("  Source:  " + m.getSourceRef());
        // D-13: null glob -> "(all files)"
        out.println("  Glob:    " + (m.getGlob() == null ? "(all files)" : m.getGlob()));
        out.println("  Recurse: " + m.isRecurse());
        out.println("  Sync:    " + m.isSync());
    }
}
```

### Pattern 5: GlobMatcher — infra/ Utility

```java
// Source: JDK 21 java.nio.file.FileSystem.getPathMatcher javadoc
// Location: src/main/java/org/saltations/infra/GlobMatcher.java
public final class GlobMatcher {

    private GlobMatcher() {}

    /**
     * Returns true if {@code path} matches the given glob pattern.
     * The + character is treated as a literal (not a regex quantifier) by the JDK glob engine.
     * @param glob glob pattern (without "glob:" prefix), e.g., "**\/*.md"
     * @param path absolute or relative path to test against
     */
    public static boolean matches(String glob, Path path) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(path);
    }
}
```

### Pattern 6: Test Structure — Plain JUnit 5, No @MicronautTest

Every command test uses this exact setup structure (copied from Phase 9):

```java
// Source: existing SourceAddCommandTest.java pattern
@TempDir Path tempDir;

@BeforeEach void setUp() throws Exception {
    XdgPaths xdgPaths = new XdgPaths() {
        @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
        @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
        @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
    };
    ConfigService configService = new ConfigService(xdgPaths);
    configService.init();
    DestinationService destService = new DestinationService(configService);
    SomeCommand command = new SomeCommand(destService);
    commandLine = new CommandLine(command);
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    commandLine.setOut(new PrintWriter(stdout, true));
    commandLine.setErr(new PrintWriter(stderr, true));
}
```

### Anti-Patterns to Avoid

- **@MicronautTest for command tests:** Pure-logic and command tests use plain JUnit 5. Only use `@MicronautTest` if you need full Micronaut injection wiring — these tests don't.
- **Caching ViracochaConfig as a field:** DestinationService must load fresh on each call, never cache the config object (established by D-08 in Phase 9, mirrored here).
- **Path normalization before traversal check:** Always check `rawPath.contains("..")` before calling `Path.of(rawPath)`. `Path.of("/tmp/../etc").normalize()` silently eats the `..`, defeating the security check.
- **Existence check in destination add:** Source add checked existence because sources must be real directories now. Destination add does NOT check existence — workspace directories may not exist yet.
- **"glob:" prefix in GlobMatcher parameter:** The caller passes the pattern without the prefix (e.g., `"**/*.md"`), and `GlobMatcher` prepends `"glob:"` internally.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Glob pattern matching | Custom regex-based file matcher | `FileSystems.getDefault().getPathMatcher("glob:" + pattern)` | JDK handles `*`, `**`, `?`, `[...]`, `{...}` correctly; `+` is literal; no off-by-one on directory boundaries |
| JSON serialization | Manual string building | `new ObjectMapper().writeValueAsString(entry)` | Jackson already on classpath; handles null fields, escaping, nested objects correctly |
| YAML config persistence | Direct file writing | `ConfigService.load()` / `ConfigService.save()` | ConfigService handles version check, XDG paths, SnakeYAML round-trip |
| Aligned column formatting | Manual padding loops | `String.format("%-" + maxWidth + "s  %s", name, path)` | Already established in `SourceListCommand` |

**Key insight:** Every non-trivial utility needed for this phase already exists in the codebase or in the JDK. The only new code is command wiring and `GlobMatcher` (which is 5 lines of real logic).

## Common Pitfalls

### Pitfall 1: DestinationAddCommand Does Not Check Path Existence

**What goes wrong:** Developer follows `SourceAddCommand` too closely and adds `Files.exists(Path.of(rawPath))` check to `DestinationAddCommand`.
**Why it happens:** SourceService checks existence because sources must be readable directories now. Destinations are workspace directories that may not exist yet.
**How to avoid:** D-04 explicitly states no existence check. Only validate `..` traversal and duplicate names.
**Warning signs:** Test for "non-existent path" in DestinationAddCommandTest should expect exit 0, not exit 1.

### Pitfall 2: Two-Positional-Parameter Commands Break picocli Parsing

**What goes wrong:** `DestinationRemoveMappingCommand` has `NAME` and `INDEX` as positionals. If both are declared with `@Parameters` but without explicit `index="0"` and `index="1"`, picocli may bind them in the wrong order or throw an error.
**Why it happens:** picocli requires explicit `index="N"` when there are multiple positional parameters in the same command.
**How to avoid:** Always use `@Parameters(index="0")` for `NAME` and `@Parameters(index="1")` for `INDEX`.
**Warning signs:** Test with `commandLine.execute("my-dest", "0")` sets wrong field.

### Pitfall 3: MappingEntry Constructor Field Order

**What goes wrong:** `MappingEntry` has `@AllArgsConstructor` with field order: `sourceRef, glob, recurse, sync, params`. Calling `new MappingEntry("src", null, false, false, new LinkedHashMap<>())` passes `null` for glob correctly. Mixing up field order silently produces wrong data.
**Why it happens:** `@AllArgsConstructor` generates constructor based on field declaration order in the class body.
**How to avoid:** Check `MappingEntry.java` field order before writing constructor call. Use named setters if the field count makes it error-prone.
**Warning signs:** YAML config has `sourceRef` where `glob` should be.

### Pitfall 4: destination show Parameters Block When Map Is Empty

**What goes wrong:** Developer prints `Parameters:` even when the `parameters` map is empty, showing `Parameters:` with nothing under it.
**Why it happens:** D-11 says omit the section entirely when the map is empty. `SourceShowCommand` omits the Parameters block when `parameters` list is empty. The destination version must do the same for the map.
**How to avoid:** Guard: `if (!entry.getParameters().isEmpty())` before printing the Parameters block.
**Warning signs:** `destination show` output contains `Parameters:` with no indented lines below it.

### Pitfall 5: removeMapping Index Off-By-One in Error Message

**What goes wrong:** Error message says "destination has N mappings" but uses wrong count — e.g., uses 0-based max instead of actual size.
**Why it happens:** Confusion between 0-based index and 1-based count.
**How to avoid:** Error message: `"Mapping index " + index + " out of range (destination has " + dest.getMappings().size() + " mappings)."` — `size()` is always the correct count.
**Warning signs:** Test with index=0 on empty destination prints "destination has 0 mappings" or wrong number.

### Pitfall 6: GlobMatcher with Absolute vs Relative Paths

**What goes wrong:** `FileSystems.getDefault().getPathMatcher("glob:**/*.md").matches(Path.of("/abs/path/file.md"))` returns false on some JVM implementations when the pattern uses `**` but the path is absolute.
**Why it happens:** JDK glob matching with absolute paths and `**` prefix has platform-dependent edge cases. The `matches()` method checks the full path string representation.
**How to avoid:** In `GlobMatcher`, test with both absolute and relative paths. For Phase 10 the `GlobMatcher` just needs to be correct for the unit test cases in MAP-05 — the actual file walk usage comes in Phase 11. Design the API so Phase 11 can pass relative paths derived from `root.relativize(filePath)`.
**Warning signs:** `GlobMatcherTest` passes with relative paths but Phase 11 generate fails with absolute paths.

## Code Examples

### GlobMatcher — Verified JDK 21 Pattern

```java
// Source: Java 21 FileSystem.getPathMatcher javadoc
// https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileSystem.html
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public final class GlobMatcher {
    private GlobMatcher() {}

    public static boolean matches(String glob, Path path) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(path);
    }
}
```

JDK 21 glob special characters: `*`, `**`, `?`, `\` (escape), `[...]` (bracket), `{...}` (group), `/` (separator). The `+` character is NOT special — it matches itself literally. This is the basis for MAP-05's explicit test requirement.

### GlobMatcherTest — MAP-05 Coverage

```java
// Tests that verify + is treated as a literal (not a regex quantifier)
@Test
void plusInGlobPatternMatchesLiteralPlus() {
    // If + were a regex quantifier, "file+.md" would fail to compile or match wrong paths
    assertTrue(GlobMatcher.matches("*+*.md", Path.of("file+name.md")),
        "+ in glob must match literal + in filename");
}

@Test
void plusGlobDoesNotMatchPathWithoutPlus() {
    assertFalse(GlobMatcher.matches("*+*.md", Path.of("filename.md")),
        "Pattern *+*.md must NOT match filename.md (no + present)");
}

@Test
void doubleStarMatchesAcrossDirectoryBoundaries() {
    assertTrue(GlobMatcher.matches("**/*.md", Path.of("docs/guide/readme.md")));
}

@Test
void singleStarDoesNotCrossDirectoryBoundary() {
    assertFalse(GlobMatcher.matches("*.md", Path.of("docs/readme.md")));
}
```

### DestinationAddMappingCommand — Two-Positional Pattern

```java
// Source: picocli @Parameters with explicit index — extends existing command patterns
@Command(name = "add-mapping", ...)
@Singleton
public class DestinationAddMappingCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the destination.")
    private String destName;

    @Option(names = {"--source"}, required = true,
            description = "Name of the source to map from.")
    private String sourceRef;

    @Option(names = {"--glob"},
            description = "Glob pattern filter (null = copy all files).")
    private String glob;  // null when not specified

    @Option(names = {"--recurse"},
            description = "Walk source directory recursively.")
    private boolean recurse;

    @Option(names = {"--sync"},
            description = "Keep destination in sync on 'vira sync'.")
    private boolean sync;

    // ... call() follows SourceAddCommand exception-handling pattern
}
```

### DestinationRemoveMappingCommand — Two Positionals

```java
@Command(name = "remove-mapping", ...)
@Singleton
public class DestinationRemoveMappingCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the destination.")
    private String destName;

    @Parameters(index = "1", description = "0-based index of the mapping to remove.")
    private int index;

    // call(): catch IndexOutOfBoundsException from service, print D-24 error, return 1
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| v2 catalog/archetype/project/subscription commands | v3 source/destination/mapping model | Phase 8 | DestinationService uses new unified model |
| v2 pattern commands (deleted) | v3 source commands (Phase 9) | Phase 9 | Template to replicate for Phase 10 |

**No deprecated patterns in this phase** — all Phase 9 patterns remain current.

## Open Questions

1. **GlobMatcher method signature for Phase 11 compatibility**
   - What we know: Phase 11 (generate) will use `GlobMatcher` to filter files during directory walk
   - What's unclear: Whether Phase 11 will pass relative paths (from `root.relativize(file)`) or absolute paths. JDK `PathMatcher` with `glob:**/*.md` behaves differently on absolute vs relative paths.
   - Recommendation: Design `GlobMatcher.matches(String glob, Path path)` to accept any `Path`. Add a second overload `matchesRelative(String glob, Path root, Path absolutePath)` that calls `root.relativize(absolutePath)` internally. This way Phase 10 tests use simple relative paths and Phase 11 can use the overload naturally.

2. **`addMapping` return type**
   - What we know: D-03 leaves this to Claude's discretion
   - What's unclear: Whether returning `MappingEntry` adds value for tests
   - Recommendation: Return `void` — the command always shows success/failure via message, and tests can verify via `listMappings()` rather than checking the return value.

3. **`removeMapping` error signaling for destination-not-found vs index-out-of-range**
   - What we know: Both are distinct errors (D-23, D-24). Return `false` for not-found; throw `IndexOutOfBoundsException` for range.
   - Recommendation: `removeMapping` returns `boolean` (false = dest not found) and throws `IndexOutOfBoundsException` with D-24 message for out-of-range. Commands catch the exception separately from the boolean path. This matches the `removeSource` boolean pattern for not-found while using a distinct exception type for the range error.

## Environment Availability

Step 2.6: SKIPPED (no external dependencies — this phase is pure code/config changes using the existing JDK, Maven, and project dependencies already confirmed available).

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) — managed by Micronaut 4.10.10 |
| Config file | `pom.xml` (surefire plugin via micronaut-parent) |
| Quick run command | `./mvnw test -pl . -Dtest="GlobMatcherTest,DestinationServiceTest" -q` |
| Full suite command | `./mvnw test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DEST-01 | `destination add --name X --path P` exits 0 and persists | integration | `./mvnw test -Dtest="DestinationAddCommandTest"` | No — Wave 0 |
| DEST-02 | `destination list` prints aligned name+path, JSONL with `--json` | integration | `./mvnw test -Dtest="DestinationListCommandTest"` | No — Wave 0 |
| DEST-03 | `destination show NAME` prints multi-line block with Parameters + Mappings sections | integration | `./mvnw test -Dtest="DestinationShowCommandTest"` | No — Wave 0 |
| DEST-04 | `destination remove NAME` exits 0, removes from config | integration | `./mvnw test -Dtest="DestinationRemoveCommandTest"` | No — Wave 0 |
| DEST-05 | Duplicate destination name exits 1 with "already exists" error | integration | `./mvnw test -Dtest="DestinationAddCommandTest#addDuplicateNameExitsOneWithAlreadyExistsError"` | No — Wave 0 |
| DEST-06 | Path with `..` exits 1 with traversal error | integration | `./mvnw test -Dtest="DestinationAddCommandTest#addPathWithDotDotExitsOneWithTraversalError"` | No — Wave 0 |
| MAP-01 | `destination add-mapping` accepts source, glob, recurse, sync; persists mapping | integration | `./mvnw test -Dtest="DestinationAddMappingCommandTest"` | No — Wave 0 |
| MAP-02 | `destination list-mappings NAME` prints numbered mapping blocks | integration | `./mvnw test -Dtest="DestinationListMappingsCommandTest"` | No — Wave 0 |
| MAP-03 | `destination remove-mapping NAME INDEX` removes correct mapping | integration | `./mvnw test -Dtest="DestinationRemoveMappingCommandTest"` | No — Wave 0 |
| MAP-04 | Unknown source reference rejected with "Source 'X' not found." | integration | `./mvnw test -Dtest="DestinationAddMappingCommandTest#unknownSourceExitsOneWithNotFoundError"` | No — Wave 0 |
| MAP-05 | `GlobMatcher` treats `+` as literal; `**/*.md` matches across directories | unit | `./mvnw test -Dtest="GlobMatcherTest"` | No — Wave 0 |

### Sampling Rate

- **Per task commit:** `./mvnw test -Dtest="GlobMatcherTest,DestinationServiceTest" -q`
- **Per wave merge:** `./mvnw test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

All test files are new — none exist yet:

- [ ] `src/test/java/org/saltations/infra/GlobMatcherTest.java` — covers MAP-05
- [ ] `src/test/java/org/saltations/destination/DestinationServiceTest.java` — covers DEST-05, DEST-06, service CRUD
- [ ] `src/test/java/org/saltations/destination/DestinationAddCommandTest.java` — covers DEST-01, DEST-05, DEST-06
- [ ] `src/test/java/org/saltations/destination/DestinationListCommandTest.java` — covers DEST-02
- [ ] `src/test/java/org/saltations/destination/DestinationShowCommandTest.java` — covers DEST-03 (including Parameters omission, Mappings section)
- [ ] `src/test/java/org/saltations/destination/DestinationRemoveCommandTest.java` — covers DEST-04
- [ ] `src/test/java/org/saltations/destination/DestinationAddMappingCommandTest.java` — covers MAP-01, MAP-04
- [ ] `src/test/java/org/saltations/destination/DestinationListMappingsCommandTest.java` — covers MAP-02
- [ ] `src/test/java/org/saltations/destination/DestinationRemoveMappingCommandTest.java` — covers MAP-03

## Project Constraints (from CLAUDE.md)

| Directive | Impact on Phase 10 |
|-----------|-------------------|
| Tech stack: JDK 21, Micronaut, picocli, Lombok, Jackson, SnakeYAML — no deviations in v1 | No new dependencies. GlobMatcher uses JDK 21 `java.nio.file.FileSystem` (built-in). |
| Config format: YAML only | All persistence via `ConfigService.save()` which writes YAML via SnakeYAML. |
| Regeneration: generate must never overwrite existing workspace files | Out of scope for Phase 10 (generate is Phase 11). |
| Scope: local filesystem only | Destination path is stored as-is; no network or Git operations. |
| `@Data @NoArgsConstructor @AllArgsConstructor` on model POJOs, no `@Builder` | `DestinationEntry` and `MappingEntry` already comply. No new model classes. |
| PascalCase for Java class files, test files append `Test` suffix | All new classes follow this convention. |
| 4-space indentation | Apply in all new files. |
| Thin command → service singleton pattern | `DestinationService` holds all validation and persistence. Commands parse args, call service, format output. |
| `@Spec CommandSpec spec` for output via `spec.commandLine().getOut()` / `.getErr()` | Required in all new command classes. |
| Exit 0 on success, exit 1 on any error | All `call()` methods return 0 or 1. |
| `Callable<Integer>` for all commands | All command classes implement `Callable<Integer>`. |

## Sources

### Primary (HIGH confidence)

- Java 21 official docs — `java.nio.file.FileSystem.getPathMatcher` — glob syntax specification, confirmed `+` is not a special character
- Existing codebase (read directly): `SourceService.java`, `SourceCommand.java`, `SourceAddCommand.java`, `SourceShowCommand.java`, `SourceListCommand.java`, `SourceRemoveCommand.java` — exact patterns to replicate
- Existing codebase (read directly): `DestinationEntry.java`, `MappingEntry.java`, `ViracochaConfig.java` — model already defined
- Existing codebase (read directly): `ConfigService.java`, `HiddenPathFilter.java` — infrastructure to inject/reference
- Existing tests (read directly): `SourceAddCommandTest.java`, `SourceServiceTest.java`, `SourceShowCommandTest.java`, `SourceListCommandTest.java`, `SourceRemoveCommandTest.java` — test patterns to replicate

### Secondary (MEDIUM confidence)

- `ViracochaCommand.java` — confirmed exact subcommand registration syntax for adding `DestinationCommand.class`
- `CONTEXT.md` (10-CONTEXT.md) — all locked decisions verified against existing code patterns

### Tertiary (LOW confidence)

None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; all libraries already in project and tested
- Architecture: HIGH — patterns verified by reading actual source files, not inferred from training data
- Pitfalls: HIGH — derived from reading the actual code and understanding the differences between source and destination validation semantics
- GlobMatcher behavior: HIGH — verified via official Java 21 FileSystem.getPathMatcher documentation

**Research date:** 2026-05-10
**Valid until:** 2026-08-10 (stable library stack; JDK glob behavior is stable across JDK versions)
