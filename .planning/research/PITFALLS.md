# Pitfalls Research

**Domain:** Breaking v3 redesign of a Java CLI tool — YAML schema replacement, HTTP source fetching, glob matching, sync model shift (Micronaut 4 + picocli + jackson-dataformat-yaml + SnakeYAML)
**Researched:** 2026-05-08
**Confidence:** HIGH (derived from direct code analysis of v2 codebase + well-understood library behavior)

---

## Critical Pitfalls

### Pitfall 1: Jackson-dataformat-yaml deserializes unknown fields silently, masking corrupt v2 config reads

**What goes wrong:**
The v3 `ViracochaConfig` replaces `catalogs[]`, `archetypes[]`, `projects[]` (with nested `subscriptions[]`) with `sources[]` and `destinations[]`. Jackson-dataformat-yaml's default `ObjectMapper` ignores unknown fields during deserialization (`DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` defaults to `false`). If a user's existing v2 config.yaml is loaded against the v3 model, Jackson silently drops all v2 fields and returns an empty config (version field and empty lists). No error is thrown. The user runs `vira generate` and gets "0 files created" with no explanation.

**Why it happens:**
This is exactly the same ObjectMapper construction used in `ConfigService` today — `new ObjectMapper(new YAMLFactory())` with no further configuration. The clean-break approach means there is no migration path, so a user who upgraded without manually converting their config will silently lose all their registrations.

**How to avoid:**
- In `ConfigService.load()`, check the `version` field immediately after deserializing. If the loaded `version` is not 3 (e.g. it is 1 or 2, or is 0 because the field was missing), abort with a clear error: "Config is version N — v3 requires schema version 3. Migrate manually or run 'vira config init' to start fresh."
- Do NOT attempt silent migration in `load()`. Fail loudly so the user knows what happened.
- Add a round-trip YAML test asserting that a v2-format YAML string causes the version check to fail rather than producing a silent empty config.

**Warning signs:**
- `generate` reports "0 files created, 0 skipped, 0 failed" on a freshly rebuilt JAR against an existing config.
- `vira source list` shows nothing despite a populated config file.

**Phase to address:**
Phase 1 (Model + Config) — the version guard belongs in `ConfigService.load()` before any command can reach the data.

---

### Pitfall 2: `ViracochaCommand` subcommands list — Micronaut processes ALL `@Singleton` beans at startup, so deleted command packages still cause compile errors if partially removed

**What goes wrong:**
`ViracochaCommand` statically lists subcommands: `ArchetypeCommand.class`, `CatalogCommand.class`, `ProjectCommand.class`, `SubscriptionCommand.class`, `SyncCommand.class`. These are `@Singleton` beans. Micronaut's annotation processor generates DI wiring at compile time. If you delete a command class but leave it in the `subcommands = {}` list (or vice versa), the build breaks. Worse, if you delete the package but leave imports, the Micronaut bean factory still tries to wire it. The v3 clean break deletes ~35 Java source files across 5 packages (`archetype/`, `catalog/`, `project/`, `subscription/`, part of `sync/`). It is easy to miss one cross-reference.

**Why it happens:**
The static `subcommands = {}` array in `@Command` is the single declaration point, but cross-references are scattered across: `ViracochaCommand.java` imports, the `@Command` array itself, test files importing deleted classes, `reflect-config.json` entries, and any `@Inject` constructors in surviving classes that still reference deleted types.

**How to avoid:**
- Delete packages in compilation order: delete leaf command classes first, then their group command, then remove from `ViracochaCommand.subcommands`, then delete the package. Compile and fix after each package deletion.
- Update `reflect-config.json` in the same commit. The current file lists `CatalogEntry`, `ArchetypeEntry`, `ProjectEntry`, `MappingEntry` — all need replacing with `SourceEntry`, `DestinationEntry`, `MappingEntry` (v3 shape).
- Run `mvn compile` (not just `mvn test`) immediately after deleting each package — annotation processing errors surface at compile, not at runtime.

**Warning signs:**
- "ClassNotFoundException" or "BeanDefinitionException" at application startup for a non-existent class.
- `mvn compile` shows "cannot find symbol" for deleted command imports still present in `ViracochaCommand.java`.

**Phase to address:**
Phase 1 (Model + Config) — the deletion of v2 classes is a prerequisite for the new model POJOs to be clean. All v2 sources must be removed before introducing v3 models.

