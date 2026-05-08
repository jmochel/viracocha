# Architecture Research

**Domain:** Java CLI workspace management tool (Viracocha / `vira`)
**Stack:** JDK 21, Micronaut 4.10.x, picocli, Freemarker, jackson-dataformat-yaml, Logback
**Researched:** 2026-05-08
**Confidence:** HIGH (based on direct code analysis of the existing codebase)

---

## v3 Architecture: What This Document Covers

v3 replaces the v2 catalog/archetype/project/subscription model with a unified sources/destinations model. This document describes:

1. The v3 config schema and model POJOs
2. Which existing components are **removed**, **replaced**, or **kept/modified**
3. New component responsibilities
4. Data flow for all four main operations
5. The build order respecting dependencies
6. Remote HTTP source integration pattern

---

## Component Disposition: New vs Modified vs Removed

### Removed (v3 deletes the entire package)

| Package | Classes | Why Removed |
|---------|---------|-------------|
| `org.saltations.archetype` | `ArchetypeCommand`, `RegisterArchetypeCommand`, `ListArchetypesCommand`, `ShowArchetypeCommand`, `UnregisterArchetypeCommand`, `ArchetypePathUtils`, `FreemarkerVariableExtractor` | Archetypes become sources with `templates: true`; functionality migrates to `SourceService` |
| `org.saltations.catalog` | `CatalogCommand`, `RegisterCatalogCommand`, `ListCatalogsCommand`, `ShowCatalogCommand`, `UnregisterCatalogCommand` | Catalogs become sources with `templates: false` |
| `org.saltations.project` | `ProjectCommand`, `CreateProjectCommand`, `ListProjectsCommand`, `ShowProjectCommand`, `UnregisterProjectCommand`, `AddMappingCommand` | Projects become destinations; mapping management moves to `destination/` |
| `org.saltations.subscription` | `SubscriptionCommand`, `AddSubscriptionCommand`, `ListSubscriptionsCommand`, `ShowSubscriptionCommand`, `RemoveSubscriptionCommand` | Subscriptions replaced by `sync: true` flag on `MappingEntry` |

### Replaced (same responsibility, new implementation)

| Old Class | New Class | Change |
|-----------|-----------|--------|
| `model/ViracochaConfig.java` | `model/ViracochaConfig.java` | New fields: `sources[]`, `destinations[]`; remove `catalogs`, `archetypes`, `projects` |
| `model/MappingEntry.java` | `model/MappingEntry.java` | New fields: `name`, `source`, `dest`, `glob`, `recurse`, `sync`; remove `archetypeName`, `workspacePath`, `parameters` |
| `generate/GeneratorService.java` | `generate/GeneratorService.java` | Rewritten: destination→mapping→source traversal; glob+recurse; remote fetch path; `templates` flag gates Freemarker |
| `sync/DefaultSyncService.java` | `sync/DefaultSyncService.java` | Rewritten: mapping-driven (not subscription-driven); source→destination only; remove bidirectional |
| `sync/SyncService.java` (interface) | `sync/SyncService.java` | New signature: `syncDestination(name, mappingNameOrNull, dryRun, verbose)` |
| `sync/SyncCommand.java` | `sync/SyncCommand.java` | New option: `--destination` (was `--project-name`); remove `--subscription` |
| `generate/GenerateCommand.java` | `generate/GenerateCommand.java` | New option: `--destination` (was `--project-name`) |
| `ViracochaCommand.java` | `ViracochaCommand.java` | New subcommands: `SourceCommand`, `DestinationCommand`; remove archetype/catalog/project/subscription |
| `model/ArchetypeEntry.java` | `model/SourceEntry.java` | Name change; add `templates` flag |
| `model/ProjectEntry.java` | `model/DestinationEntry.java` | Name change; mappings now reference source by name; add `parameters` map |

### Kept Unchanged

