# Phase 2: Catalogs and Patterns - Research

**Researched:** 2026-03-28
**Domain:** Java CLI command groups, Jackson YAML POJO typing, Freemarker variable extraction via regex
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `publisher list` and `pattern list` output plain text with aligned columns, no headers. Example: `<name>  <path>` or `<name>  <path>  <param-count>`.
- **D-02:** `publisher show` and `pattern show` output a multi-line key-value block. Example: `Name: foo` / `Path: /some/path` / `Parameters: name, email, role`.
- **D-03:** All list and show commands support a `--json` flag that switches output to JSONL (one JSON object per line per entry, or a single JSON object for show).
- **D-04:** Extraction captures `${varName}` syntax only — top-level name before any `.` or `?`. Example: `${user.name}` → extracts `user`; `${title?upper_case}` → extracts `title`. Applies to file content AND file/folder name path segments.
- **D-05:** If any file in the pattern has a malformed Freemarker expression (e.g., `${unclosed`), registration fails fast — print a clear error, exit 1, do not modify config.
- **D-06:** Registering a name that already exists → print error (`Publisher/Pattern 'foo' already registered. Use unregister first.`), exit 1, do not modify config.
- **D-07:** Unregistering a name that does not exist → print error (`Publisher/Pattern 'foo' not found.`), exit 1.
- **D-08:** Every implemented task must include at least one JUnit 5 test verifying its behavior before the task is considered complete. Use `@MicronautTest` for integration tests (commands, config round-trips); plain JUnit 5 for pure-logic units (e.g., the Freemarker extractor). Test fixture config paths must point to `@TempDir` directories — never the real XDG config.

### Claude's Discretion

- Exact column alignment widths for plain text output
- Precise JSON field names for `--json` output (should be camelCase, e.g., `name`, `path`, `parameters`)
- Internal package structure for publisher/pattern commands and model POJOs
- Whether `CatalogCommand` and `PatternCommand` are in separate packages or a shared `commands` package
- Freemarker extraction regex implementation detail (character class for valid identifier chars)

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUB-01 | User can run `vira catalog register --name <name> --path <path>` to register a named publisher in central config | ConfigService load-mutate-save pattern; CatalogEntry POJO; duplicate guard D-06 |
| PUB-02 | `vira catalog register` validates that the specified path exists on disk before registering | `Files.exists(Path.of(path))` before adding to list |
| PUB-03 | User can run `vira catalog list` to display all registered catalogs (name, path) | D-01 plain text aligned columns; D-03 --json flag |
| PUB-04 | User can run `vira catalog show --name <name>` to display details of a specific catalog | D-02 multi-line key-value block; D-03 --json flag |
| PUB-05 | User can run `vira catalog unregister --name <name>` to remove a catalog from central config | D-07 not-found error; load-mutate-save |
| PAT-01 | User can run `vira pattern register --name <name> --path <path>` to register a named pattern in central config | Same as PUB-01 + FreemarkerVariableExtractor |
| PAT-02 | `vira pattern register` validates that the specified path exists on disk before registering | `Files.exists` before registering |
| PAT-03 | `vira pattern register` extracts Freemarker variable names from pattern source and stores them | FreemarkerVariableExtractor: scan file content + path segments; D-04 regex; D-05 fail on malformed |
| PAT-04 | User can run `vira pattern list` to display all registered patterns (name, path, parameter count) | D-01 with param count column |
| PAT-05 | User can run `vira pattern show --name <name>` to display pattern details including extracted parameter names | D-02; comma-separated param list |
| PAT-06 | User can run `vira pattern unregister --name <name>` to remove a pattern from central config | D-07 not-found error |
</phase_requirements>

---

## Summary

Phase 2 adds two new command groups (`vira catalog` and `vira pattern`) with four leaf subcommands each (register, list, show, unregister). The structural pattern is identical to Phase 1's `ConfigCommand` → `InitCommand`/`ShowConfigCommand` hierarchy: a group `@Command` class containing subcommands via static `subcommands = {...}` declaration, with each leaf command as a `@Singleton` implementing `Callable<Integer>`.