---

### Pitfall 3: SnakeYAML round-trips `Map<String,String>` parameters in block style — hand-edited flow style is silently rewritten on next save

**What goes wrong:**
The v3 `DestinationEntry` holds `parameters` as a `Map<String,String>`. When Jackson-dataformat-yaml serializes this, SnakeYAML writes it in block style:
```yaml
parameters:
  key1: value1
  key2: value2
```
This is readable but makes config files verbose when destinations have many parameters. The real problem is the inverse: if a user hand-edits the YAML and writes parameters as an inline map (`{key1: value1, key2: value2}`) or flow-style, SnakeYAML parses it correctly but Jackson writes it back in block style on the next save, causing unexpected diffs. This erodes trust in `config.yaml` as a predictable file.

**Why it happens:**
The `ObjectMapper(new YAMLFactory())` default does not configure `YAMLGenerator.Feature.USE_NATIVE_TYPE_ID` or `MINIMIZE_QUOTES` or explicit `DumperOptions`. SnakeYAML 2.x changed some default `DumperOptions` compared to 1.x, and the Micronaut parent POM manages which SnakeYAML version is active.

**How to avoid:**
- Accept block style as the canonical format — it is more readable and diffs cleanly.
- Document in `ConfigService` javadoc that hand-edits using flow style will be rewritten to block style on next save.
- Add a YAML round-trip test specifically for `DestinationEntry` with a non-empty `parameters` map to lock in the expected serialized form before shipping.

**Warning signs:**
- Config file changes on every `vira source register` or `vira destination add` even when no semantic content changed.
- User reports that manually edited config is "reformatted" after running any command.

**Phase to address:**
Phase 1 (Model + Config) — decide on serialization style and lock it with a round-trip test.

---

### Pitfall 4: Java `PathMatcher` glob syntax does not treat `+` as a quantifier — patterns from the schema example are silently wrong

**What goes wrong:**
`MappingEntry.glob` holds patterns like `*.mdc`, `3[0-9]+*.mdc`, `main/action.yml`. The v3 design uses these as filename filters against walked source paths. `FileSystem.getPathMatcher("glob:" + pattern)` works, but there are two failure modes:

1. `PathMatcher.matches(path)` matches against the **full absolute path** by default. A glob of `*.mdc` will never match `/home/jmochel/.../rules/foo.mdc` because the full path has separators. You must call `matcher.matches(relativePath.getFileName())` or `matcher.matches(relativePathFromRoot)` consistently.

2. `glob:3[0-9]+*.mdc` — the `+` in glob syntax is NOT a quantifier. Standard Java glob treats `+` as a literal character. The intent may be "three followed by one or more digits" but glob `3[0-9]+*.mdc` means "3, any digit, literal '+', anything, .mdc". The pattern would silently match nothing, with zero files copied and no error thrown.

**Why it happens:**
Developers familiar with regex apply regex quantifier semantics to glob patterns. The v3 schema example in `ARCHITECTURE.md` uses `3[0-9]+*.mdc` which will fail silently.

**How to avoid:**
- `GlobMatcher` utility must always call `matcher.matches(Path.of(filename))` where `filename` is just the final path segment (or the relative path from source root for multi-segment globs like `main/action.yml`).
- Document in `GlobMatcher` javadoc that `+` is a literal in glob syntax, not a quantifier.
- For multi-segment globs (paths like `main/action.yml`), match against the relative path from the source root, not the filename.
- Add unit tests covering: `*.mdc` matching `foo.mdc`, not matching `foo.mdc.bak`; bracket ranges `[0-9]`; and explicitly assert that `3[0-9]+*.mdc` does NOT match `310-foo.mdc` so the team learns the behavior before it bites them.
- Consider validating globs at `mapping add` time using `FileSystem.getPathMatcher` — an invalid glob pattern throws `PatternSyntaxException` at matcher creation, not at match time.

**Warning signs:**
- `vira generate` reports "0 files created, 0 skipped" for a destination with mappings that should match files.
- A glob pattern containing `+` returns zero matches.

**Phase to address:**
Phase 2 (Sources) — `GlobMatcher` is an infrastructure utility that must be built and unit-tested before `GeneratorService` uses it. Glob semantics must be correct before generate/sync phases start.