| Class | Notes |
|-------|-------|
| `config/ConfigService.java` | Load/save mechanics identical; only the POJO type changes |
| `config/ConfigCommand.java` | No change needed |
| `config/InitCommand.java` | No change needed |
| `config/ShowConfigCommand.java` | May need display updates for new schema |
| `config/ConfigNotInitializedException.java` | Unchanged |
| `generate/PathExpander.java` | Unchanged — still used for `${var}` path segments when `templates: true` |
| `generate/GenerationResult.java` | Unchanged — `(generated, skipped, failed, verboseLines)` record still fits |
| `infra/XdgPaths.java` | Unchanged |
| `sync/SyncConflictKind.java` | Unchanged |
| `sync/SyncConflictRecord.java` | May need minor rename (subscriptionId → mappingName) |
| `sync/SyncEngineResult.java` | Needs minor rename (subscriptionResults → mappingResults) |

### New Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `SourceEntry.java` | `model/` | POJO: name, path, templates |
| `DestinationEntry.java` | `model/` | POJO: name, path, parameters, mappings |
| `SourceCommand.java` | `source/` | Group command: `vira source` |
| `RegisterSourceCommand.java` | `source/` | `vira source register` |
| `ListSourcesCommand.java` | `source/` | `vira source list` |
| `ShowSourceCommand.java` | `source/` | `vira source show` |
| `RemoveSourceCommand.java` | `source/` | `vira source remove` |
| `SourceService.java` | `source/` | CRUD + Freemarker variable extraction |
| `DestinationCommand.java` | `destination/` | Group command: `vira destination` |
| `RegisterDestinationCommand.java` | `destination/` | `vira destination register` |
| `ListDestinationsCommand.java` | `destination/` | `vira destination list` |
| `ShowDestinationCommand.java` | `destination/` | `vira destination show` |
| `RemoveDestinationCommand.java` | `destination/` | `vira destination remove` |
| `AddMappingCommand.java` | `destination/` | `vira destination add-mapping` |
| `ListMappingsCommand.java` | `destination/` | `vira destination list-mappings` |
| `RemoveMappingCommand.java` | `destination/` | `vira destination remove-mapping` |
| `DestinationService.java` | `destination/` | CRUD for destinations and their mappings |
| `RemoteFetcher.java` | `infra/` | HTTP GET bytes; returns `byte[]` for a single URL |
| `GlobMatcher.java` | `infra/` | Wraps `FileSystem.getPathMatcher("glob:...")` for filename evaluation |

---

## v3 Config Schema

```yaml
version: 3

sources:
  - name: sample-archetype
    path: /home/jmochel/sample-archetype
    templates: true
  - name: idioms
    path: /home/jmochel/ai-idioms
    templates: false
  - name: cc-user-global
    path: /home/jmochel/ai-idioms/claude/user-global
    templates: false
  - name: cursor-project-rules
    path: /home/jmochel/ai-idioms/cursor/project/rules
    templates: false
  - name: claude-gha-review-repo
    path: https://raw.githubusercontent.com/anthropics/claude-code-action
    templates: false

destinations:
  - name: cc-user
    path: /home/jmochel/.claude
    parameters:
      key1: value1
    mappings:
      - name: all
        source: cc-user-global
        dest: .
        glob: "*"
        sync: false
        recurse: true

  - name: the-app-project
    path: /home/jmochel/pers/the-app
    mappings:
      - name: archetype
        source: sample-archetype
        dest: .
        sync: false
        recurse: true
      - name: general-rules
        source: cursor-project-rules
        dest: .cursor/rules
        glob: '[A-Za-z]+*.mdc'
        sync: true
        recurse: false
      - name: gha-review-action
        source: claude-gha-review-repo
        dest: .
        glob: 'main/action.yml'
        sync: false
        recurse: false
```

### Schema Field Reference

**`SourceEntry`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | yes | Unique identifier used in mapping `source` references |
| `path` | String | yes | Absolute local path or HTTP(S) URL |
| `templates` | boolean | yes | If `true`, files are processed through Freemarker before writing |

**`DestinationEntry`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | yes | Unique identifier for CLI targeting |
| `path` | String | yes | Absolute path to destination root (created if absent) |
| `parameters` | Map\<String,String\> | no | Freemarker template variables available to all mappings |
| `mappings` | List\<MappingEntry\> | no | Ordered copy/sync rules for this destination |

**`MappingEntry`**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | String | yes | — | Unique within destination; CLI targeting |
| `source` | String | yes | — | Must match a `SourceEntry.name` |
| `dest` | String | yes | — | Relative path within destination root; `.` means root |
| `glob` | String | no | `*` | Filename glob pattern applied to source files |
| `recurse` | boolean | no | `false` | Walk subdirectories of source path |
| `sync` | boolean | no | `false` | Included in `vira sync` runs |

