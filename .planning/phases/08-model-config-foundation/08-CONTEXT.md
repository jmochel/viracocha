# Phase 8: Model & Config Foundation - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Delete all v2 CLI command packages (catalog, archetype, project, subscription), introduce v3 config POJOs (SourceEntry, DestinationEntry, MappingEntry v3), add a version guard to ConfigService.load(), and update ViracochaCommand subcommand list. GeneratorService and DefaultSyncService are NOT rewritten here — they survive as stubs until phases 11 and 12.

</domain>

<decisions>
## Implementation Decisions

### Utility Relocation
- **D-01:** `ArchetypePathUtils` moves to `infra/` and is renamed to `HiddenPathFilter`. Update all imports in `GeneratorService` and `DefaultSyncService` accordingly.
- **D-02:** `FreemarkerVariableExtractor` moves to `infra/` (keeping its name). Phase 9 (SourceService) uses it from its new location.
- **D-03:** Package deletion order: move utilities to `infra/` first, then delete `archetype/`, `catalog/`, `project/`, `subscription/` one at a time — compile after each deletion to catch any remaining cross-references before proceeding.

### v3 Config POJOs
- **D-04:** `SourceEntry` fields: `name` (String), `path` (String), `templates` (boolean, default `false`), `parameters` (List\<String\> — variable names only, consistent with current ArchetypeEntry pattern).
- **D-05:** `DestinationEntry` fields: `name` (String), `path` (String), `parameters` (Map\<String, String\> — default param values), `mappings` (List\<MappingEntry\>).
- **D-06:** `MappingEntry` v3 fields: `sourceRef` (String), `glob` (String, default `null` = no filter = select all files), `recurse` (boolean, default `false` — flat copy by default, opt-in to subtree walk), `sync` (boolean, default `false` — opt-in to keep-in-sync), `params` (Map\<String, String\> — per-mapping param overrides).
- **D-07:** `ViracochaConfig` v3 fields: `version` (int, value `3`), `sources` (List\<SourceEntry\>), `destinations` (List\<DestinationEntry\>).
- **D-08:** All new POJOs use Lombok `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`. No `@Builder` alone on Micronaut-injected beans.

### Version Guard
- **D-09:** `ConfigService.load()` reads the raw `version` field first (using a minimal POJO or `JsonNode`). If missing or < 3, throw a new `ConfigVersionException` (extends `IOException` or is a checked exception) with a message: `"Config file is v{N} — v3 format required. Delete ~/.config/viracocha/config.yaml and run 'vira config init' to start fresh."` Exit code 1.
- **D-10:** `ConfigService.init()` writes a fresh `ViracochaConfig` with `version: 3`. Idempotent — if config already exists and is v3, prints "Config already initialized" and exits 0 (existing Phase 1 behavior).

### reflect-config.json
- **D-11:** Update `reflect-config.json` to replace all v2 model class entries with v3 entries (`SourceEntry`, `DestinationEntry`, `MappingEntry`, `ViracochaConfig`). GraalVM native image is out of scope but maintaining the file avoids silently stale config.

### Removed Subcommands
- **D-12:** `ViracochaCommand` subcommands list updated to remove `ArchetypeCommand`, `CatalogCommand`, `ProjectCommand`, `SubscriptionCommand`. Retains `ConfigCommand`, `GenerateCommand`, `SyncCommand`. Source/destination/mapping commands are added in phases 9 and 10.

### Claude's Discretion
- Implementation of the version pre-read in `load()` (inline `JsonNode` peek vs. minimal POJO vs. map deserialization)
- Whether to introduce a `ConfigVersionException` as a new class or reuse/extend `ConfigNotInitializedException`
- Exact package structure within `infra/` for the relocated utilities

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements for this phase
- `.planning/REQUIREMENTS.md` — CFG-01, CFG-02, CFG-03 (v3 schema POJOs, version guard, v2 command removal)

### Key source files to understand before modifying
- `src/main/java/org/saltations/model/ViracochaConfig.java` — current v2 config root
- `src/main/java/org/saltations/config/ConfigService.java` — load/save/init; version guard goes here
- `src/main/java/org/saltations/ViracochaCommand.java` — subcommand list to update
- `src/main/java/org/saltations/archetype/ArchetypePathUtils.java` — must move to infra/ before archetype/ is deleted
- `src/main/java/org/saltations/archetype/FreemarkerVariableExtractor.java` — must move to infra/ before archetype/ is deleted
- `src/main/resources/META-INF/native-image/org.saltations/viracocha/reflect-config.json` — update with v3 model classes

### Prior phase decisions that carry forward
- `.planning/phases/01-foundation/01-CONTEXT.md` — exit codes (Callable\<Integer\>), Lombok patterns, test requirements

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConfigService` (config/): `init()`, `load()`, `save()` structure is preserved; only `load()` needs a version guard added
- `XdgPaths` (infra/): unchanged; `infra/` is the right home for `HiddenPathFilter` and `FreemarkerVariableExtractor`
- `ConfigNotInitializedException`: pattern for throwing from `load()` — `ConfigVersionException` follows the same convention

### Established Patterns
- Lombok `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor` on all model POJOs — must be preserved on v3 POJOs
- `@JsonAlias` used on ArchetypeEntry to accept legacy YAML field names — may not be needed on v3 POJOs (clean break)
- Static subcommand declaration in `@Command(subcommands = {...})` — removing old entries follows existing pattern

### Integration Points
- `GeneratorService` and `DefaultSyncService` import `ArchetypePathUtils` — update import to `infra.HiddenPathFilter` in Phase 8; service bodies unchanged until phases 11/12
- `reflect-config.json` is the only GraalVM artifact that references model classes by name — must stay in sync

</code_context>

<specifics>
## Specific Ideas

No specific implementation references beyond the decisions above — open to standard approaches for version-guard implementation detail.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 08-model-config-foundation*
*Context gathered: 2026-05-08*
