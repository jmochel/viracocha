# Phase 8: Model & Config Foundation - Research

**Researched:** 2026-05-08
**Domain:** Java/Micronaut/Lombok model refactoring, Jackson YAML serialization, picocli subcommand management
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `ArchetypePathUtils` moves to `infra/` and is renamed to `HiddenPathFilter`. Update all imports in `GeneratorService` and `DefaultSyncService` accordingly.
- **D-02:** `FreemarkerVariableExtractor` moves to `infra/` (keeping its name). Phase 9 (SourceService) uses it from its new location.
- **D-03:** Package deletion order: move utilities to `infra/` first, then delete `archetype/`, `catalog/`, `project/`, `subscription/` one at a time — compile after each deletion to catch any remaining cross-references before proceeding.
- **D-04:** `SourceEntry` fields: `name` (String), `path` (String), `templates` (boolean, default `false`), `parameters` (List\<String\> — variable names only, consistent with current ArchetypeEntry pattern).
- **D-05:** `DestinationEntry` fields: `name` (String), `path` (String), `parameters` (Map\<String, String\> — default param values), `mappings` (List\<MappingEntry\>).
- **D-06:** `MappingEntry` v3 fields: `sourceRef` (String), `glob` (String, default `null` = no filter = select all files), `recurse` (boolean, default `false` — flat copy by default, opt-in to subtree walk), `sync` (boolean, default `false` — opt-in to keep-in-sync), `params` (Map\<String, String\> — per-mapping param overrides).
- **D-07:** `ViracochaConfig` v3 fields: `version` (int, value `3`), `sources` (List\<SourceEntry\>), `destinations` (List\<DestinationEntry\>).
- **D-08:** All new POJOs use Lombok `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`. No `@Builder` alone on Micronaut-injected beans.
- **D-09:** `ConfigService.load()` reads the raw `version` field first (using a minimal POJO or `JsonNode`). If missing or < 3, throw a new `ConfigVersionException` (extends `IOException` or is a checked exception) with a message: `"Config file is v{N} — v3 format required. Delete ~/.config/viracocha/config.yaml and run 'vira config init' to start fresh."` Exit code 1.
- **D-10:** `ConfigService.init()` writes a fresh `ViracochaConfig` with `version: 3`. Idempotent — if config already exists and is v3, prints "Config already initialized" and exits 0 (existing Phase 1 behavior).
- **D-11:** Update `reflect-config.json` to replace all v2 model class entries with v3 entries (`SourceEntry`, `DestinationEntry`, `MappingEntry`, `ViracochaConfig`). GraalVM native image is out of scope but maintaining the file avoids silently stale config.
- **D-12:** `ViracochaCommand` subcommands list updated to remove `ArchetypeCommand`, `CatalogCommand`, `ProjectCommand`, `SubscriptionCommand`. Retains `ConfigCommand`, `GenerateCommand`, `SyncCommand`. Source/destination/mapping commands are added in phases 9 and 10.

### Claude's Discretion

- Implementation of the version pre-read in `load()` (inline `JsonNode` peek vs. minimal POJO vs. map deserialization)
- Whether to introduce a `ConfigVersionException` as a new class or reuse/extend `ConfigNotInitializedException`
- Exact package structure within `infra/` for the relocated utilities

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CFG-01 | v3 config POJOs — `SourceEntry` (name, path, templates boolean, parameters), `DestinationEntry` (name, path, parameters, mappings), `MappingEntry` v3 (sourceRef, glob, recurse, sync, params) — with YAML round-trip and no data loss | D-04 through D-08; Jackson 2.19.2 YAML round-trip pattern documented below |
| CFG-02 | `ConfigService.load()` detects a v2 config (missing version field or version < 3) and fails with a clear error message instructing the user to recreate their config | D-09; `JsonNode` peek pattern documented below; `ConfigNotInitializedException` pattern as model |
| CFG-03 | All v2 CLI command packages (catalog, archetype, project, subscription) are removed from the codebase; running old command names produces "unknown command" error | D-12; all 4 packages identified on disk; deletion order in D-03; 2 infra files must relocate first |
</phase_requirements>

