# Viracocha

## What This Is

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable named **sources** (local folder paths, optionally Freemarker-templated) and **destinations** (workspace root paths), then populating destination paths from sources via **mappings** that specify glob filtering, recursion, and sync behavior. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

## Core Value

A developer registers sources and destinations once, then populates any workspace with a single command — and regeneration is safe (skips existing files). Mappings with `sync: true` keep destination copies up to date with their source on demand via `vira sync`.

## Requirements

### Validated (v1.0 — shipped 2026-04-04)

- ✓ CLI scaffold with Micronaut DI + picocli; Maven + JUnit 5 — v1.0
- ✓ Config init/show; XDG YAML central config; JSONL logging — v1.0
- ✓ Catalogs and archetypes (Freemarker params extracted at registration) — v1.0
- ✓ Projects and mappings with per-mapping param overrides — v1.0
- ✓ `vira generate` with skip-existing, dry-run, verbose — v1.0
- ✓ Full detail: `.planning/milestones/v1.0-REQUIREMENTS.md`

### Validated (v2.0 — shipped 2026-04-04)

- ✓ CFG-01/CFG-02: Subscription rows in central YAML (`SubscriptionEntry` nested under `ProjectEntry`); config load/save round-trips without data loss — v2.0
- ✓ SUB-01–SUB-07: `vira subscription add/list/show/remove` with project/catalog validation, path guard (no `..` traversal), duplicate rejection, plain + JSON output — v2.0
- ✓ SYN-01–SYN-05: `DefaultSyncService` with one-way (catalog→ws, ws→catalog) and bidirectional sync; `Files.mismatch` conflict detection; hidden-path parity with `generate` — v2.0
- ✓ SYN-06–SYN-09: `vira sync --project-name` with `--subscription`, `--dry-run`, `--verbose`, `--json`; summary line (copied/skipped/failed/conflicts); exit 1 on failure — v2.0
- ✓ X-01/X-02: Integration tests for all three sync directions; README documents subscriptions, sync directions, conflict policy, and example workflow — v2.0
- ✓ Full detail: `.planning/milestones/v2.0-REQUIREMENTS.md`

### Validated (v3.0 — shipped 2026-05-11)

- ✓ CFG-01: v3 config POJOs (SourceEntry, DestinationEntry, MappingEntry, ViracochaConfig v3); YAML round-trip without data loss — Phase 8
- ✓ CFG-02: ConfigService.load() version guard; ConfigVersionException for v2 config files — Phase 8
- ✓ CFG-03: All v2 command packages (archetype, catalog, project, subscription) removed; ViracochaCommand registers only config/generate/sync — Phase 8

- ✓ SRC-01–SRC-07: `SourceService` with full CRUD + path validation (anti-traversal, existence, duplicate); `vira source add/list/show/remove` commands registered under `SourceCommand` group; Freemarker variable extraction via `@Singleton FreemarkerVariableExtractor`; `--json` output on show/list; aligned column list; exact error messages — Phase 9

- ✓ DEST-01–DEST-06, MAP-01–MAP-05: `DestinationService` with full CRUD + mapping ops (no path-existence check on add; paths stored as-is); `vira destination add/list/show/remove/add-mapping/list-mappings/remove-mapping` registered under `DestinationCommand` group (alias `dest`); `GlobMatcher` in `infra/` wrapping JDK `PathMatcher`; `+` treated as literal in glob patterns; aligned column list; `--json` JSONL output; exact error messages; 60 new tests (136 total) — Phase 10

- ✓ GEN-01–GEN-07: `GeneratorService.generate()` full v3 traversal (destinations→mappings→sources; glob+recurse filters; Freemarker expansion in paths and content; binary file copy without corruption; skip-existing); `vira generate --destination-name` routing; `--dry-run`, `--verbose`, interactive destination-creation prompt; `GenerateCommand` wired with testable output writer + stdin; 14 tests across `GeneratorServiceTest` and `GenerateCommandTest` — Phase 11

- ✓ SYN-01–SYN-07: `DefaultSyncService.sync()` full v3 traversal (source→destination only; timestamp conflict detection via `Files.getLastModifiedTime`; `Files.mismatch` for content equality; `REPLACE_EXISTING` copy semantics; glob+hidden-path filters; `isSync()` mapping guard); `SyncResult` record with 6 fields; `SyncCommand` rewritten with `--destination-name` (required), `--dry-run`, `--verbose`, `--json`; exit 1 on conflicts; summary line always printed; 13 new tests (161 total) — Phase 12

- ✓ Full detail: `.planning/milestones/v3.0-REQUIREMENTS.md`

### Active

(None — v3.0 complete. Define requirements for next milestone via `/gsd:new-milestone`.)

### Out of Scope

- Sync *to* remote sources (HTTP/Git) — sources are read-only; write operations stay on local filesystem
- Remote sources (http/https) — local filesystem only in v3; deferred to v4
- Watch mode / background sync daemon — deferred to v4+ (one-shot `vira sync` covers the use case)
- Graal native image — GraalVM profile exists in pom.xml but native build is not a current target
- Multiple config profiles — single XDG config path is sufficient for current use case
- Interactive merge UI — scriptable CLI-first policy; conflict abort is the correct default
- Bidirectional sync — v3 sync is source→destination only; the old ws→catalog direction is removed with subscriptions
- Config auto-migration from v2 — clean break; fail-with-instructions is sufficient