---

## Model POJOs

```java
// model/ViracochaConfig.java
@Data
public class ViracochaConfig {
    private int version = 3;
    private List<SourceEntry> sources = new ArrayList<>();
    private List<DestinationEntry> destinations = new ArrayList<>();
}

// model/SourceEntry.java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceEntry {
    private String name;
    private String path;            // local path or HTTP(S) URL
    private boolean templates;
    private List<String> parameters = new ArrayList<>();  // extracted at register if templates:true
}

// model/DestinationEntry.java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DestinationEntry {
    private String name;
    private String path;
    private Map<String, String> parameters = new LinkedHashMap<>();
    private List<MappingEntry> mappings = new ArrayList<>();
}

// model/MappingEntry.java  (replaces v2 version entirely)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MappingEntry {
    private String name;
    private String source;          // references SourceEntry.name
    private String dest;            // relative path within destination root
    private String glob;            // default "*"
    private boolean recurse;        // default false
    private boolean sync;           // default false
}
```

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLI Layer (picocli)                          │
│  ViracochaCommand                                                    │
│  ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────┐           │
│  │ config/* │ │  source/*    │ │ dest/*   │ │generate  │  sync/   │ │
│  └────┬─────┘ └──────┬───────┘ └────┬─────┘ └────┬─────┘  └──┬───┘ │
└───────┼───────────────┼──────────────┼────────────┼────────────┼────┘
        │               │              │            │            │
┌───────┼───────────────┼──────────────┼────────────┼────────────┼────┐
│       │           Service Layer      │            │            │    │
│  ┌────▼─────┐ ┌───────▼──────┐ ┌────▼──────┐ ┌──▼──────┐ ┌───▼──┐ │
│  │ConfigSvc │ │SourceService │ │DestSvc    │ │Generator│ │Sync  │ │
│  └────┬─────┘ └───────┬──────┘ └────┬──────┘ │Service  │ │Svc   │ │
│       │               │             │        └──┬──────┘ └───┬──┘ │
└───────┼───────────────┼─────────────┼───────────┼────────────┼────┘
        │               │             │           │            │
┌───────┼───────────────┼─────────────┼───────────┼────────────┼────┐
│       │          Infrastructure     │           │            │    │
│  ┌────▼────┐ ┌────────┴─────────────┴───────────┴────────────┘    │
│  │XdgPaths │ │  GlobMatcher  PathExpander  RemoteFetcher           │
│  └─────────┘ └───────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
        │                        │
┌───────▼────────┐      ┌────────▼────────┐
│  config.yaml   │      │  Local FS /     │
│  (XDG YAML)    │      │  Remote HTTP(S) │
└────────────────┘      └─────────────────┘
```

### Component Responsibilities

| Component | Package | Responsibility |
|-----------|---------|----------------|
| `ViracochaCommand` | `org.saltations` | Root CLI entry point; declares all subcommand groups |
| `ConfigService` | `config/` | Load/save `ViracochaConfig` YAML; XDG path resolution |
| `SourceService` | `source/` | CRUD for `SourceEntry`; Freemarker variable extraction for `templates:true` |
| `DestinationService` | `destination/` | CRUD for `DestinationEntry` and nested `MappingEntry` |
| `GeneratorService` | `generate/` | Copy engine: destination→mapping→source traversal; glob+recurse; Freemarker; skip-existing |
| `DefaultSyncService` | `sync/` | Sync engine: mapping-driven; source→destination only; conflict detection |
| `XdgPaths` | `infra/` | Resolves `~/.config/viracocha/config.yaml` and log paths |
| `GlobMatcher` | `infra/` | Applies `mapping.glob` to filenames via `FileSystem.getPathMatcher` |
| `RemoteFetcher` | `infra/` | HTTP GET to fetch single file bytes; read-only |
| `PathExpander` | `generate/` | Expands `${var}` Freemarker expressions in path segments |
| Model POJOs | `model/` | `ViracochaConfig`, `SourceEntry`, `DestinationEntry`, `MappingEntry` — no logic |

---

## Recommended Package Structure

```
org.saltations/
  ViracochaCommand.java             ← REPLACED: new subcommand list

  config/                           ← UNCHANGED (4 files)
    ConfigCommand.java
    InitCommand.java
    ShowConfigCommand.java
    ConfigService.java
    ConfigNotInitializedException.java

  source/                           ← NEW PACKAGE
    SourceCommand.java
    RegisterSourceCommand.java
    ListSourcesCommand.java
    ShowSourceCommand.java
    RemoveSourceCommand.java
    SourceService.java

  destination/                      ← NEW PACKAGE
    DestinationCommand.java
    RegisterDestinationCommand.java
    ListDestinationsCommand.java
    ShowDestinationCommand.java
    RemoveDestinationCommand.java
    AddMappingCommand.java
    ListMappingsCommand.java
    RemoveMappingCommand.java
    DestinationService.java

  generate/                         ← MODIFIED
    GenerateCommand.java            ← option rename: --destination
    GeneratorService.java           ← full rewrite
    PathExpander.java               ← UNCHANGED
    GenerationResult.java           ← UNCHANGED

  sync/                             ← MODIFIED
    SyncCommand.java                ← option rename: --destination
    SyncService.java                ← interface signature change
    DefaultSyncService.java         ← full rewrite
    SyncConflictKind.java           ← UNCHANGED
    SyncConflictRecord.java         ← minor: subscriptionId → mappingName
    SyncEngineResult.java           ← minor: subscriptionResults → mappingResults

  model/                            ← REPLACED
    ViracochaConfig.java            ← sources[], destinations[]
    SourceEntry.java                ← NEW (was ArchetypeEntry/CatalogEntry)
    DestinationEntry.java           ← NEW (was ProjectEntry)
    MappingEntry.java               ← REPLACED (all fields change)

  infra/                            ← EXTENDED
    XdgPaths.java                   ← UNCHANGED
    GlobMatcher.java                ← NEW
    RemoteFetcher.java              ← NEW

  DELETED packages:
    archetype/  (7 files)
    catalog/    (5 files)
    project/    (6 files)
    subscription/ (5 files)
```

---

## Data Flow

### `vira source register --name cursor-rules --path /home/jmochel/ai-idioms/cursor/rules`

```
RegisterSourceCommand.run()
  → ConfigService.load()
  → validate: name not already in config.sources
  → validate: path is existing local dir OR valid HTTP(S) URL (no filesystem check for URLs)
  → if --templates: SourceService.extractParameters(path) using FreemarkerVariableExtractor logic
  → SourceEntry{name, path, templates, parameters?}
  → config.sources.add(entry)
  → ConfigService.save(config)
  → print: "Source 'cursor-rules' registered."
```

**Note:** `FreemarkerVariableExtractor` logic migrates into `SourceService` — the extractor class is deleted with the archetype package, but the regex + walk logic is reproduced in `SourceService.extractParameters()`.

### `vira destination add-mapping --destination the-app --name java-rules --source cursor-rules --dest .cursor/rules --glob '3[0-9]+*.mdc' --sync`

```
AddMappingCommand.run()
  → ConfigService.load()
  → validate: destination 'the-app' exists in config.destinations
  → validate: source 'cursor-rules' exists in config.sources
  → validate: no '..' in dest path
  → validate: no duplicate mapping name within destination
  → MappingEntry{name="java-rules", source="cursor-rules", dest=".cursor/rules",
                 glob="3[0-9]+*.mdc", recurse=false, sync=true}
  → destination.mappings.add(entry)
  → ConfigService.save(config)
  → print: "Mapping 'java-rules' added to destination 'the-app'."
```

### `vira generate --destination the-app-project`

```
GenerateCommand.run()
  → GeneratorService.generate("the-app-project", dryRun, verbose)
      → ConfigService.load()
      → find DestinationEntry by name (throw if absent)
      → resolve destination.path → create if absent
      → for each mapping in destination.mappings:
          → find SourceEntry by mapping.source (throw if absent)
          → resolve file list:
              → if source.path is local:
                  → Files.walk(sourcePath, recurse ? MAX_DEPTH : 1)
                  → filter: !hasHiddenSegment, isRegularFile
                  → filter: GlobMatcher.matches(mapping.glob, filename)
              → if source.path is HTTP(S):
                  → RemoteFetcher.fetchIndex(url) → List<String> relative paths
                  → filter by glob on final path segment
          → merge params: destination.parameters (base) [no source-level params override in v3]
          → for each resolved file:
              → compute relPath from source root
              → expand path segments (PathExpander) if source.templates == true
              → compute absolute target: destination.path / mapping.dest / relPath
              → if target escapes destination.path: log Failed, continue
              → if target exists as file: log Skipped, continue
              → if target exists as directory: log Failed, continue
              → if source.templates == true:
                  → content = Files.readString(sourceFile)
                  → rendered = Freemarker.process(content, destination.parameters)
                  → if !dryRun: Files.writeString(target, rendered)
              → else:
                  → if !dryRun: Files.copy(sourceFile, target)
              → generated++
      → return GenerationResult(generated, skipped, failed, verboseLines)
```

**Key changes from v2:**
- Loop is destinations→mappings→sources (was projects→mappings→archetypes)
- `glob` and `recurse` are per-mapping flags applied during file enumeration
- `templates` flag is per-source, not per-mapping
- Remote sources enter at the file enumeration step via `RemoteFetcher`
- Parameter merge is destination.parameters only (no mapping-level overrides in v3)

### `vira sync --destination the-app-project`

```
SyncCommand.run()
  → DefaultSyncService.syncDestination("the-app-project", null, dryRun, verbose)
      → ConfigService.load()
      → find DestinationEntry by name
      → filter: mappings where mapping.sync == true
      → for each sync-eligible mapping:
          → find SourceEntry by mapping.source
          → if source is remote: skip (remote sources are read-only targets for sync)
          → resolve file list from source (local walk + glob + recurse)
          → for each file:
              → compute absolute destination path
              → if dest file absent:
                  → if !dryRun: Files.copy(sourceFile, destFile, COPY_ATTRIBUTES)
                  → copied++
              → else if Files.mismatch(source, dest) == -1L:
                  → skipped++
              → else:
                  → conflict: abort this mapping, record CONTENT_MISMATCH
      → return SyncEngineResult with per-mapping results
      → exit 1 if any conflicts or failures
```

**Key changes from v2:**
- Driven by `mapping.sync == true` flag, not subscription list
- Only source→destination direction (no bidirectional, no workspace→catalog)
- Remote sources with `sync: true` are skipped (noted in output); local only
- Result aggregates by mapping name, not subscription id

---

## Remote Source Integration: Dedicated `RemoteFetcher`

Remote HTTP(S) sources enter the architecture as a separate read path rather than being inlined in `GeneratorService`. The rationale:

- `GeneratorService` already has significant branching for glob/recurse/templates. Adding HTTP fetch inline creates a third major branch that obscures the algorithm.
- `RemoteFetcher` can be tested independently and injected as a mock in unit tests.
- The same fetcher is reused by `DefaultSyncService` for remote-source sync eligibility checks.

```java
// infra/RemoteFetcher.java
@Singleton
public class RemoteFetcher {

    /**
     * Fetches the content of a single URL as bytes.
     * Throws RemoteFetchException (unchecked) on HTTP error or network failure.
     */
    public byte[] fetch(String url) { ... }

    /**
     * Returns true if the path string starts with http:// or https://
     */
    public static boolean isRemote(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }
}
```

`GeneratorService` and `DefaultSyncService` both call `RemoteFetcher.isRemote(source.getPath())` to branch between local walk and remote fetch. The remote fetch path for a single-file URL applies the glob to the URL path's final segment.

**Limitations of v3 remote support:**
- Single-file fetch only (one URL → one file)
- No directory listing from arbitrary HTTP servers
- GitHub raw URLs work only for specific file paths, not directory traversal
- Authentication is not supported — URLs must be public

---

## Micronaut + picocli Integration Pattern

Unchanged from v2. Every `@Command` class is annotated `@Singleton` so Micronaut manages injection. Subcommands are declared statically in `@Command(subcommands={...})`. `ViracochaCommand.main()` calls `PicocliRunner.execute()`.

```java
// Pattern for every command class
@Command(name = "register", description = "...", mixinStandardHelpOptions = true)
@Singleton
public class RegisterSourceCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--name"}, required = true)
    private String name;

    private final SourceService sourceService;

    @Inject
    public RegisterSourceCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        // validate → call service → print result
        return 0;
    }
}
```

Group commands (e.g. `SourceCommand`) hold no logic — they declare subcommands and return 0.

---

## Error Handling Strategy

| Error Condition | Exception Class | Exit Code |
|----------------|-----------------|-----------|
| Config not initialized | `ConfigNotInitializedException` (existing) | 1 |
| Source not found | `IllegalArgumentException` (inline message) | 1 |
| Destination not found | `IllegalArgumentException` (inline message) | 1 |
| Missing Freemarker parameter | `TemplateException` caught → verbose line | counted as failed |
| Remote fetch failure | `RemoteFetchException` (new unchecked) | 1 |
| Path traversal (dest has `..`) | `IllegalArgumentException` at add-mapping time | 2 |
| Target escapes destination root | counted as failed file in `GenerationResult` | 0 (non-zero if failed > 0) |
| Sync conflict | per-mapping abort; summary line to stderr | 1 |

Error messages follow the existing pattern: caught in command `call()`, printed to `spec.commandLine().getErr()`, returns non-zero.

---

## Build Order

Phase dependencies flow strictly downward. A phase's tests can only pass after its dependencies are in place.

```
Phase 1: Model + Config Foundation
  ├── ViracochaConfig.java (version:3, sources[], destinations[])
  ├── SourceEntry.java
  ├── DestinationEntry.java
  ├── MappingEntry.java (v3 fields)
  ├── ConfigService.java (only POJO class reference changes)
  └── ViracochaCommand.java (remove old subcommands; add SourceCommand, DestinationCommand stubs)
  Rationale: all other phases depend on these POJOs and config load/save round-trips.
  Tests: config round-trip YAML serialization with version:3 schema.