---

## Summary

Phase 8 is a structural refactoring phase. It replaces the v2 config schema (catalogs, archetypes, projects, subscriptions) with a v3 schema (sources, destinations, mappings), removes the v2 CLI command packages, relocates two shared utility classes to `infra/`, and adds a version guard in `ConfigService.load()`.

The codebase compiles cleanly and all 78 tests pass before this phase begins. The git working tree contains staged-but-uncommitted v2 work (patterns→archetypes rename) that is already incorporated into the on-disk source. The key risk is deletion order: `archetype/` contains two utilities (`ArchetypePathUtils`, `FreemarkerVariableExtractor`) imported by `GeneratorService` and `DefaultSyncService` — these must move to `infra/` before any package deletion or the build will break.

The Jackson YAML stack (jackson-dataformat-yaml 2.19.2) supports all required patterns: `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` with SnakeYAML 2.4, nested list/map fields, boolean defaults, and `readTree()` for version pre-reading.

**Primary recommendation:** Execute in strict dependency order — infra relocation, v3 POJO creation, ViracochaConfig replacement, version guard, test rewrite, package deletion, subcommand removal.

---

## Standard Stack

### Core (no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| jackson-dataformat-yaml | 2.19.2 | YAML serialize/deserialize POJOs | Already in use; handles all v3 field types |
| SnakeYAML | 2.4 | YAML parser underlying jackson-dataformat-yaml | Transitive; already runtime dependency |
| Lombok | (via micronaut-parent) | `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` | Already in use; annotation processors configured |
| JUnit 5 | (via micronaut-parent) | Unit tests for YAML round-trips and version guard | Already in use; test pattern established |

**No new dependencies are required for this phase.**

### Version Verification

```bash
# Confirmed via ./mvnw dependency:list (2026-05-08):
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.19.2
com.fasterxml.jackson.core:jackson-databind:jar:2.19.2
com.fasterxml.jackson.core:jackson-annotations:jar:2.19.2
org.yaml:snakeyaml:jar:2.4
```

---

## Architecture Patterns

### Established POJO Pattern (from existing codebase)

All model POJOs in `org.saltations.model` follow this pattern. Verified in `ArchetypeEntry.java`, `MappingEntry.java`, `ProjectEntry.java`:

```java
// Source: src/main/java/org/saltations/model/ArchetypeEntry.java (verified)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchetypeEntry {
    private String name;
    private String path;
    private List<String> parameters = new ArrayList<>();
}
```

v3 POJOs follow the identical pattern. All three new POJOs go in `org.saltations.model`.

### POJO Field Defaults with @Data

For boolean fields, Jackson YAML uses Java defaults unless a YAML key is present. The default `false` for `templates`, `recurse`, and `sync` is correct Java field initialization:

```java
// Boolean false default — no annotation needed
private boolean templates = false;
private boolean recurse = false;
private boolean sync = false;

// Null default for optional String
private String glob = null;
```

Jackson `ObjectMapper` with `YAMLFactory` reads missing YAML keys as the Java default value — no `@JsonProperty(defaultValue=...)` needed for primitives.

### Map Field Pattern (from existing codebase)

`ProjectEntry.java` and `MappingEntry.java` both use `LinkedHashMap` for `parameters`. Use the same pattern for v3:

```java
// Source: src/main/java/org/saltations/model/MappingEntry.java (verified)
private Map<String, String> parameters = new LinkedHashMap<>();
```

Use `LinkedHashMap` (not `HashMap`) to preserve YAML key insertion order, matching the existing codebase convention.

### Nested List-of-Objects (from existing codebase)

`ProjectEntry` nests `List<MappingEntry>` and `List<SubscriptionEntry>`, which jackson-dataformat-yaml handles via polymorphic deserialization of known types. The same pattern works for `DestinationEntry` nesting `List<MappingEntry>`:

