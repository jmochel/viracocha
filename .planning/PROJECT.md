# Viracocha

## What This Is

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable Freemarker-templated archetypes and folder sources (catalogs), then generating workspace content from those registrations and keeping catalog artifacts in sync with workspace copies via subscriptions. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

## Core Value

A developer can register archetypes and catalogs once, then generate a correctly-structured workspace with a single command — and regenerating is safe (skips existing files). Subscriptions keep catalog-published files in sync with the workspace on demand via `vira sync`.

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

### Active

*(No active requirements — start fresh with `/gsd:new-milestone` to define v3.0 requirements)*

### Out of Scope

- Remote catalogs (HTTP/Git) — local filesystem paths only; network/auth complexity deferred indefinitely
- Watch mode / background sync daemon — deferred to v3.0+ (one-shot `vira sync` covers the use case)
- Graal native image — GraalVM profile exists in pom.xml but native build is not a target for v2.0
- Multiple config profiles — single XDG config path is sufficient for current use case
- Interactive merge UI — scriptable CLI-first policy; conflict abort is the correct v2.0 default

## Context

**Shipped v1.0 (2026-04-04):** Full CLI per roadmap — config init/show, catalogs, archetypes with Freemarker params, projects and mappings, and `vira generate` with skip-existing, dry-run, and verbose output. 4 phases, 11 plans. Requirements archive: `.planning/milestones/v1.0-REQUIREMENTS.md`.

**Shipped v2.0 (2026-04-04):** Subscriptions, sync engine, `vira sync` CLI, and docs. 3 phases, 9 plans. 5,191 LOC Java. Requirements archive: `.planning/milestones/v2.0-REQUIREMENTS.md`.

The project is named "viracocha" (package: `org.saltations`). The CLI binary is `vira`. Micronaut + picocli is intentional and must be preserved.

Archetypes use Apache Freemarker for template expansion. Variables appear in file content AND in folder/file names. Parameter names are extracted from the archetype source at registration time.

Central config is a single YAML file: `~/.config/viracocha/config.yaml` (XDG). Schema: `version`, `archetypes[]`, `catalogs[]`, `projects[]` (with nested `subscriptions[]`).

## Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — no deviations
- **Config format**: YAML only for central config
- **Regeneration**: `vira generate` keeps skip-existing semantics; sync is a separate code path
- **Scope**: Local filesystem only — no network, no Git operations

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

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-06 after v2.0 milestone — Subscriptions & sync shipped*