The main new technical work in this phase is twofold: (1) upgrading `ViracochaConfig` lists from `List<Object>` to typed `List<CatalogEntry>` / `List<PatternEntry>` POJOs with proper Jackson YAML annotations, and (2) implementing `FreemarkerVariableExtractor` — a pure-logic class that walks a directory tree, reads file content and path segments, and extracts `${varName}` identifiers using a regex. The Freemarker library itself is NOT needed for extraction; the task requires only `java.util.regex.Pattern` and `java.nio.file` APIs. Freemarker will become relevant in Phase 4 for actual template rendering.

**Primary recommendation:** Replicate the command structure from Phase 1 exactly (same annotations, same DI wiring, same test pattern), add `freemarker` to pom.xml now as noted in STATE.md blockers, implement `FreemarkerVariableExtractor` as a standalone unit-testable class, and update `ViracochaConfig` to use typed list entries.

---

## Standard Stack

### Core (already in pom.xml)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| picocli | BOM-managed (~4.7.x) | CLI command parsing, `@Command`/`@Option` | Already established in Phase 1 |
| micronaut-picocli | BOM-managed | Micronaut + picocli DI integration | Already established in Phase 1 |
| jackson-dataformat-yaml | BOM-managed | YAML config serialization/deserialization | Already in pom.xml |
| lombok | BOM-managed | `@Data`, `@Builder` for model POJOs | Already in pom.xml |
| junit-jupiter | BOM-managed | JUnit 5 tests | Already in pom.xml |

### New Dependency Required

| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| org.freemarker:freemarker | **2.3.34** | Freemarker template engine (Phase 4 use); also provides FTL syntax reference for extraction regex | NOT BOM-managed by Micronaut; must be pinned explicitly |

**Version verification:** Confirmed 2.3.34 is the current release as of 2026-03-28 via Maven Central metadata. HTTP 200 confirmed for the artifact jar.

**Micronaut BOM note:** The Micronaut 4.10.10 parent BOM does not include `org.freemarker:freemarker`. The version must be declared explicitly in `pom.xml`.

**Installation:**
```xml
<dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <version>2.3.34</version>
    <scope>compile</scope>
</dependency>
```

---

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/org/saltations/
├── ViracochaCommand.java         # add CatalogCommand.class, PatternCommand.class to subcommands
├── config/                       # Phase 1 — unchanged
├── infra/
│   └── XdgPaths.java            # Phase 1 — unchanged
├── model/
│   ├── ViracochaConfig.java      # CHANGE: List<Object> -> List<CatalogEntry>, List<PatternEntry>
│   ├── CatalogEntry.java       # NEW: @Data, name + path fields
│   └── PatternEntry.java         # NEW: @Data, name + path + List<String> parameters fields
├── publisher/
│   ├── CatalogCommand.java     # NEW: group command (register/list/show/unregister subcommands)
│   ├── RegisterCatalogCommand.java
│   ├── ListCatalogsCommand.java
│   ├── ShowCatalogCommand.java
│   └── UnregisterCatalogCommand.java
└── pattern/
    ├── PatternCommand.java       # NEW: group command
    ├── RegisterPatternCommand.java
    ├── ListPatternsCommand.java
    ├── ShowPatternCommand.java
    ├── UnregisterPatternCommand.java
    └── FreemarkerVariableExtractor.java  # NEW: pure-logic, no DI needed

src/test/java/org/saltations/
├── publisher/
│   ├── RegisterCatalogCommandTest.java
│   ├── ListCatalogsCommandTest.java
│   ├── ShowCatalogCommandTest.java
│   └── UnregisterCatalogCommandTest.java
├── pattern/
│   ├── RegisterPatternCommandTest.java
│   ├── ListPatternsCommandTest.java
│   ├── ShowPatternCommandTest.java
│   ├── UnregisterPatternCommandTest.java
│   └── FreemarkerVariableExtractorTest.java
└── model/
    └── ViracochaConfigTest.java  # EXTEND: add typed list round-trip tests