```java
// Source: src/main/java/org/saltations/model/ProjectEntry.java (verified)
private List<MappingEntry> mappings = new ArrayList<>();
```

### Version Guard: JsonNode Peek Pattern

The `ConfigService` already uses a single `ObjectMapper yaml = new ObjectMapper(new YAMLFactory())` instance. The recommended approach for the version pre-read is `readTree()` on the same mapper, which avoids constructing a separate mapper or POJO:

```java
// Pattern for ConfigService.load() — version peek before full deserialization
public ViracochaConfig load() throws IOException {
    Path configFile = xdgPaths.configFile();
    if (!Files.exists(configFile)) {
        throw new ConfigNotInitializedException();
    }
    // Version pre-read using JsonNode (avoids constructing second ObjectMapper)
    JsonNode root = yaml.readTree(configFile.toFile());
    JsonNode versionNode = root.get("version");
    int version = (versionNode == null || versionNode.isNull()) ? 0 : versionNode.asInt(0);
    if (version < 3) {
        throw new ConfigVersionException(version);
    }
    return yaml.readValue(configFile.toFile(), ViracochaConfig.class);
}
```

`ObjectMapper.readTree(File)` is available in jackson-databind 2.x (HIGH confidence — verified in codebase at 2.19.2). The file is read twice — acceptable because config files are tiny (< 1KB typical).

### ConfigVersionException Pattern

`ConfigNotInitializedException` is `extends RuntimeException`. For `ConfigVersionException`, the decision (D-09) says "extends IOException or is a checked exception." Given that `load()` already declares `throws IOException`, a checked exception extending `IOException` or a `RuntimeException` both work. The `ConfigNotInitializedException` pattern (RuntimeException) is simpler but checked (`IOException`) is more explicit. Either is valid — this is Claude's Discretion.

**Recommendation (checked, extends IOException):** Keeps `load()`'s `throws IOException` signature unchanged and is more explicit about the failure cause:

```java
package org.saltations.config;

import java.io.IOException;

/**
 * Thrown by ConfigService.load() when the config file version is < 3.
 * Commands must catch this and print the message, then return exit code 1.
 */
public class ConfigVersionException extends IOException {

    public ConfigVersionException(int foundVersion) {
        super("Config file is v" + foundVersion
            + " — v3 format required. Delete ~/.config/viracocha/config.yaml"
            + " and run 'vira config init' to start fresh.");
    }
}
```

### Utility Relocation Pattern

`HiddenPathFilter` (renamed from `ArchetypePathUtils`) goes in `org.saltations.infra`. It is a `public final` utility class with a private constructor — no DI, no annotations needed. `FreemarkerVariableExtractor` keeps its name and also moves to `org.saltations.infra`. Both are plain Java classes with no Micronaut annotations, so relocation is a package rename only.

The import update in `GeneratorService` and `DefaultSyncService`:

```java
// Before:
import org.saltations.archetype.ArchetypePathUtils;
// After:
import org.saltations.infra.HiddenPathFilter;
```

```java
// Before reference:
ArchetypePathUtils.hasHiddenPathSegment(root, p)
// After reference:
HiddenPathFilter.hasHiddenPathSegment(root, p)
```

`FreemarkerVariableExtractor` references `ArchetypePathUtils` internally — that import must also be updated to `HiddenPathFilter` after relocation.

### Subcommand Removal Pattern (picocli)

Subcommands are declared statically in `@Command(subcommands = {...})`. Removal is a two-step operation:
1. Remove the class from `subcommands = {}` in `ViracochaCommand.java`
2. Remove the `import` statement for the removed command class
3. Verify the deleted class files are gone from disk (otherwise the import still resolves)

The order matters: class file must be deleted before the import is removed, or compilation may succeed but the file lingers.

### reflect-config.json Update Pattern

The file at `src/main/resources/META-INF/native-image/org.saltations/viracocha/reflect-config.json` currently contains 5 entries (ViracochaConfig, CatalogEntry, ArchetypeEntry, ProjectEntry, MappingEntry). Replace all 5 with v3 entries:

