# Feature Research

**Domain:** Java CLI workspace manager — unified sources/destinations/mappings model with remote HTTP support (v3.0)
**Researched:** 2026-05-08
**Confidence:** MEDIUM — domain knowledge drawn from v2 codebase analysis + established patterns in dotfile managers (chezmoi, GNU Stow, Mackup), workspace scaffolders (Cookiecutter, Yeoman, Maven Archetypes), and config sync tools (rsync, unison). No live web research available; confidence ratings reflect this.

---

## Scope Note

This research covers ONLY the new v3.0 features. The following are already built and shipped in v1.0/v2.0 and are NOT re-researched here:

- `vira catalog` / `vira archetype` CRUD commands
- `vira project` CRUD + `add-mapping`
- `vira subscription` CRUD
- `vira generate` (skip-existing, dry-run, verbose)
- `vira sync` (one-way + bidirectional; conflict detection via `Files.mismatch`)

v3 replaces the above with: unified `sources[]`, unified `destinations[]` (with nested `parameters[]` + `mappings[]`), and per-mapping `glob`/`recurse`/`sync` flags. The sync direction changes to source→destination only.

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| `vira source add` with `--name`, `--path`, `--templates` flag | CRUD commands are the minimum viable unit; any config manager needs registration | LOW | Replaces both `vira catalog register` and `vira archetype register` with one command. Validation: path exists on disk OR valid http(s) URL. |
| `vira source list` (plain + `--json`) | Users must see what is registered before they can use or debug it | LOW | Two-column table: name, path. `--json` for scripting. Pattern established in v2. |
| `vira source show <name>` | Single-entry detail view; needed before adding mappings | LOW | Shows all fields including `templates` flag and extracted parameter names if any. |
| `vira source remove <name>` | Registrations accumulate; remove is essential hygiene | LOW | Guard: reject if any destination mapping references this source. Error message must name the referencing mappings. |
| `vira destination add` with `--name`, `--path` | Destinations are the second required entity | LOW | Validation: path is absolute. Directory need not exist yet (created on first generate). |
| `vira destination list` (plain + `--json`) | Same reasoning as source list | LOW | Two-column table: name, path. |
| `vira destination show <name>` | Must display nested parameters and mappings — the full destination record | MEDIUM | Nested YAML-style display for the mappings list. Shows all mapping fields including glob/recurse/sync values. |
| `vira destination remove <name>` | Config hygiene | LOW | Guard: warn that nested mappings will be removed. Require `--force` to cascade-remove OR refuse until user removes mappings first. Recommend refuse-and-list: gives explicit control. |
| `vira destination add-mapping` with `--destination`, `--name`, `--source`, `--dest` | Mappings are the join point; adding one is the most frequent post-setup operation | MEDIUM | `--glob` defaults to `*`; `--recurse` defaults to false; `--sync` defaults to false. Validates: destination exists, source exists, no `..` in dest, no duplicate name within destination. |
| `vira destination remove-mapping --destination <name> --mapping <name>` | Mappings become stale; remove is essential | LOW | Clean removal from nested list; no cascade effects. |
| `vira generate --destination <name>` (skip-existing semantics) | Core use case: populate workspace from registered sources | HIGH | Iterates mappings in declaration order. Applies glob+recurse per mapping. Freemarker expansion only when `source.templates:true`. Remote sources: HTTP GET per file. Skip-existing on all output paths. |
| `vira sync --destination <name>` (source→destination only) | Keep in-sync mappings updated without regenerating from scratch | HIGH | Only processes mappings where `sync:true`. Source→destination direction only. Conflict = content mismatch on existing file → abort that mapping, report, exit 1. |
| `--dry-run` on both `generate` and `sync` | Safe preview before committing writes; power users always want this | LOW | Established in v2; preserve exact behavior. Print same summary without writing. |
| `--verbose` on both `generate` and `sync` | Debugging file-by-file decisions; essential for first-time setup | LOW | Print `Created`/`Skipped`/`Failed`/`COPY`/`SKIP`/`CONFLICT` per file. Pattern established in v2. |
| `--json` output on all list/show commands | Scripting and pipeline integration | LOW | Established in v2. Return valid JSON arrays/objects. |
| Config v2→v3 migration | Without migration, existing users are broken on upgrade | MEDIUM | Detect `version: 2` in config; auto-convert catalogs→sources (`templates:false`), archetypes→sources (`templates:true`), projects→destinations, subscriptions→mappings (`sync:true`). Print migration notice. Write backup before overwriting. Write `version: 3`. |
| `vira config show` updated for v3 schema | Users inspect their full config; must reflect the new model | LOW | Already exists; update to render `sources[]` + `destinations[]` instead of v2 structure. |
| Meaningful error messages with actionable hints | CLI tools must tell users what to do next, not just what went wrong | LOW | Pattern from ARCHITECTURE.md: "Run `vira source list`" style hints. Already in v2. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Remote HTTP(S) source support in `vira source add` and `vira generate`/`vira sync` | Single command fetches a file from a GitHub raw URL, release artifact, or any static URL and copies it to workspace — without managing a local clone | HIGH | `RemoteFetcher` reads bytes via `java.net.http.HttpClient`. Glob applied to final URL path segment. No caching: every generate/sync re-fetches. Read-only: no writes back to remote. HTTP 4xx/5xx → `RemoteFetchException` → named-file skip + summary line. |
| Per-mapping `glob` filter (Ant/NIO-style) | Fine-grained file selection from a single source without multiplying source registrations. `3[0-9]+*.mdc` selects Java-specific rules; `[A-Za-z]+*.mdc` selects general rules — both from the same source directory | MEDIUM | Use `java.nio.file.FileSystem.getPathMatcher("glob:...")`. Apply to filename only when `recurse:false`; apply to relative path from source root when `recurse:true`. Default `*` matches all files at top level. |
| Per-mapping `recurse` flag | Some mappings need all files in a subtree; others need a flat copy from one directory. One flag controls this without requiring separate source registrations | LOW | `recurse:false` (default): only direct children of `source.path` that match glob are copied. `recurse:true`: full tree walk with glob applied to relative path. `Files.walk` already in v2; glob injection is new. |
| Per-mapping `sync:true` flag replacing subscription entities | The intent — "keep this up to date" — lives at the mapping level, not in a separate subscription object. Users never need to find a subscription ID; they declare `sync:true` on the mapping | LOW | Schema-level simplification. Complexity is equivalent to v2 subscriptions; the user-facing concept is simpler. During `vira sync`, iterate all mappings where `sync:true`. Skip mappings without the flag. |
| Freemarker template expansion in file names AND content | Source files can contain `${variable}` in path segments and file content. One archetype scaffolds any project name without manual find-and-replace | MEDIUM | Already built in `PathExpander` + `GeneratorService`. v3 keeps this; fires only when `source.templates:true`. Parameters come from `destination.parameters` map. |
| Destination-scoped `parameters` map | Template variables scoped to a destination, inherited by all its mappings. No repetition of parameter values per mapping | LOW | Already in v2 `ProjectEntry`. v3 moves to `DestinationEntry`. Freemarker merge: destination parameters. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Bidirectional sync in v3 | Users who edited workspace files want to push changes back to source | Remote sources make bidirectional semantically undefined (write back to a GitHub URL?). For local sources it adds conflict resolution complexity that doubled v2 code size. The v3 data model (source is canonical, destination is a copy) makes bidirectional semantically wrong | Manage the source directory directly; re-sync to destinations on change |
| Watch mode / background daemon | Users want automatic sync when source files change | Daemon lifecycle, signal handling, PID files, and cross-platform filesystem event APIs are a large surface. Adds failure modes (stale daemon, missed events) that one-shot `vira sync` avoids | Document a cron job or systemd timer: `vira sync --destination <name>` on schedule |
| Glob matching on destination paths (filter which destination files get overwritten) | Users want to protect some destination files from sync | Sync conflict detection already protects via `Files.mismatch` abort. A second glob filter on the destination side creates confusion about which filter applies when | Use tighter source-side globs; or remove and re-add the mapping with a narrower glob |
| Authentication for remote sources | Users want to fetch from private GitHub repos | HTTP auth adds credential storage, token refresh, and security surface. v3 scope: read-only public URLs only | Use GitHub raw URLs with a short-lived PAT embedded in the URL (not stored by vira), or use a local git clone as source |
| Interactive merge on conflict | Users want to resolve conflicts in-place at sync time | Interactive prompts break scripting and CI. The tool's value is being scriptable and safe-by-default | Abort on conflict + print the conflicting file paths. User resolves manually, then re-runs sync |
| Caching for remote sources | Users want offline generate/sync | Cache invalidation semantics (TTL? manual bust? version pinning?) are non-trivial for a personal tool | Pin URLs to specific commit SHAs (GitHub raw URLs are immutable by SHA). No cache needed |
| Per-mapping parameter overrides in v3 | Users want to override destination parameters for specific mappings | v2 had this and added MappingEntry complexity for a case that rarely arises; destination-level parameters cover 95% of use cases | Register a separate destination with different parameters; mappings are cheap to add |