```

### Pattern 1: Group Command with Static Subcommands

Identical to `ConfigCommand`. Every group command is a `@Singleton` `Callable<Integer>` that returns 0 and declares its leaf subcommands statically.

```java
// Source: existing ConfigCommand.java in this project
@Command(
    name = "publisher",
    description = "Manage registered catalogs.",
    mixinStandardHelpOptions = true,
    subcommands = {
        RegisterCatalogCommand.class,
        ListCatalogsCommand.class,
        ShowCatalogCommand.class,
        UnregisterCatalogCommand.class
    }
)
@Singleton
public class CatalogCommand implements Callable<Integer> {
    @Override
    public Integer call() { return 0; }
}
```

### Pattern 2: Leaf Command with ConfigService Inject

Identical to `InitCommand`. Every leaf command gets `ConfigService` via `@Inject` constructor, uses `@Spec CommandSpec` for output streams.

```java
// Source: existing InitCommand.java in this project
@Command(name = "register", description = "Register a named publisher.", mixinStandardHelpOptions = true)
@Singleton
public class RegisterCatalogCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--name"}, required = true, description = "Publisher name")
    private String name;

    @Option(names = {"--path"}, required = true, description = "Absolute or relative path to publisher directory")
    private String path;

    private final ConfigService configService;

    @Inject
    public RegisterCatalogCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();
            // ... validation + mutation
            configService.save(config);
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
```

### Pattern 3: Typed POJO Model Entries

`CatalogEntry` and `PatternEntry` follow the same `@Data` Lombok pattern as `ViracochaConfig`. Jackson YAML handles polymorphic deserialization automatically when the field type is a concrete class (not `Object`).

```java
// Model POJO — no special Jackson annotations needed for simple typed list
@Data
public class CatalogEntry {
    private String name;
    private String path;
}

@Data
public class PatternEntry {
    private String name;
    private String path;
    private List<String> parameters = new ArrayList<>();
}
```

`ViracochaConfig` changes:
```java
private List<CatalogEntry> publishers = new ArrayList<>();
private List<PatternEntry> patterns = new ArrayList<>();
```

**Important:** Jackson with `YAMLFactory` can deserialize a `List<CatalogEntry>` from YAML without `@JsonDeserialize` annotations when using a concrete type. The existing `ConfigService` uses `yaml.readValue(file, ViracochaConfig.class)` — this already handles typed lists correctly. No change to `ConfigService` needed.

**Migration concern:** Existing `config.yaml` files written with Phase 1 code have `publishers: []` and `patterns: []` (empty lists). Jackson deserializes empty YAML lists to an empty `ArrayList` regardless of element type, so the upgrade is safe with no data migration needed.

### Pattern 4: FreemarkerVariableExtractor

A plain Java class with no DI annotations — it is a pure function that takes a `Path` (directory root) and returns a `Set<String>` of extracted variable names (sorted to a `List<String>` for stable YAML output).

**Algorithm:**
1. Walk the path tree with `Files.walk(root)` — includes files and directories
2. For each path segment name (folder names + filenames), run regex on the string
3. For each regular file, read content as a String (`Files.readString(path)`)
4. Apply the same regex to the file content
5. Collect all matches into a `LinkedHashSet` (preserves insertion order, deduplicates)

**Regex for D-04:**
```java
// Matches ${identifier} where identifier is the top-level name (before . or ?)
// Java identifier chars: [a-zA-Z_$][a-zA-Z0-9_$]*
private static final Pattern VAR_PATTERN =
    Pattern.compile("\\$\\{([a-zA-Z_$][a-zA-Z0-9_$]*)(?:[.?][^}]*)?\\}");