Phase 2: Infrastructure Utilities
  ├── GlobMatcher.java (wraps PathMatcher; testable independently)
  └── RemoteFetcher.java (HTTP GET; testable with live URL or mock)
  Rationale: GeneratorService and SyncService both need these. No other dependencies.
  Tests: GlobMatcher unit tests; RemoteFetcher integration test with a known public URL.

Phase 3: Sources
  ├── SourceService.java (CRUD + extractParameters for templates:true)
  ├── source/* commands (RegisterSourceCommand, ListSourcesCommand, ShowSourceCommand, RemoveSourceCommand)
  └── SourceCommand.java (group)
  Rationale: Destination add-mapping validates that source name exists; must exist first.
  Tests: register/list/show/remove with local path; register with HTTP URL; param extraction.

Phase 4: Destinations + Mappings
  ├── DestinationService.java (CRUD + mapping CRUD with path guard)
  ├── destination/* commands
  └── DestinationCommand.java (group)
  Rationale: GeneratorService and SyncService both load destinations from config.
  Tests: register/list/show/remove; add-mapping with source validation; path guard rejection.

Phase 5: Generate
  ├── GeneratorService.java (local + remote sources; glob + recurse; Freemarker gated on templates flag)
  └── GenerateCommand.java (--destination option)
  Rationale: Depends on model, ConfigService, GlobMatcher, RemoteFetcher, PathExpander.
  Tests: generate from local non-template source; generate from templates:true source; glob filter;
         recurse flag; skip-existing; dry-run; remote URL source.

Phase 6: Sync
  ├── DefaultSyncService.java (mapping-driven; source→destination; conflict detection)
  ├── SyncService.java (interface; new signature)
  └── SyncCommand.java (--destination option; remove --subscription)
  Rationale: Depends on model, ConfigService, GlobMatcher. Simpler than GeneratorService (no Freemarker).
  Tests: sync with sync:true mapping; conflict detection; dry-run; remote source skipped.
```

**Deletion order within Phase 1:** Remove old packages (archetype/, catalog/, project/, subscription/) first so compilation errors surface package-by-package rather than accumulating. Replace ViracochaCommand subcommand list after stubs for SourceCommand and DestinationCommand exist.

---

## Anti-Patterns

### Anti-Pattern 1: Freemarker Configuration as a Field on GeneratorService

**What people do:** Instantiate a `Configuration` object in `GeneratorService`'s constructor and reuse it across calls (as the v2 code does with `fmContentConfig`).

**Why it's wrong:** The v2 `GeneratorService` uses a single `Configuration` with no template loader set for path expansion, then re-creates templates per file using `new Template(name, reader, config)`. This couples two different use cases (path segment expansion and file content rendering) to the same config object. In v3, the `PathExpander` already handles path segments with its own scoped `Configuration`. `GeneratorService` should construct a new `Configuration` per source directory (for `templates:true` sources) using `setDirectoryForTemplateLoading`.

**Do this instead:** In v3, `GeneratorService` creates a `new Configuration(FM_VERSION)` per source root at the point of use. `PathExpander` continues to use its `StringTemplateLoader` approach. Keep the two rendering paths separate.

### Anti-Pattern 2: Embedding Remote Fetch Logic in GeneratorService

**What people do:** Add `if (source.getPath().startsWith("http"))` inline inside `GeneratorService.generate()` with an `HttpURLConnection` or `HttpClient` call embedded in the loop.

**Why it's wrong:** Makes `GeneratorService` untestable without a live network. Duplicates the fetch logic when `DefaultSyncService` also needs the same remote-eligibility check.

**Do this instead:** Inject `RemoteFetcher` into both `GeneratorService` and `DefaultSyncService`. Both call `RemoteFetcher.isRemote(path)` to branch. Tests mock or stub `RemoteFetcher`.

### Anti-Pattern 3: Mapping-Level Parameter Overrides

**What people do:** Carry forward the v2 `MappingEntry.parameters` map into v3 to allow per-mapping parameter overrides.

**Why it's wrong:** The v3 ARCHITECTURE decision eliminates per-mapping parameters. Parameters live on `DestinationEntry` and apply to all mappings under that destination. Adding mapping-level overrides re-introduces the complexity that the v3 model was designed to remove.

**Do this instead:** If a destination needs different parameters for different mappings, register two destinations. The flat model is intentional.

### Anti-Pattern 4: Bidirectional Sync Retained for Remote Sources

**What people do:** Attempt to implement `sync: true` on a mapping pointing to a remote URL source.

**Why it's wrong:** Remote sources are read-only in v3. Writing back to a GitHub raw URL or any HTTP endpoint is out of scope and architecturally nonsensical for this tool.

**Do this instead:** `DefaultSyncService` checks `RemoteFetcher.isRemote(source.getPath())` and skips that mapping with a clear warning: `"Mapping 'name' has sync:true but source 'src' is remote — skipped."` The exit code remains 0 for this case (it is a configuration oversight, not a failure).

---

## Integration Points

### Config Service as the Single Config Authority

All command and service classes receive `ConfigService` via constructor injection. No class reads or writes the YAML file directly. This is the existing pattern and must be preserved.

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Command → Service | Direct method call (Micronaut DI-wired) | Services are `@Singleton`; commands are `@Singleton` |
| Service → ConfigService | `configService.load()` / `configService.save()` | Load fresh per command invocation; never cache the config object across calls |
| GeneratorService → RemoteFetcher | Injected `@Singleton`; `fetcher.fetch(url)` returns `byte[]` | RemoteFetcher handles redirects; throws `RemoteFetchException` on non-2xx |
| GeneratorService → GlobMatcher | Injected `@Singleton`; `matcher.matches(glob, filename)` | GlobMatcher wraps Java `PathMatcher`; `*` glob is the default pass-through |
| GeneratorService → PathExpander | Injected `@Singleton`; called only when `source.templates == true` | PathExpander is unchanged from v2 |

---

## Sources

- Direct code analysis of v2 codebase: `GeneratorService.java`, `DefaultSyncService.java`, `ViracochaConfig.java`, `MappingEntry.java`, `ProjectEntry.java`, `ArchetypeEntry.java`, `CatalogEntry.java`, `ConfigService.java`, `PathExpander.java`, `FreemarkerVariableExtractor.java`, `SyncService.java`, `SyncCommand.java`, `GenerateCommand.java`, `ViracochaCommand.java`, `XdgPaths.java`, `CatalogCommand.java`
- `.planning/PROJECT.md` v3.0 section: unified sources/destinations design decisions
- Micronaut picocli integration pattern: unchanged from v2 (`PicocliRunner`, `@Singleton` on commands)

---
*Architecture research for: Viracocha v3.0 unified sources/destinations model*
*Researched: 2026-05-08*