---

### Pitfall 5: Remote HTTP source fetching has no directory-listing protocol — `RemoteFetcher` cannot enumerate files at a URL prefix

**What goes wrong:**
The v3 design lists a remote source with `path: https://raw.githubusercontent.com/anthropics/claude-code-action` and a mapping with `glob: 'main/action.yml'`. The generator must "walk" the source and filter by glob. For local sources, `Files.walk(root)` provides the file list. For remote sources, there is no equivalent — HTTP GET on a directory prefix does not return a file listing. GitHub raw, S3, nginx all differ. If `RemoteFetcher` tries to GET the base URL and enumerate files from HTML, this is brittle scraping. If it GETs `baseUrl + "/" + glob` literally, actual glob patterns like `*.mdc` cannot be resolved.

**Why it happens:**
The architecture describes `RemoteFetcher.fetch(url)` as returning bytes, which works for a single file. But the generate/sync flow loops over "the file list from the source filtered by glob" — that list must come from somewhere. The design does not specify how `RemoteFetcher` enumerates remote file lists.

**How to avoid:**
For v3, constrain remote source glob usage: a remote source mapping's `glob` must be a **literal relative path** (no wildcards) or explicitly `*` meaning "fetch the single resource at the URL". The `RemoteFetcher` fetches exactly one URL: `sourceEntry.path + "/" + mapping.glob` (or just `sourceEntry.path` if glob is `*`). Document this constraint clearly.
- Validate at `mapping add` time: if source is remote and glob contains `*`, `?`, or `[`, emit an error: "Remote sources only support literal path globs — no wildcards."
- The `recurse: true` flag must be rejected at generate time when the source is remote — there is no directory walk for HTTP.

**Warning signs:**
- A remote source mapping with `glob: '*.yml'` copies nothing and no error is shown.
- `recurse: true` on a remote source silently does nothing.

**Phase to address:**
Phase 2 (Sources) — the constraint on remote source glob semantics must be documented and validated when building `RemoteFetcher` and `SourceService`. Phase 4 (Generate) must enforce `recurse: false` for remote sources.

---

### Pitfall 6: `vira sync` with zero sync-eligible mappings exits 0 silently — user thinks sync ran

**What goes wrong:**
v2 synced ALL subscriptions for a project by default. v3 syncs only mappings with `sync: true` for a destination. If a user migrates their config manually and forgets to set `sync: true` on any mapping, `vira sync --destination foo` exits 0 with "Copied: 0, Skipped: 0" — no indication that nothing is configured for sync. The user assumes sync ran successfully.

The subtler problem: the option rename from `--project-name` to `--destination` also breaks any existing shell scripts. Picocli correctly rejects unknown options, but if the developer leaves old options in during refactoring, behavior becomes undefined.

**Why it happens:**
The semantics shift from "opt-in sync by creating a subscription" to "opt-in sync by setting a per-mapping flag." The command runs successfully on a destination where `sync: true` is set on zero mappings — zero-result success is ambiguous. And renaming a required CLI option is a breaking change that does not come with any automatic warning.

**How to avoid:**
- In `SyncCommand.call()`, after collecting mappings with `sync: true`, if the list is empty, print a warning: "No mappings in destination 'name' have sync: true. Nothing to sync. Run 'vira destination show --name name' to review mapping flags."
- Return exit code 0 (not an error) but make the message visible on stdout.
- Do NOT add `--project-name` as an alias in `SyncCommand`. Picocli's rejection of unknown options is the correct behavior — let it fail fast for old scripts.
- In `vira destination show` output, explicitly list the `sync` flag value for each mapping.

**Warning signs:**
- `vira sync --destination foo` exits 0 with all-zero counts.
- User reports "sync does nothing" after migrating from v2.

**Phase to address:**
Phase 5 (Sync) — the warning for zero sync-eligible mappings. The option renaming happens in Phase 3 (Destinations) when `SyncCommand` gets its `--destination` option.

---

### Pitfall 7: `templates: false` sources must use byte copy — reusing the string-read path corrupts binary files