## Context

**Shipped v1.0 (2026-04-04):** Full CLI per roadmap — config init/show, catalogs, archetypes with Freemarker params, projects and mappings, and `vira generate` with skip-existing, dry-run, and verbose output. 4 phases, 11 plans.

**Shipped v2.0 (2026-04-04):** Subscriptions, sync engine, `vira sync` CLI, and docs. 3 phases, 9 plans. Requirements archive: `.planning/milestones/v2.0-REQUIREMENTS.md`.

**Shipped v3.0 (2026-05-11):** Complete v3 model rewrite — replaced catalog/archetype/project/subscription with sources/destinations/mappings. 5 phases, 16 plans, 32 tasks. 2,469 LOC Java (main) + 3,075 LOC Java (test) = 5,544 total. 161 passing tests, 0 failures. Requirements archive: `.planning/milestones/v3.0-REQUIREMENTS.md`.

The project is named "viracocha" (package: `org.saltations`). The CLI binary is `vira`. Micronaut + picocli is intentional and must be preserved.

Sources with `templates: true` use Apache Freemarker for template expansion. Variables appear in file content AND in folder/file names. Parameter names are extracted from the source at registration time or declared explicitly.

Central config is a single YAML file: `~/.config/viracocha/config.yaml` (XDG). v3 schema: `version: 3`, `sources[]`, `destinations[]` (with nested `parameters{}` and `mappings[]`).

## Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — no deviations
- **Config format**: YAML only for central config
- **Regeneration**: `vira generate` keeps skip-existing semantics; sync is a separate code path
- **Scope**: Local filesystem only — remote HTTP(S) sources deferred to v4

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep Micronaut DI | Already in skeleton; provides clean DI without manual wiring | ✓ Good — DI worked cleanly for all services |
| Skip-existing on generate | Protects hand-edited workspace files from being overwritten | ✓ Good — no reported conflicts |
| Subscriptions deferred to v2 | Adds significant complexity (sync semantics, conflict resolution) | ✓ Good — correct sequencing; v1 was cleaner without it |
| XDG config paths | Standard for Linux CLI tools; consistent with tool ecosystem | ✓ Good — unchanged |
| Subscriptions nested under ProjectEntry | Project is the anchor for workspace path; keeps YAML cohesive | ✓ Good — cleaner than a flat top-level subscriptions list |
| Global UUID for subscription ids | Enables `show`/`remove` by id without project context | ✓ Good — simplifies CLI targeting |
| Conflict default = abort | Safe-by-default; avoids silent data loss | ✓ Good — force flags available for future overrides |
| Bidirectional as union-then-two-pass | Deterministic ordering; avoids double-counting | ✓ Good — TreeSet union works cleanly |
| `Files.mismatch` for conflict detection | JDK 12+ NIO; avoids content hashing overhead | ✓ Good — accurate and fast for small files |
| Collapse catalogs+archetypes into sources (v3) | Two concepts with one distinguishing flag were unnecessary complexity; `templates: true/false` captures the difference cleanly | ✓ Good — 35 requirements shipped; templates flag cleanly distinguishes source types |
| Collapse projects into destinations (v3) | Projects were destinations with extra ceremony; flat `destinations[]` list is simpler and more general | ✓ Good — simpler and more general; covers any directory, not just projects |
| Eliminate subscriptions in favor of per-mapping sync flag (v3) | Subscriptions as a separate top-level concept added indirection; `sync: true` on a mapping is the natural place for this intent | ✓ Good — `sync: true` is more discoverable; subscription overhead is gone |
| Add glob + recurse per mapping (v3) | Enables fine-grained selection from a single source into multiple destinations without multiplying source registrations | ✓ Good — enabled fine-grained control without multiplying sources |
| Remote sources read-only (v3) | Writing to remote repos is out of scope; sources are always the authoritative origin | ✓ Good — local-only shipped cleanly; remote deferred to v4 |
| Wave 0 test scaffold (Nyquist pattern) | Pre-create `@Disabled` test stubs in a Wave 0 plan before writing implementation — gives compile-time gate and test-first structure | ✓ Good — used in Phases 11 and 12; improved plan verification quality |
| ConfigVersionException extends IOException | Surfaces as IOException in existing command handlers without adding new catch blocks | ✓ Good — zero boilerplate in command layer |
| Raw-string traversal check before Path.of() | Prevent normalization bypass (`../` after resolve) when validating source/destination paths | ✓ Good — applied consistently in SourceService and DestinationService |
| Content-identity check priority over mtime (sync) | `Files.mismatch == -1L` takes priority: always skip when content identical, even if mtime differs | ✓ Good — avoids unnecessary copies when mtime skew is irrelevant |
| REPLACE_EXISTING for sync copy | Destination mtime must reflect sync time (D-02); never COPY_ATTRIBUTES | ✓ Good — destination timestamps correctly reflect last sync time |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-11 — v3.0 milestone complete; full model rewrite shipped; 161 tests green*