---

## Feature Dependencies

```
[vira source add]
    └──required-by──> [vira destination add-mapping]
                          (mapping.source must reference a registered source)
    └──required-by──> [vira generate]
                          (source path must be resolvable at generate time)
    └──required-by──> [vira sync]
                          (sync iterates source entries)

[vira destination add]
    └──required-by──> [vira destination add-mapping]
                          (mapping belongs to a destination)
    └──required-by──> [vira generate]
                          (destination path is where files are written)
    └──required-by──> [vira sync]
                          (sync writes to destination)

[vira destination add-mapping]
    └──required-by──> [vira generate]
                          (generate processes mappings in declaration order)
    └──required-by──> [vira sync]
                          (sync filters to sync:true mappings)

[vira generate]
    └──enhances──> [vira sync]
                      (generate first-populates; sync keeps current)
    └──depends-on──> [glob + recurse filtering]
                         (per-mapping file selection)
    └──depends-on──> [remote HTTP fetch]
                         (when source.path is http(s)://)
    └──depends-on──> [Freemarker expansion]
                         (when source.templates:true)

[Config v2→v3 migration]
    └──must-precede──> [all v3 commands]
                          (ConfigService.load() is the first call in every command;
                           stale v2 schema causes parse errors)

[remote HTTP fetch]
    └──orthogonal-to──> [Freemarker expansion]
                            (templates:false expected for remote sources;
                             combination is not blocked but should be warned)
    └──constrained-by──> [sync:true on mappings]
                              (remote sync re-fetches on each vira sync run;
                               no caching means network dependency)

[glob filtering]
    └──composes-with──> [recurse flag]
                            (recurse:false → glob matches filename only;
                             recurse:true → glob matches full relative path)
```