**What goes wrong:**
The v2 generator reads all source files with `Files.readString(sourceFile, StandardCharsets.UTF_8)` and always passes them through Freemarker, because v2 only had template sources. v3 adds `templates: false` for the first time. It is natural to reuse the existing string-read-then-maybe-expand pattern, but `templates: false` sources must copy bytes directly. If the refactored generator still reads non-template content as a UTF-8 `String` before deciding whether to apply Freemarker, it will corrupt any binary file (images, compiled resources) that lives in a `templates: false` source.

**Why it happens:**
v2 only supported archetype sources where Freemarker processing was always applied. The `templates` flag is new in v3. The existing `GeneratorService` has a single code path: `Files.readString` → `renderTemplate` → `Files.writeString`. Splitting on the `templates` flag is a new branch that did not exist before.

**How to avoid:**
In `GeneratorService`, branch on `sourceEntry.isTemplates()`:
- `true` → read as UTF-8 String → Freemarker expand → write as UTF-8 String (existing path)
- `false` → read as `byte[]` (or stream) → write bytes directly (`Files.copy` for local, `OutputStream.write` for remote bytes)

For remote sources: `RemoteFetcher.fetchBytes(url)` returns `byte[]`. For `templates: true` remote sources, decode to UTF-8 then Freemarker expand. For `templates: false` remote sources, write bytes directly.

**Warning signs:**
- Binary files (`.png`, `.ico`, `.class`) copied from a `templates: false` source are corrupted at destination.
- File size changes unexpectedly after copy.
- Image files fail to open after being "copied" by vira.

**Phase to address:**
Phase 4 (Generate) — the branch on `templates` flag belongs in `GeneratorService`, and the byte-copy path must have a dedicated test with a non-text file.

---

### Pitfall 8: Micronaut DI wires `@Singleton` beans eagerly — `RemoteFetcher` and `GlobMatcher` must not do I/O in their constructors

**What goes wrong:**
Every picocli command class annotated `@Singleton` and registered in `subcommands = {}` is instantiated by Micronaut DI during `PicocliRunner.execute()`. Constructor injection happens at application startup. The specific risk in v3: `RemoteFetcher` may be tempted to validate network connectivity at construction time, or `GlobMatcher` might pre-compile patterns stored in config. Neither should happen in a constructor — it would cause every `vira` invocation (including `vira --help`) to fail if the network is unreachable or config is not initialized.

**Why it happens:**
Micronaut's eager singleton instantiation is correct behavior for DI correctness, but developers accustomed to lazy Spring beans expect constructor logic to run only when the bean is first used.

**How to avoid:**
- All `@Singleton` constructors must only store injected dependencies. Zero I/O, zero network, zero file reads in constructors.
- `RemoteFetcher` must be stateless — take URL as a method parameter, not a constructor parameter.
- `GlobMatcher` must compile patterns when `matches()` is called, not at construction.
- `HttpClient` instance in `RemoteFetcher` may be created in the constructor (it does not open connections until a request is made), but no actual HTTP calls in constructors.

**Warning signs:**
- Any command shows a startup error ("IOException during bean creation") before the command logic runs.
- `vira --help` triggers an `IOException` because a bean constructor attempted file I/O.

**Phase to address:**
Phase 2 (Sources) — `RemoteFetcher` and `GlobMatcher` are built here; enforce the no-I/O-in-constructor rule from the start.

---

### Pitfall 9: `ArchetypePathUtils.hasHiddenPathSegment` is in the deleted `archetype/` package — surviving code still imports it

**What goes wrong:**
Both `GeneratorService` and `DefaultSyncService` import `org.saltations.archetype.ArchetypePathUtils`. When the `archetype/` package is deleted in Phase 1, these imports break at compile time. If the developer moves the utility but forgets to update the imports in `GeneratorService`, the build fails silently on the first `mvn test` run.

**Why it happens:**
`ArchetypePathUtils` was created as a shared utility but placed in the `archetype` package by proximity. The v3 clean break requires moving it to `infra/` (per the v3 architecture), but it is easy to forget to update all callers when also doing a large-scale deletion.

**How to avoid:**
- Move `ArchetypePathUtils` (or its equivalent) to `org.saltations.infra` as part of Phase 1, before deleting the `archetype/` package.
- Run `grep -r "archetype" src/main/java` after Phase 1 is complete; the result should be empty.
- The sync service deletion means `DefaultSyncService.java` is gone entirely — but `GeneratorService.java` survives and imports `ArchetypePathUtils`. Fix this import before the package deletion commit.

