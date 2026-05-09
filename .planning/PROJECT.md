# Viracocha

## What This Is

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable named **sources** (local folder paths or remote URLs, optionally Freemarker-templated) and **destinations** (workspace root paths), then populating destination paths from sources via **mappings** that specify glob filtering, recursion, and sync behavior. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

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

### Validated (v3.0 — in progress)

- ✓ CFG-01: v3 config POJOs (SourceEntry, DestinationEntry, MappingEntry, ViracochaConfig v3); YAML round-trip without data loss — Phase 8
- ✓ CFG-02: ConfigService.load() version guard; ConfigVersionException for v2 config files — Phase 8
- ✓ CFG-03: All v2 command packages (archetype, catalog, project, subscription) removed; ViracochaCommand registers only config/generate/sync — Phase 8

- ✓ SRC-01–SRC-07: `SourceService` with full CRUD + path validation (anti-traversal, existence, duplicate); `vira source add/list/show/remove` commands registered under `SourceCommand` group; Freemarker variable extraction via `@Singleton FreemarkerVariableExtractor`; `--json` output on show/list; aligned column list; exact error messages — Phase 9

### Active (v3.0 — in progress)

## Current Milestone: v3.0 Unified Sources & Destinations

**Goal:** Replace the catalog/archetype/project/subscription model with a unified sources/destinations schema — cleaner, simpler, and extensible to remote content.

**Target features:**
- New config schema: `sources[]` (with `templates: true/false`), `destinations[]` with nested `parameters[]` and `mappings[]`
- `vira source add/list/show/remove` — supports local paths and http(s) URLs
- `vira destination add/list/show/remove`
- `vira mapping add/list/remove` (per destination)
- `vira generate` updated for new schema
- `vira sync` updated (source→destination only; per-mapping `sync: true/false` flag)
- Remote source support: fetch-and-copy from http(s) URLs at generate/sync time (no local caching)
- Remove all v2 commands (catalog, archetype, project, subscription) — breaking change; no migration

### Out of Scope

- Sync *to* remote sources (HTTP/Git) — sources are read-only; write operations stay on local filesystem
- Watch mode / background sync daemon — deferred to v4+ (one-shot `vira sync` covers the use case)
- Graal native image — GraalVM profile exists in pom.xml but native build is not a current target
- Multiple config profiles — single XDG config path is sufficient for current use case
- Interactive merge UI — scriptable CLI-first policy; conflict abort is the correct default
- Bidirectional sync — v3 sync is source→destination only; the old ws→catalog direction is removed with subscriptions

## Context

**Shipped v1.0 (2026-04-04):** Full CLI per roadmap — config init/show, catalogs, archetypes with Freemarker params, projects and mappings, and `vira generate` with skip-existing, dry-run, and verbose output. 4 phases, 11 plans. Requirements archive: `.planning/milestones/v1.0-REQUIREMENTS.md`.

**Shipped v2.0 (2026-04-04):** Subscriptions, sync engine, `vira sync` CLI, and docs. 3 phases, 9 plans. 5,191 LOC Java. Requirements archive: `.planning/milestones/v2.0-REQUIREMENTS.md`.

**v3.0 (in design — 2026-05):** Unified sources/destinations config model. Eliminates catalog, archetype, project, and subscription concepts. Mappings become the single join point between a source and a destination, with glob, recursion, and sync semantics per mapping. Schema migration required from v2 config files.

The project is named "viracocha" (package: `org.saltations`). The CLI binary is `vira`. Micronaut + picocli is intentional and must be preserved.

Sources with `templates: true` use Apache Freemarker for template expansion. Variables appear in file content AND in folder/file names. Parameter names are extracted from the source at registration time or declared explicitly.

Central config is a single YAML file: `~/.config/viracocha/config.yaml` (XDG). v3 schema: `version`, `sources[]`, `destinations[]` (with nested `parameters[]` and `mappings[]`).

## Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — no deviations
- **Config format**: YAML only for central config
- **Regeneration**: `vira generate` keeps skip-existing semantics; sync is a separate code path
- **Scope**: Local filesystem reads + remote HTTP(S) reads — no write operations to remote sources

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
| Collapse catalogs+archetypes into sources (v3) | Two concepts with one distinguishing flag were unnecessary complexity; `templates: true/false` captures the difference cleanly | ⏳ Pending validation |
| Collapse projects into destinations (v3) | Projects were destinations with extra ceremony; flat `destinations[]` list is simpler and more general (covers user-level dirs, not just projects) | ⏳ Pending validation |
| Eliminate subscriptions in favor of per-mapping sync flag (v3) | Subscriptions as a separate top-level concept added indirection; `sync: true` on a mapping is the natural place for this intent | ⏳ Pending validation |
| Add glob + recurse per mapping (v3) | Enables fine-grained selection from a single source into multiple destinations without multiplying source registrations | ⏳ Pending validation |
| Remote sources read-only (v3) | Writing to remote repos is out of scope; sources are always the authoritative origin | ⏳ Pending validation |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-09 — Phase 8 complete; v3 model foundation in place; v2 command packages removed*