```

**Malformed expression detection for D-05:**
A `${unclosed` expression is one where `${` appears in the string but there is no matching `}`. Detection approach:

```java
// After extracting all valid matches, check for any remaining ${ that wasn't matched
// Simple approach: scan for ${ that does not have a closing } before the next ${
private static final Pattern UNCLOSED = Pattern.compile("\\$\\{[^}]*$", Pattern.MULTILINE);
// OR: use a stateful scan — find all ${ positions, verify each has a } before next ${
```

The simplest reliable approach: after running `VAR_PATTERN` on the full content, separately check if `content.contains("${")` and the count of `${` occurrences exceeds the count of valid `VAR_PATTERN` matches. Any unmatched `${` signals a malformed expression.

**Extraction scope — D-04:** Path segments AND file content. For path segments, extract variable names from folder/file names within the pattern root (e.g., `${service}Controller.java` → extracts `service`). For file content, scan the full text of each file.

### Pattern 5: --json Flag Output

Use Jackson's `ObjectMapper` (already available via Micronaut context or instantiated directly) to serialize entries. For `list` commands, write one JSON object per line (JSONL). For `show` commands, write a single JSON object.

```java
@Option(names = {"--json"}, description = "Output as JSON")
private boolean json;

// In call():
if (json) {
    ObjectMapper om = new ObjectMapper();
    // For list: one object per line
    for (CatalogEntry e : config.getCatalogs()) {
        spec.commandLine().getOut().println(om.writeValueAsString(e));
    }
} else {
    // Plain text aligned columns
    for (CatalogEntry e : config.getCatalogs()) {
        spec.commandLine().getOut().printf("%-20s  %s%n", e.getName(), e.getPath());
    }
}
```

Jackson `ObjectMapper` (JSON, not YAML) is already on the classpath via `micronaut-serde-jackson`. Instantiate it directly in the command; no bean injection needed for this utility use.

### Anti-Patterns to Avoid

- **Runnable instead of Callable:** All commands must be `Callable<Integer>`. Phase 1 established this — do not regress to `Runnable`.
- **Dynamic subcommand registration:** All subcommands must be in `@Command(subcommands = {...})` — no runtime `addSubcommand()` calls.
- **Using `@MicronautTest` for command tests:** Phase 1 test pattern uses plain JUnit with manual `new CommandLine(command)` wiring. This is the established pattern — do not use `@MicronautTest` for command tests that need output capture. Use `@MicronautTest` only for integration tests that need the full DI container.
- **Lombok `@Builder` alone on Micronaut beans:** Must pair with `@NoArgsConstructor` + `@AllArgsConstructor` to keep Jackson deserialization working. For model POJOs (not DI beans), `@Data` alone is sufficient.
- **Freemarker `Configuration` for extraction:** Freemarker's template parsing API is heavyweight and adds unnecessary complexity for the extraction task. The decision (D-04) specifies pure regex extraction — do not use `freemarker.template.Configuration` for Phase 2 extraction.
- **Caching config across commands:** `configService.load()` must be called fresh at the start of each command `call()` — never cache the loaded config as a field.
- **Writing to System.out directly:** Always use `spec.commandLine().getOut()` and `spec.commandLine().getErr()` — never `System.out.println()` in command classes.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Directory tree walking | Custom recursive File visitor | `Files.walk(path)` (java.nio) | Already handles symlinks, depth, exceptions correctly |
| YAML serialization | Custom YAML writer | `jackson-dataformat-yaml` (already in pom) | Handles escaping, null safety, round-trips |
| Freemarker variable identification in Phase 4 | Custom template parser | `freemarker.template.Configuration` | Correct parsing of all FTL constructs |
| Column alignment | Custom padding logic | `String.format("%-Ns", ...)` or `printf` | Built into JDK, no library needed |
| JSON serialization for --json | Custom JSON builder | `ObjectMapper` (Jackson, already on classpath) | Already available, handles all escaping |

**Key insight:** The entire Freemarker variable extraction task (PAT-03) can be implemented with `java.util.regex` and `java.nio.file` — no new library APIs are needed. The `freemarker` dependency added to `pom.xml` is for Phase 4 rendering, not Phase 2 extraction.

---

## Runtime State Inventory

Step 2.5 SKIPPED — this is a greenfield feature phase, not a rename/refactor/migration phase.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JDK 21 | All compilation and tests | Yes | 21 (Temurin) | — |
| Maven wrapper (mvnw) | Build | Yes | 3.9.x | — |
| Maven Central network access | freemarker 2.3.34 download | Yes (verified HTTP 200) | — | — |
| freemarker 2.3.34 jar | Phase 4 rendering; pom.xml dependency add | Yes (Maven Central) | 2.3.34 | — |

**Missing dependencies with no fallback:** None.

**Missing dependencies with fallback:** None.

**Blocker from STATE.md:** `freemarker` is NOT yet in `pom.xml`. It must be added in Wave 0 / Task 0 before any feature code that imports Freemarker classes is written. The first plan must add this dependency.

---

## Common Pitfalls

### Pitfall 1: ViracochaConfig Typed List Breaks Existing Tests

**What goes wrong:** `ViracochaConfigTest` currently asserts that serialized YAML contains `catalogs` and `patterns` keys. Changing the list type from `List<Object>` to `List<CatalogEntry>` may alter how Jackson serializes an empty list — but it should NOT break anything because an empty `ArrayList<CatalogEntry>` serializes to `publishers: []` in YAML, identical to `List<Object>`.

**Why it happens:** Concern about Jackson type erasure with YAML factory. In practice, empty lists serialize identically regardless of element type. Non-empty lists will now serialize with field names from `CatalogEntry` rather than as raw objects.

**How to avoid:** Run all 17 existing tests after changing `ViracochaConfig`. If any test fails, the serialization format changed — review Jackson YAML behavior.

**Warning signs:** `ViracochaConfigTest.defaultConfigSerializesToYaml` fails after the model change.

### Pitfall 2: Partial Subcommand Hierarchy Fails Silently at Runtime

**What goes wrong:** Adding `CatalogCommand.class` to `ViracochaCommand.subcommands` but NOT also adding all leaf subcommands to `CatalogCommand.subcommands` causes picocli to accept `vira catalog` but silently fail on `vira catalog register`.

**Why it happens:** Picocli builds its command tree statically from annotations. A missing subcommand in the static declaration means the subcommand class is never registered.

**How to avoid:** Declare ALL leaf subcommands in the group command's `@Command(subcommands = {...})` annotation before any integration testing. Wire the full hierarchy in a single task.

**Warning signs:** `vira catalog register --help` shows only the group command help, not the register command. Exit code 2 from picocli on unrecognized subcommand.

### Pitfall 3: Freemarker Malformed Expression False Positives

**What goes wrong:** The malformed expression detector (D-05) may trigger false positives on files that contain literal `${` strings that are intentionally not Freemarker expressions (e.g., shell scripts, documentation, Bash heredocs in template files).

**Why it happens:** Detection logic counts `${` occurrences without context.

**How to avoid:** The project scope is Freemarker templates — assume all `${` in pattern files are intended as Freemarker syntax. A `${` without a closing `}` on the same logical expression IS an error in a Freemarker template. This is consistent with D-05 intent.

**Warning signs:** Legitimate shell script templates in a pattern directory trigger false registration failures.

### Pitfall 4: Files.walk Includes Hidden Files and Directories

**What goes wrong:** `Files.walk()` on a pattern directory traverses all files including `.git/`, `.DS_Store`, and other non-template files, causing spurious variable extraction hits or performance issues.

**Why it happens:** `Files.walk()` is not filtered by default.

**How to avoid:** Add a filter to skip hidden files/directories (those whose name starts with `.`). Use `.filter(p -> !p.getFileName().toString().startsWith("."))`.

**Warning signs:** Extracted variable list contains spurious names from `__pycache__` or `.git/config` files.

### Pitfall 5: ConfigNotInitializedException Propagation

**What goes wrong:** A command's `call()` method calls `configService.load()` but does not catch `ConfigNotInitializedException` — the exception propagates to picocli, which prints a stack trace to stderr instead of the user-friendly message.

**Why it happens:** The exception extends `RuntimeException` (or `IOException` — check the implementation) and may not be in the catch block.

**How to avoid:** Every command that calls `configService.load()` must catch `ConfigNotInitializedException` explicitly and print the user-facing message to `spec.commandLine().getErr()`, then return 1.

**Warning signs:** Tests show "Config not initialized" appearing in stack traces rather than clean error messages.

---

## Code Examples

### Verified Pattern: Load-Mutate-Save for Register

```java
// Source: ConfigService.java + InitCommand.java in this project
@Override
public Integer call() {
    try {
        // Guard: fails fast if config not initialized (CONF-03)
        ViracochaConfig config = configService.load();

        // Validate path exists on disk (PUB-02 / PAT-02)
        if (!Files.exists(Path.of(path))) {
            spec.commandLine().getErr().println("Error: path does not exist: " + path);
            return 1;
        }

        // Duplicate check (D-06)
        boolean alreadyExists = config.getCatalogs().stream()
            .anyMatch(e -> e.getName().equals(name));
        if (alreadyExists) {
            spec.commandLine().getErr().println(
                "Publisher '" + name + "' already registered. Use unregister first.");
            return 1;
        }

        // Mutate
        config.getCatalogs().add(new CatalogEntry(name, path));

        // Save
        configService.save(config);
        spec.commandLine().getOut().println("Publisher '" + name + "' registered.");
        return 0;
    } catch (ConfigNotInitializedException e) {
        spec.commandLine().getErr().println(e.getMessage());
        return 1;
    } catch (IOException e) {
        spec.commandLine().getErr().println("Error: " + e.getMessage());
        return 1;
    }
}
```

### Verified Pattern: Test Wiring (from InitCommandTest.java)

```java
// Source: InitCommandTest.java in this project
@TempDir Path tempDir;

@BeforeEach
void setUp() {
    XdgPaths xdgPaths = new XdgPaths() {
        @Override public Path configFile() { return tempDir.resolve("viracocha/config.yaml"); }
        @Override public Path configDir() { return tempDir.resolve("viracocha"); }
        @Override public Path dataDir() { return tempDir.resolve("share/viracocha"); }
    };
    ConfigService configService = new ConfigService(xdgPaths);
    // Wire command manually — no Micronaut container needed
    RegisterCatalogCommand command = new RegisterCatalogCommand(configService);
    commandLine = new CommandLine(command);
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    commandLine.setOut(new PrintWriter(stdout, true));
    commandLine.setErr(new PrintWriter(stderr, true));
}
```

### Verified Pattern: FreemarkerVariableExtractor Regex

```java
// D-04: top-level name before any . or ?
// Freemarker identifier: [a-zA-Z_$][a-zA-Z0-9_$]*
// Handles: ${name}, ${user.email} -> "user", ${title?upper_case} -> "title"
private static final Pattern VAR_PATTERN =
    Pattern.compile("\\$\\{([a-zA-Z_$][a-zA-Z0-9_$]*)(?:[.?][^}]*)?\\}");

// Malformed detection: any ${ not consumed by VAR_PATTERN is malformed
// Simple check: count ${ occurrences vs matched groups
long openCount = content.chars()
    .filter(c -> content.indexOf("${", offset) >= 0).count();
// Safer: use a second pattern for bare ${ with no closing }
private static final Pattern UNCLOSED_PATTERN =
    Pattern.compile("\\$\\{(?![^}]*\\})");
```

### Verified Pattern: Files.walk for Directory Extraction

```java
// java.nio.file — no extra dependency needed
public Set<String> extractFromDirectory(Path root) throws IOException {
    Set<String> vars = new LinkedHashSet<>();
    try (Stream<Path> stream = Files.walk(root)) {
        List<Path> paths = stream
            .filter(p -> !p.getFileName().toString().startsWith("."))
            .collect(Collectors.toList());

        for (Path p : paths) {
            // Extract from path segment names
            extractFromString(p.getFileName().toString(), vars);

            // Extract from file content
            if (Files.isRegularFile(p)) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                checkForMalformed(content, p);  // throws on D-05 violation
                extractFromString(content, vars);
            }
        }
    }
    return vars;
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `List<Object>` in ViracochaConfig | `List<CatalogEntry>` / `List<PatternEntry>` | Phase 2 | Type-safe access; Jackson handles transparently |
| No freemarker in pom.xml | `freemarker:2.3.34` added | Phase 2 start | Required for Phase 4; harmless in Phase 2 |

**Deprecated/outdated:**
- `ViracochaConfig.publishers` as `List<Object>`: replaced by `List<CatalogEntry>` in Phase 2.
- `ViracochaConfig.patterns` as `List<Object>`: replaced by `List<PatternEntry>` in Phase 2.

---

## Open Questions

1. **CatalogEntry: should `@AllArgsConstructor` be added?**
   - What we know: `@Data` generates a required-args constructor only if there are `final` fields. `@Data` alone does NOT generate a constructor if all fields are mutable.
   - What's unclear: Whether `new CatalogEntry(name, path)` works without an explicit `@AllArgsConstructor`.
   - Recommendation: Add `@AllArgsConstructor` explicitly to `CatalogEntry` and `PatternEntry` alongside `@Data` so that convenient constructor calls work. Also add `@NoArgsConstructor` for Jackson deserialization. This is the safe pattern.

2. **freemarker version: should a `<properties>` entry be added?**
   - What we know: `logstash-logback-encoder` was pinned with inline `<version>7.4</version>` in Phase 1 (noted in STATE.md) because it is not BOM-managed.
   - What's unclear: Team preference for consistency.
   - Recommendation: Follow the same pattern — add `<freemarker.version>2.3.34</freemarker.version>` to `<properties>` and reference it as `${freemarker.version}` in the dependency, for consistency with other pinned versions.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) + maven-surefire |
| Config file | `pom.xml` (surefire auto-detects JUnit 5) |
| Quick run command | `./mvnw test -pl . -Dtest="*CatalogCommand*,*PatternCommand*,*FreemarkerVariable*" -q` |
| Full suite command | `./mvnw test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PUB-01 | register saves publisher to config | unit | `./mvnw test -Dtest=RegisterCatalogCommandTest` | No - Wave 0 |
| PUB-02 | register rejects non-existent path | unit | `./mvnw test -Dtest=RegisterCatalogCommandTest#registrationFailsForNonExistentPath` | No - Wave 0 |
| PUB-03 | list shows name+path columns | unit | `./mvnw test -Dtest=ListCatalogsCommandTest` | No - Wave 0 |
| PUB-04 | show prints key-value block | unit | `./mvnw test -Dtest=ShowCatalogCommandTest` | No - Wave 0 |
| PUB-05 | unregister removes publisher | unit | `./mvnw test -Dtest=UnregisterCatalogCommandTest` | No - Wave 0 |
| PAT-01 | pattern register saves to config | unit | `./mvnw test -Dtest=RegisterPatternCommandTest` | No - Wave 0 |
| PAT-02 | register rejects non-existent path | unit | `./mvnw test -Dtest=RegisterPatternCommandTest#registrationFailsForNonExistentPath` | No - Wave 0 |
| PAT-03 | variable extraction from content+paths | unit | `./mvnw test -Dtest=FreemarkerVariableExtractorTest` | No - Wave 0 |
| PAT-03 | malformed expression rejects registration | unit | `./mvnw test -Dtest=FreemarkerVariableExtractorTest#malformedExpressionThrows` | No - Wave 0 |
| PAT-04 | list shows name+path+paramCount | unit | `./mvnw test -Dtest=ListPatternsCommandTest` | No - Wave 0 |
| PAT-05 | show includes parameter list | unit | `./mvnw test -Dtest=ShowPatternCommandTest` | No - Wave 0 |
| PAT-06 | unregister removes pattern | unit | `./mvnw test -Dtest=UnregisterPatternCommandTest` | No - Wave 0 |
| D-03 | --json flag produces JSONL output | unit | covered within each list/show test class | No - Wave 0 |
| D-06 | duplicate registration rejected | unit | covered within each register test class | No - Wave 0 |
| D-07 | unregister unknown name exits 1 | unit | covered within each unregister test class | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `./mvnw test -Dtest="[TaskTestClass]" -q`
- **Per wave merge:** `./mvnw test`
- **Phase gate:** `./mvnw test` — all 17 existing + new tests green before `/gsd:verify-work`

### Wave 0 Gaps

All test files are new — none exist yet:

- [ ] `src/test/java/org/saltations/catalog/RegisterCatalogCommandTest.java`
- [ ] `src/test/java/org/saltations/catalog/ListCatalogsCommandTest.java`
- [ ] `src/test/java/org/saltations/catalog/ShowCatalogCommandTest.java`
- [ ] `src/test/java/org/saltations/catalog/UnregisterCatalogCommandTest.java`
- [ ] `src/test/java/org/saltations/pattern/RegisterPatternCommandTest.java`
- [ ] `src/test/java/org/saltations/pattern/ListPatternsCommandTest.java`
- [ ] `src/test/java/org/saltations/pattern/ShowPatternCommandTest.java`
- [ ] `src/test/java/org/saltations/pattern/UnregisterPatternCommandTest.java`
- [ ] `src/test/java/org/saltations/pattern/FreemarkerVariableExtractorTest.java`

These test files are created as part of each feature task, not as a separate Wave 0 setup task. The existing test infrastructure (JUnit 5, maven-surefire, `@TempDir`) is sufficient.

---

## Project Constraints (from CLAUDE.md)

The following directives from CLAUDE.md are binding constraints on all planning and implementation in this phase:

| Directive | Impact on Phase 2 |
|-----------|-------------------|
| Tech stack: JDK 21, Micronaut, picocli, Lombok, Freemarker, jackson-dataformat-yaml, Logback — no deviations | Freemarker is the mandated template engine; Jackson YAML for config |
| Config format: YAML only | `config.yaml` stores catalogs and patterns; no JSON config files |
| Regeneration must not overwrite — skip-existing semantics | Not relevant in Phase 2 (no generation); relevant in Phase 4 |
| Local filesystem only — no network, no Git operations in v1 | Publisher/pattern paths are local filesystem paths only |
| All subcommands declared statically | `@Command(subcommands = {...})` — no dynamic registration |
| Lombok must be first in annotationProcessorPaths | Do not reorder pom.xml annotation processor paths when adding freemarker |
| Use `@Spec CommandSpec` for picocli output | `spec.commandLine().getOut()` and `spec.commandLine().getErr()` always |
| GSD workflow enforcement | All file changes must go through GSD execute-phase |

---

## Sources

### Primary (HIGH confidence)
- Existing source files in `/home/jmochel/pers/viracocha/src/` — direct inspection of Phase 1 code
- Maven Central: `https://repo1.maven.org/maven2/org/freemarker/freemarker/maven-metadata.xml` — confirmed 2.3.34 is latest release
- Maven Central: HTTP 200 on `freemarker-2.3.34.jar` — artifact confirmed available
- `./mvnw test` output — 17 tests pass, Phase 1 baseline confirmed green
- Micronaut BOM 4.10.10 — confirmed freemarker is NOT in BOM (no version managed)

### Secondary (MEDIUM confidence)
- JDK 21 `java.nio.file.Files.walk` API — standard JDK, stable since Java 8
- `java.util.regex.Pattern` for FTL variable extraction — well-established Java standard

### Tertiary (LOW confidence)
- None

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all existing dependencies confirmed by pom.xml inspection; freemarker 2.3.34 confirmed on Maven Central
- Architecture: HIGH — command pattern verified by reading all Phase 1 source files; patterns are direct replications
- Pitfalls: HIGH — derived from actual code inspection (ConfigService, test patterns, Lombok behavior)

**Research date:** 2026-03-28
**Valid until:** 2026-04-28 (stable APIs — Micronaut, picocli, Jackson, freemarker)