**Warning signs:**
- `mvn compile` fails with "cannot find symbol: org.saltations.archetype.ArchetypePathUtils" after package deletion.

**Phase to address:**
Phase 1 (Model + Config) — move the utility before deleting the package.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Skip version guard in `ConfigService.load()` | One less check | Silent data loss when v2 user upgrades — empty config, no error | Never |
| Reuse existing `@JsonAlias("patterns")` on `sources` field | Backward compat feel | Two names for the same field confuse users; v3 is a clean break | Never — remove all v2 aliases |
| Single `ObjectMapper` instance shared across all YAML operations | Simple code | Not thread-safe if ever used in async context; minor issue for CLI | Acceptable in CLI (single-threaded by design) |
| `RemoteFetcher` using `URL.openStream()` instead of `HttpClient` | Fewer lines of code | No timeout, no redirect handling, no status code checking; hangs on slow servers | Never — v3 introduces HTTP; use `HttpClient` with explicit timeouts |
| Glob validation deferred to generate time | Simpler `mapping add` command | User discovers bad glob only when they run `vira generate`; confusing and delayed feedback | Acceptable for v3 MVP if error message is clear at generate time |
| Keeping `ArchetypePathUtils.hasHiddenPathSegment` in old package with a redirect import | Less refactoring | Creates dependency from `generate/` on deleted `archetype/` package | Never — move to `infra/` as part of v2 cleanup |
| Creating a new Freemarker `Configuration` per file | Simple per-file code | Slow for sources with many files; `Configuration` construction is heavyweight | Never — one `Configuration` per source tree as v2 already does |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| jackson-dataformat-yaml + Lombok `@Builder` | `@Builder` suppresses the no-arg constructor that Jackson needs for deserialization | Always add `@NoArgsConstructor` and `@AllArgsConstructor` alongside `@Builder` on YAML model classes — v2 already does this correctly; preserve the pattern for `SourceEntry` and `DestinationEntry` |
| jackson-dataformat-yaml + primitive `boolean` fields | A field `private boolean recurse` with default `false` serializes as `recurse: false` in YAML. A missing field deserializes to `false` correctly. Boxed `Boolean` deserializes a missing field to `null`, breaking `isRecurse()` calls | Use primitive `boolean` not `Boolean` for `recurse` and `sync` in `MappingEntry` |
| picocli + Micronaut `@Singleton` + `@Spec` | `CommandSpec` is injected by picocli post-construction using field injection (`@Spec`). Micronaut does not manage this field. Do not try to `@Inject` a `CommandSpec` through Micronaut | Declare `@Spec CommandSpec spec` as a field only; never as a constructor parameter — v2 already uses this pattern correctly |
| `java.net.http.HttpClient` + JDK 21 | Creating a new `HttpClient` per fetch call is acceptable but leaks the internal thread pool subtly | Declare `HttpClient` as a `@Singleton` field in `RemoteFetcher`, constructed once in the constructor, never closed (process exits immediately after CLI command) |
| `FileSystem.getPathMatcher("glob:" + userPattern)` | If `userPattern` is empty, `getPathMatcher` throws `PatternSyntaxException` | Default `glob` to `"*"` in `MappingEntry` constructor/builder; validate non-empty at `mapping add` time before passing to `getPathMatcher` |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Creating a new Freemarker `Configuration` per file expansion | Slow `vira generate` for sources with many files | Create one `Configuration` per source tree, as v2 `GeneratorService` already does with `fmContentConfig` | Noticeable at ~100 files |
| `Files.walk()` without try-with-resources | File descriptor leak on large source trees | Always `try (Stream<Path> walk = Files.walk(root)) { ... }` — v2 code does this correctly; preserve in v3 | Any source directory with more than ~1000 files on Linux |
| Re-reading config file inside the mapping loop | Multiple disk reads per mapping during generate | Call `ConfigService.load()` once at the top of `GeneratorService.generate()` — never inside the mapping loop | Noticeable with more than 10 mappings |
| HTTP GET without timeout | `vira generate` hangs indefinitely on a slow or unresponsive remote source | Set `HttpClient` connect and request timeouts; 10s connect, 30s read is appropriate for a CLI tool | First time any remote source is unreachable |
| `PathExpander` creates a new Freemarker `Configuration` per segment (current v2 code) | Slow path expansion for deeply nested templates | `PathExpander.expandSegment()` currently creates a new `Configuration` per call — acceptable for path segments (few per file) but worth noting | Present now; becomes worse with many template files |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| No path traversal guard on `mapping.dest` for v3 | A crafted config with `dest: ../../.ssh/authorized_keys` writes outside the destination root | Already present in v2 for `workspacePath`; must be carried forward to `MappingEntry.dest` in `DestinationService.addMapping()` — reject any `dest` that resolves outside `destination.path` after `normalize()` |
| Remote source URL with `file://` scheme | `RemoteFetcher` using `HttpClient` will follow `file://` URIs on some implementations, potentially reading arbitrary local files | Validate that remote sources must start with `http://` or `https://`; reject `file://`, `ftp://`, etc. at `source register` time |
| Freemarker template expansion of remote content without restricting class resolution | A malicious remote template could use Freemarker directives to read local files via `freemarker.template.utility.Execute` or `?new` | For remote sources with `templates: true`, set `cfg.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER)` in the `Configuration`. The v2 code does not do this because all sources were trusted local archetypes; v3 remote sources are untrusted. |
| HTTP redirect following to non-HTTPS URLs | A `http://` source URL could redirect to a `file://` URI via a malicious redirect | Use `java.net.http.HttpClient` which by default follows only HTTP/HTTPS redirects and does not follow cross-protocol redirects; `URL.openStream()` does NOT have this protection |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| `vira sync` with zero sync-eligible mappings exits 0 silently | User thinks sync ran; data is stale | Print "No mappings with sync: true found in destination 'name'. Run 'vira destination show --name name' to review." |
| `vira generate` option renamed from `--project-name` to `--destination` | Shell scripts break with no guidance | `--project-name` must not be added as an alias — let picocli reject it cleanly; document the breaking change in the release notes |
| Source removed from config but still referenced by a mapping | Exception thrown mid-run after partial generation | Validate all mapping source references exist before starting any file I/O; report all missing sources upfront before writing a single file |
| `vira destination add-mapping` succeeds when source has `templates: true` but destination has zero parameters | Freemarker expansion fails at generate time for every template variable | At `mapping add` time, if source `templates: true`, warn if destination has no `parameters` defined: "Source 'x' uses templates but destination 'y' has no parameters — generation will fail for any template variable" |
| Long-running `vira generate` on a remote source with no progress output | User thinks the tool is hung | Print "Fetching [url]..." before each remote HTTP GET |