### Dependency Notes

- **vira source add required by add-mapping:** `MappingEntry.source` is a named reference to `SourceEntry.name`. Validate at add-mapping time (not deferred to generate).
- **Config migration must precede all v3 commands:** `ConfigService.load()` is the first call in every command. v2 schema must either auto-migrate or error with migration instructions. Recommend auto-migration with backup file written to same directory.
- **Glob + recurse compose:** When `recurse:false`, apply PathMatcher against `path.getFileName().toString()`. When `recurse:true`, apply PathMatcher against the full relative path from source root. This distinction matters for patterns like `**/*.mdc` (only meaningful with recurse:true).
- **Remote fetch is orthogonal to templates:** Remote sources should always be `templates:false` in practice (Freemarker-expanding files fetched from an upstream repo is unexpected and potentially destructive). The combination is not blocked by schema, but warn during generate if it occurs.
- **Generate enhances sync:** Intended workflow — `vira generate` for initial population (all mappings, skip-existing), then `vira sync` for ongoing updates (only `sync:true` mappings, conflict-abort). The two commands share file-resolution logic but have different collision semantics.

---

## MVP Definition

### Launch With (v3.0)

Minimum viable product to validate the unified model and unlock daily use.

- [ ] Config schema v3: `sources[]`, `destinations[]` with nested `parameters[]` + `mappings[]`
- [ ] Config v2→v3 auto-migration (with backup)
- [ ] `vira source add/list/show/remove`
- [ ] `vira destination add/list/show/remove`
- [ ] `vira destination add-mapping / remove-mapping`
- [ ] `vira generate --destination` (glob + recurse; Freemarker for templates:true; skip-existing)
- [ ] `vira sync --destination` (sync:true mappings only; source→destination; conflict-abort)
- [ ] Remote HTTP(S) source support in generate and sync
- [ ] Remove all v2 commands (catalog, archetype, project, subscription)

### Add After Validation (v3.x)

Features to add once the v3 model is proven in daily use.

- [ ] `vira generate --all` — generate all destinations in one pass; useful for bootstrapping a new machine
- [ ] `vira sync --all` — sync all destinations in one pass
- [ ] `vira source validate <name>` — check that source path exists and is reachable (local or remote)
- [ ] `vira destination validate <name>` — dry-run all mappings and report which files would be created/skipped