```json
[
  { "name": "org.saltations.model.ViracochaConfig",
    "allDeclaredConstructors": true, "allDeclaredFields": true, "allDeclaredMethods": true },
  { "name": "org.saltations.model.SourceEntry",
    "allDeclaredConstructors": true, "allDeclaredFields": true, "allDeclaredMethods": true },
  { "name": "org.saltations.model.DestinationEntry",
    "allDeclaredConstructors": true, "allDeclaredFields": true, "allDeclaredMethods": true },
  { "name": "org.saltations.model.MappingEntry",
    "allDeclaredConstructors": true, "allDeclaredFields": true, "allDeclaredMethods": true }
]
```

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| YAML version pre-read | Custom YAML parser or string scan | `ObjectMapper.readTree()` from existing Jackson instance | readTree() is O(1) for the root node; string scanning misses edge cases (comments, multiline values) |
| YAML round-trip with nested maps | Custom serializer | `LinkedHashMap<String, String>` + Jackson default serialization | Jackson handles Map<String,String> serialization natively; no custom serializers needed |
| Boolean YAML defaults | `@JsonProperty(defaultValue=...)` | Java field initializer `= false` | Jackson reads missing YAML keys as the Java default; no annotation needed for primitives |

---

## Common Pitfalls

### Pitfall 1: Deleting archetype/ Before Moving Utilities

**What goes wrong:** Compiling after deleting `archetype/` while `GeneratorService` and `DefaultSyncService` still import `org.saltations.archetype.ArchetypePathUtils` causes a compilation error that blocks the build.

**Why it happens:** `FreemarkerVariableExtractor` internally references `ArchetypePathUtils`. Both files must move together.

**How to avoid:** Follow D-03 strictly — move both utilities to `infra/` and compile, THEN delete `archetype/`.

**Warning signs:** Any compilation error referencing `org.saltations.archetype` after a file deletion.

### Pitfall 2: Tests Referencing Deleted v2 Model Classes

**What goes wrong:** Existing tests in `catalog/`, `archetype/`, `project/`, `subscription/` and `model/` packages still reference `CatalogEntry`, `ArchetypeEntry`, `ProjectEntry`, `SubscriptionEntry`. Deleting these model classes breaks those tests.

**Why it happens:** The test files for deleted packages must be deleted along with the packages. The model tests (`ViracochaConfigTest`, `ViracochaConfigTypedListTest`) must be rewritten for v3.

**How to avoid:** Delete test files in the same step as the corresponding production package. Rewrite `ViracochaConfigTest`, `ViracochaConfigTypedListTest`, `ViracochaConfigProjectTypedListTest` to reference v3 POJOs.

**Warning signs:** `TEST FAILURES` in catalog, archetype, project, or subscription test packages after model class deletion.

### Pitfall 3: ViracochaConfig init() Still Writes version:1

**What goes wrong:** After updating `ViracochaConfig` to default `version = 3`, the `ConfigService.init()` must call `save(new ViracochaConfig())` and the new default produces `version: 3`. But existing tests in `ConfigServiceTest` assert `version: 1` (e.g., `assertEquals(1, config.getVersion())`).

**Why it happens:** The `ConfigServiceTest` hardcodes `version: 1` assertions.

**How to avoid:** Update all affected test assertions to `version: 3` when updating `ViracochaConfig`.

**Warning signs:** `ConfigServiceTest.initCreatesConfigFile` failing with "Expected version: 1 but got: 3".

### Pitfall 4: GeneratorService and DefaultSyncService Compilation After Stub State

**What goes wrong:** `GeneratorService` and `DefaultSyncService` reference v2 model classes (`ProjectEntry`, `ArchetypeEntry`, `CatalogEntry`, `SubscriptionEntry`) extensively. When these model classes are deleted, both services will fail to compile.