---

## "Looks Done But Isn't" Checklist

- [ ] **Version guard:** `ConfigService.load()` checks `config.getVersion() == 3` before returning — verify with a test loading a v2-format YAML string that should throw or return a clear error
- [ ] **reflect-config.json updated:** Old model class entries (`CatalogEntry`, `ArchetypeEntry`, `ProjectEntry`) removed; new entries (`SourceEntry`, `DestinationEntry`, `MappingEntry`) added — verify by reading the file after Phase 1
- [ ] **Path traversal guard on mapping.dest:** `DestinationService.addMapping()` rejects `dest` paths that normalize outside the destination root — verify with a `../` test case
- [ ] **Glob matched against filename, not full path:** `GlobMatcher` calls `matcher.matches(Path.of(filename))` for filename-only globs — verify with a test asserting `*.mdc` matches `foo.mdc` and does NOT match when the full absolute path is passed
- [ ] **`templates: false` sources use byte copy:** `GeneratorService` for non-template sources calls byte-level copy, not `Files.readString()` — verify with a binary file (e.g. a PNG) test case
- [ ] **Remote source rejects wildcard globs:** `SourceService` or `GeneratorService` validates that remote sources cannot use glob patterns containing `*`, `?`, or `[` — verify with a unit test
- [ ] **`vira sync` zero-mapping warning:** `SyncCommand` prints a visible warning when no mappings have `sync: true` — verify with an integration test
- [ ] **HTTP timeout configured:** `RemoteFetcher` sets connect and read timeouts on `HttpClient` — verify by code review of constructor
- [ ] **`ArchetypePathUtils` moved to `infra/`:** No surviving code imports from `org.saltations.archetype` package after Phase 1 — verify with `grep -r "archetype" src/main/java`
- [ ] **`ViracochaCommand.subcommands` matches actual packages:** `mvn compile` after all deletions with zero "cannot find symbol" errors
- [ ] **Freemarker `SAFER_RESOLVER` applied for remote templates:** When source is remote and `templates: true`, `Configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER)` is called — verify by code review of `GeneratorService`

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Silent v2-to-v3 config data loss (version guard missing) | HIGH | Restore config from backup if user has one; otherwise re-register all sources and destinations manually with `vira source register` and `vira destination add` |
| Compile error from partially deleted v2 packages | LOW | Fix remaining references in `ViracochaCommand.java`, `reflect-config.json`, test imports; run `mvn compile` iteratively |
| Wrong glob semantics (`+` treated as literal) | LOW | Correct the glob string in config YAML directly; no JAR rebuild needed |
| Remote source fetch hangs without timeout | MEDIUM | Kill process; add timeout to `HttpClient` in `RemoteFetcher`; rebuild JAR |
| Freemarker unsafe template execution on remote content | HIGH | Requires JAR rebuild with `SAFER_RESOLVER` applied; no user-side workaround |
| Binary file corruption from string-based copy on `templates: false` source | MEDIUM | Delete corrupt destination files; fix `GeneratorService` to use byte copy for `templates: false`; re-run `vira generate` (skip-existing means non-corrupt files are not overwritten) |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Silent v2 config read (no version guard) | Phase 1: Model + Config | Test: loading v2 YAML string produces clear version error, not empty config |
| Partial v2 class deletion breaks compile | Phase 1: Model + Config | `mvn compile` clean after each package deletion in Phase 1 |
| YAML serialization style rewrite on save | Phase 1: Model + Config | Round-trip test for `DestinationEntry` with `parameters` map |
| Glob `+` treated as literal, not quantifier | Phase 2: Sources | `GlobMatcher` unit tests with bracket and wildcard patterns including `+` |
| Remote source cannot enumerate files | Phase 2: Sources | `RemoteFetcher` contract documented; `mapping add` validation rejects wildcards for remote sources |
| `ArchetypePathUtils` import from deleted package | Phase 1: Model + Config | `grep -r "archetype" src/main/java` returns empty after Phase 1 |
| No byte copy for `templates: false` sources | Phase 4: Generate | Integration test: copy a binary file through a non-template source; verify byte equality |
| Micronaut eager singleton does I/O in constructor | Phase 2: Sources | Code review gate: no I/O in constructors of `RemoteFetcher`, `GlobMatcher` |
| `sync` zero-mapping silent exit | Phase 5: Sync | Integration test with destination having zero mappings with `sync: true` |
| Path traversal on `mapping.dest` | Phase 3: Destinations | Unit test: `../` in dest value rejected at `addMapping()` |
| Freemarker unsafe builtins for remote templates | Phase 4: Generate | Code review: `SAFER_RESOLVER` applied when source is remote and `templates: true` |
| `reflect-config.json` has stale v2 class names | Phase 1: Model + Config | Read file after Phase 1 and confirm only v3 model classes are listed |