### Future Consideration (v4+)

Features to defer until the v3 model demonstrates value.

- [ ] Watch mode — defer until one-shot sync proves insufficient for daily use
- [ ] Authentication for remote sources — defer until private repo access is a real pain point
- [ ] Multiple config profiles — defer until multi-machine workflow creates demand
- [ ] Per-mapping parameter overrides — defer; destination-level parameters cover known use cases

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Config v2→v3 migration | HIGH | MEDIUM | P1 |
| `vira source` CRUD | HIGH | LOW | P1 |
| `vira destination` CRUD | HIGH | LOW | P1 |
| `vira destination add-mapping / remove-mapping` | HIGH | LOW | P1 |
| `vira generate` updated (glob + recurse + remote) | HIGH | HIGH | P1 |
| `vira sync` updated (sync:true only, source→dest) | HIGH | MEDIUM | P1 |
| Remote HTTP(S) source in generate + sync | HIGH | MEDIUM | P1 |
| Remove all v2 commands | MEDIUM | LOW | P1 |
| `vira generate --all` / `vira sync --all` | MEDIUM | LOW | P2 |
| `vira source validate` / `vira destination validate` | MEDIUM | LOW | P2 |
| Watch mode | LOW | HIGH | P3 |
| Remote source authentication | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for v3.0 launch
- P2: Should have; add when core is stable
- P3: Nice to have; future milestone

---

## CLI UX Patterns for Source/Destination/Mapping Commands

Confidence: MEDIUM — based on domain knowledge from picocli ecosystem, chezmoi, GNU Stow, rsync, and the existing v2 command structure.

### Command Naming Conventions

| v2 Command | v3 Replacement | Rationale |
|------------|----------------|-----------|
| `vira catalog register` | `vira source add` | `add` is shorter and more intuitive for daily use than `register` |
| `vira catalog unregister` | `vira source remove` | `remove` matches `add`; symmetric naming |
| `vira archetype register` | `vira source add --templates` | Single command; flag distinguishes template sources |
| `vira project create` | `vira destination add` | `add` not `create` — no side effects until generate |
| `vira project add-mapping` | `vira destination add-mapping` | Kept as subcommand of `destination` (not top-level `vira mapping add`) |
| `vira subscription add` | Removed — replaced by `--sync` on `add-mapping` | Per-mapping `sync:true` eliminates the separate subscription entity |

Note on mapping command placement: PROJECT.md describes `vira mapping add/list/remove` as a top-level command group. The ARCHITECTURE.md design places mapping commands under `vira destination add-mapping`. The codebase precedent (v2 `vira project add-mapping`) also supports nesting under the parent entity. Recommend keeping mappings nested under `vira destination` because all mapping operations require a destination argument anyway — a top-level `vira mapping add` without `--destination` is incomplete.

### Expected CLI Behaviors

**Add commands:** Accept required options, validate immediately, fail fast with named error. Print confirmation: `Source 'cursor-rules' added.`

**List commands:** Default to plain tabular output. `--json` returns a JSON array. Consider `--quiet` returning names only (useful for shell completion scripts).

**Show commands:** Print all fields including nested collections. For `vira destination show`, display the full mapping list. Use YAML-style indented block (not flat tables) for nested collections — easier to read than a table for 6+ mapping fields.

**Remove commands:** If referential integrity would be violated (removing a source that has mappings), print a clear error naming the dependent mappings. Refuse and require the user to remove mappings first. This gives explicit control and avoids silent cascade deletes.

**Source type indicator in output:** Display `[local]` vs `[remote]` prefix in list output. Users need to know which sources will trigger HTTP fetches at generate/sync time.

---

## Glob and Recurse Behavior — Expected Edge Cases