**Why it happens:** Phase 8 stubs these services — they must compile but their bodies will be semantically broken (references to non-existent types). The CONTEXT.md says "GeneratorService and DefaultSyncService are NOT rewritten here — they survive as stubs until phases 11 and 12."

**How to avoid:** Replace `GeneratorService` and `DefaultSyncService` with stub implementations that compile but throw `UnsupportedOperationException`. Their bodies referencing v2 models cannot survive unchanged.

**Warning signs:** Compilation errors in `generate/` or `sync/` packages after model class deletion.

### Pitfall 5: Map<String,String> YAML Deserialization Without Type Info

**What goes wrong:** Jackson's YAML deserializer needs to know the Map type. Without it, maps may deserialize as `Map<String, Object>` causing `ClassCastException` at runtime.

**Why it happens:** Generic type erasure at runtime.

**How to avoid:** Declare fields explicitly as `Map<String, String>` (not `Map`). With Lombok `@Data`, Jackson's databind uses the field type for deserialization. Verified in existing `MappingEntry` and `ProjectEntry`.

**Warning signs:** `ClassCastException` when reading map values as String in generated code.

### Pitfall 6: ConfigVersionException Catch in ShowConfigCommand

**What goes wrong:** `ShowConfigCommand.call()` catches `ConfigNotInitializedException` explicitly. After the version guard, it must also catch `ConfigVersionException` (or let it propagate as an `IOException` and be caught by the `IOException` handler).

**Why it happens:** The existing `ShowConfigCommand` has a specific catch block for `ConfigNotInitializedException`. If `ConfigVersionException extends IOException`, it will be caught by the `IOException` catch block — but with a generic "Error reading config:" prefix. If the display should say the actual version message, the `ConfigVersionException` must be caught first or the message preserved.

**How to avoid:** If `ConfigVersionException extends IOException`, the existing `IOException` catch in `ShowConfigCommand` prints `e.getMessage()` which will contain the correct version message. Verify the output matches the success criterion: "A v2 config file causes `vira` to print a clear error message."

---

## Code Examples

Verified patterns from existing codebase:

### YAML Round-Trip Test Pattern (existing)

```java
// Source: src/test/java/org/saltations/model/ViracochaConfigTest.java (verified)
private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

@Test
void configRoundTripPreservesVersion() throws Exception {
    ViracochaConfig original = new ViracochaConfig();
    String yaml_str = yaml.writeValueAsString(original);
    ViracochaConfig deserialized = yaml.readValue(yaml_str, ViracochaConfig.class);
    assertEquals(original.getVersion(), deserialized.getVersion());
}
```

v3 round-trip tests follow the same pattern with v3 fields.

### ConfigService Test Stub for XdgPaths (existing pattern)

```java
// Source: src/test/java/org/saltations/config/ConfigServiceTest.java (verified)
XdgPaths xdgPaths = new XdgPaths() {
    @Override
    public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
    @Override
    public Path configDir() { return tempDir.resolve("viracocha"); }
    @Override
    public Path dataDir() { return tempDir.resolve("share").resolve("viracocha"); }
};
configService = new ConfigService(xdgPaths);
```

New tests for `ConfigVersionException` follow this same inline-stub pattern.

### Version Guard Test (new, using existing infrastructure)

```java
@Test
void loadThrowsConfigVersionExceptionForV2Config() throws Exception {
    // Write a v2 config file manually
    configService.init();
    Path configFile = configService.xdgPaths().configFile();
    Files.writeString(configFile, "version: 1\ncatalogs: []\narchetypes: []\nprojects: []\n");
    assertThrows(ConfigVersionException.class,
        () -> configService.load(),
        "load() must throw ConfigVersionException when version < 3");
}

@Test
void loadThrowsConfigVersionExceptionForMissingVersionField() throws Exception {
    configService.init();
    Path configFile = configService.xdgPaths().configFile();
    Files.writeString(configFile, "catalogs: []\n"); // no version field
    assertThrows(ConfigVersionException.class,
        () -> configService.load());
}
```

---

## Runtime State Inventory