---

## Sources

- Direct code analysis of v2 viracocha codebase: `ConfigService.java`, `DefaultSyncService.java`, `GeneratorService.java`, `ViracochaCommand.java`, `reflect-config.json`, `MappingEntry.java`, `ArchetypePathUtils.java`
- Jackson-dataformat-yaml default `FAIL_ON_UNKNOWN_PROPERTIES = false` — standard Jackson behavior, HIGH confidence
- Java `FileSystem.getPathMatcher` glob specification: `+` is not a quantifier in glob syntax (JDK `java.nio.file.FileSystem` documentation, HIGH confidence)
- Micronaut eager singleton bean instantiation at `PicocliRunner.execute()` time — observed from v2 code and Micronaut DI documentation, HIGH confidence
- Freemarker 2.3.34 `TemplateClassResolver.SAFER_RESOLVER` — Freemarker documentation on restricting class resolution for untrusted templates, MEDIUM confidence (verify exact API name against Freemarker 2.3.34 docs before use)
- v3 architecture design in `.planning/research/ARCHITECTURE.md`
- `PathExpander.java` — confirms per-call `Configuration` construction pattern to be aware of at scale

---
*Pitfalls research for: v3 unified sources/destinations model — Viracocha CLI breaking redesign*
*Researched: 2026-05-08*