| Case | Expected Behavior | Notes |
|------|-------------------|-------|
| `glob: "*"` + `recurse: false` | Copy all regular files in the source root directory, no subdirectories | Default case; matches `ls source/` behavior |
| `glob: "*.mdc"` + `recurse: false` | Copy only `.mdc` files in source root; skip all other files and all subdirectories | Filename-only match |
| `glob: "**/*.mdc"` + `recurse: true` | Copy `.mdc` files at any depth in the source tree | `**` is a multi-segment glob wildcard; apply `PathMatcher("glob:**/*.mdc")` to full relative path |
| `glob: "[0-9]*.md"` + `recurse: false` | Copy files whose names start with a digit | Bracket character class in Java NIO glob |
| `glob: "*"` + `recurse: true` | Copy entire source tree — all files at all depths | Full subtree copy |
| Source directory is empty | Skip silently; report `0 files` in summary | Not an error condition |
| Source path doesn't exist (local) | Error: "Source path '/path' does not exist." Exit 1 | Validate at generate/sync time, not only at registration time |
| Remote source URL returns 404 | `RemoteFetchException` → skip that file, log error, increment failed count | HTTP errors do not abort the entire generate run |
| Remote source URL returns redirect | Follow redirect (HttpClient default); use final URL content | Do not expose redirect chain to user |
| Hidden files (dot-prefixed names) | **Include** by default; user's glob controls selection | CRITICAL CHANGE from v2: v2 excluded hidden paths via `ArchetypePathUtils.hasHiddenPathSegment`. v3 must NOT auto-exclude hidden files — many managed config files are hidden (`.claude`, `.cursor`, `.cursor/rules`). Java NIO glob `*` does NOT match dot-files by default on some platforms; verify behavior and use `{*,.*}` or a custom filter if needed. This is the most likely source of a v3 regression. |
| Glob with embedded path separator: `glob: "subdir/*.yml"` + `recurse: true` | Copy only `.yml` files directly inside `subdir/` | Works with Java NIO PathMatcher applied to full relative path. Document this capability — it is not obvious from the schema. |
| Destination subdirectory `dest: .cursor/rules` doesn't exist | Create intermediate directories on first write (`Files.createDirectories`) | Same as v2 behavior; preserve |
| File at destination path exists as a directory | Fail that file: "Cannot write file — path exists as directory." Increment failed count, continue other files | Same as v2 behavior; preserve |
| Path traversal in `dest` field: `dest: ../../etc` | Reject at add-mapping validation: "Mapping dest must not escape destination root." | Security guard; already in v2 |
| Same source + same dest used in two mappings | Allowed; second mapping's files overlay the first (declaration order matters) | Document the ordering guarantee |

---

## Competitor Feature Analysis

Reference tools analyzed from domain knowledge (MEDIUM confidence — no live verification):

| Feature | chezmoi | GNU Stow | rsync | Vira v3 |
|---------|---------|----------|-------|---------|
| Source model | Single source tree per machine (git repo) | Package directories under stow root | Source directory or remote host | Named source registry — multiple sources, each optionally Freemarker-templated |
| Destination model | `~` fixed | Target directory per stow operation | Destination path per invocation | Named destination registry — multiple destinations, each with ordered mappings |
| Filtering | Templates + `.chezmoiignore` | `.stow-global-ignore` | `--include`/`--exclude` with glob | Per-mapping `glob` + `recurse` flags |
| Template engine | Go templates | None | None | Apache Freemarker (per source: `templates:true`) |
| Remote sources | None (local git) | None | SSH/rsync daemon | HTTP(S) GET — read-only, no auth |
| Conflict handling | Abort with message | Symlink conflict = error | `--update` / `--checksum` semantics | Abort per-mapping with summary; exit 1 |
| Config format | YAML + TOML | Directory structure | CLI flags only | YAML (XDG path) |
| Sync direction | Source→home (one-way) | Source→target (symlinks, not copies) | Configurable | Source→destination (one-way) |

Vira v3 occupies a niche not covered by any single tool: named source + destination registration with per-mapping glob/recurse/sync control, Freemarker template expansion, and remote HTTP source support — all driven by a single YAML config file and a scriptable CLI. The closest analog is chezmoi for the "manage config files across workspaces" use case, but chezmoi targets a single home directory, uses Go templates, and has no concept of multiple named destinations or a source registry.

---

## Sources

- Viracocha v2 codebase (`/home/jmochel/pers/viracocha/src/`) — HIGH confidence; analyzed directly
- Viracocha PROJECT.md — HIGH confidence; project constraints and decisions
- Viracocha ARCHITECTURE.md (v3 schema) — HIGH confidence; v3 schema and data flow
- Java NIO `PathMatcher` glob semantics — MEDIUM confidence; training knowledge; verify hidden-file behavior in integration tests
- chezmoi / GNU Stow / rsync patterns — MEDIUM confidence; domain knowledge used for competitive positioning only
- picocli command naming conventions — MEDIUM confidence; training knowledge from picocli tool ecosystem

---

*Feature research for: Viracocha v3.0 unified sources/destinations/mappings model*
*Researched: 2026-05-08*