This phase is a refactor/rename. Checking all 5 categories.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | `~/.config/viracocha/config.yaml` on developer machines will be v1 or v2. The version guard in `load()` covers this — it will fail with a clear message on first use after upgrade. | Code edit (version guard); no data migration (intentional clean break per requirements) |
| Live service config | None — viracocha is a CLI tool; no persistent daemon, no n8n, no external service config | None |
| OS-registered state | None — no Task Scheduler, systemd, pm2, or launchd registrations | None |
| Secrets/env vars | None — no secret keys reference class names or package names | None |
| Build artifacts | `target/` directory contains compiled classes from v2. Will be stale after v3 refactor — Maven `clean` before first build resolves this. | Run `./mvnw clean` before Phase 8 work begins |

**Nothing found in categories 2–4.** Runtime risk is limited to the developer's own config file on disk, which the version guard handles gracefully.

---

## Validation Architecture

nyquist_validation is enabled (config.json `workflow.nyquist_validation: true`).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) via `junit-jupiter-api` + `junit-jupiter-engine` |
| Config file | None — Maven Surefire auto-detects JUnit 5 via `junit-jupiter-engine` on classpath |
| Quick run command | `./mvnw test -pl . -Dtest=ViracochaConfigTest,ConfigServiceTest -q` |
| Full suite command | `./mvnw test -q` |

**Baseline:** 78 tests, 0 failures (verified 2026-05-08 before Phase 8).

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CFG-01 | SourceEntry, DestinationEntry, MappingEntry v3 YAML round-trip | unit | `./mvnw test -Dtest=ViracochaConfigV3Test,ViracochaConfigV3TypedListTest -q` | Wave 0 |
| CFG-01 | ViracochaConfig v3 default serializes to YAML with version:3, sources, destinations | unit | `./mvnw test -Dtest=ViracochaConfigV3Test -q` | Wave 0 |
| CFG-02 | ConfigService.load() throws ConfigVersionException for version < 3 | unit | `./mvnw test -Dtest=ConfigServiceTest -q` | Extend existing |
| CFG-02 | ConfigService.load() throws ConfigVersionException for missing version field | unit | `./mvnw test -Dtest=ConfigServiceTest -q` | Extend existing |
| CFG-02 | ShowConfigCommand prints version error message and exits 1 for v2 config | unit | `./mvnw test -Dtest=ShowConfigCommandTest -q` | Extend existing |
| CFG-03 | ViracochaCommand has no archetype/catalog/project/subscription subcommands | smoke | `./mvnw test -Dtest=ViracochaCommandTest -q` | Extend existing |

### Sampling Rate

- **Per task commit:** `./mvnw test -q`
- **Per wave merge:** `./mvnw test -q`
- **Phase gate:** Full suite green (`./mvnw test -q`) before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/org/saltations/model/ViracochaConfigV3Test.java` — covers CFG-01 (v3 ViracochaConfig round-trip with version:3, sources, destinations)
- [ ] `src/test/java/org/saltations/model/ViracochaConfigV3TypedListTest.java` — covers CFG-01 (SourceEntry, DestinationEntry, MappingEntry v3 list fields round-trip)

Note: Existing `ViracochaConfigTest.java`, `ViracochaConfigTypedListTest.java`, `ViracochaConfigProjectTypedListTest.java` must be deleted or rewritten (they reference v2 model classes that will no longer exist).

---

## Existing Test Files That Will Break (must be deleted with their packages)

The following test files reference v2 model classes or command classes that will be deleted. They must be deleted as part of their respective package deletion steps:

| Test File | Reason |
|-----------|--------|
| `src/test/java/org/saltations/catalog/ListCatalogsCommandTest.java` | CatalogCommand deleted |
| `src/test/java/org/saltations/catalog/RegisterCatalogCommandTest.java` | CatalogCommand deleted |
| `src/test/java/org/saltations/catalog/ShowCatalogCommandTest.java` | CatalogCommand deleted |
| `src/test/java/org/saltations/catalog/UnregisterCatalogCommandTest.java` | CatalogCommand deleted |
| `src/test/java/org/saltations/project/AddMappingAndShowProjectTest.java` | ProjectCommand deleted |
| `src/test/java/org/saltations/project/ProjectCommandsTest.java` | ProjectCommand deleted |
| `src/test/java/org/saltations/subscription/AddSubscriptionCommandTest.java` | SubscriptionCommand deleted |
| `src/test/java/org/saltations/subscription/SubscriptionListShowRemoveTest.java` | SubscriptionCommand deleted |
| `src/test/java/org/saltations/model/ViracochaConfigTest.java` | References v2 model (version:1, catalogs, archetypes) |
| `src/test/java/org/saltations/model/ViracochaConfigTypedListTest.java` | References CatalogEntry, ArchetypeEntry |
| `src/test/java/org/saltations/model/ViracochaConfigProjectTypedListTest.java` | References ProjectEntry, MappingEntry v2 |

Note: There are no `archetype/` test files on disk (the archetype package was renamed from pattern/ in prior uncommitted work). The `src/test/java/org/saltations/` directory has no `archetype/` subdirectory.

---

## Current Working Tree State (Critical Pre-Condition)

The git working tree contains uncommitted changes that represent partial v2 work already done. These are the **starting state** for Phase 8 — not changes Phase 8 needs to make:

- `ViracochaConfig.java` already has `List<ArchetypeEntry> archetypes` with `@JsonAlias("patterns")` — the `patterns` package has been deleted
- `ViracochaCommand.java` still imports and registers `ArchetypeCommand`, `CatalogCommand`, `ProjectCommand`, `SubscriptionCommand`
- `GeneratorService.java` and `DefaultSyncService.java` still import `org.saltations.archetype.ArchetypePathUtils`
- `archetype/` package still exists on disk (the files have not been moved yet)
- All 78 tests currently pass with this state

Phase 8 starts from this clean-compiling state and moves forward to v3.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 8 is code/config changes only. No external services, databases, runtimes, or CLI tools beyond the existing JDK 21 + Maven are required.

The build environment is verified:

| Dependency | Available | Version | Notes |
|------------|-----------|---------|-------|
| JDK 21 | Yes | 21 (Temurin) | Required; confirmed via successful `./mvnw compile` |
| Maven wrapper | Yes | 3.x via `./mvnw` | Required; `./mvnw` present |
| jackson-dataformat-yaml | Yes | 2.19.2 | In pom.xml; confirmed via dependency:list |

---

## Sources

### Primary (HIGH confidence)

- Codebase direct read — all patterns verified from live source files
- `src/main/java/org/saltations/model/` — all v2 POJO patterns
- `src/main/java/org/saltations/config/ConfigService.java` — version guard insertion point
- `src/main/java/org/saltations/config/ConfigNotInitializedException.java` — exception pattern to follow
- `src/test/java/org/saltations/config/ConfigServiceTest.java` — test stub pattern for XdgPaths
- `src/test/java/org/saltations/model/ViracochaConfigTest.java` — round-trip test pattern
- `./mvnw dependency:list` output (2026-05-08) — confirmed Jackson 2.19.2, SnakeYAML 2.4
- `./mvnw test` output (2026-05-08) — confirmed 78 tests, 0 failures baseline

### Secondary (MEDIUM confidence)

- Jackson `ObjectMapper.readTree(File)` — documented in Jackson databind API; available since 2.x; no external verification needed given codebase already uses `ObjectMapper.readValue(File, Class)`

---

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — no new dependencies; all libraries verified in dependency:list
- Architecture: HIGH — all patterns verified directly from existing codebase source files
- Pitfalls: HIGH — identified from concrete analysis of import dependencies and test file contents
- Test infrastructure: HIGH — `./mvnw test` run with 78/78 passing as baseline

**Research date:** 2026-05-08
**Valid until:** 2026-06-08 (stable Java/Maven ecosystem; dependency versions unlikely to change)
