# Viracocha

## What This Is

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable Freemarker-templated patterns and folder sources (catalogs), then generating workspace content from those registrations. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

## Core Value

A developer can register patterns and catalogs once, then generate a correctly-structured workspace with a single command — and regenerating is safe (skips existing files).

## Current Milestone: v2.0 Subscriptions & sync

**Goal:** Let users attach **subscriptions** from registered **catalogs** into a **project workspace**, then run **`vira sync`** to propagate published artifacts **to and from** the workspace with explicit direction and conflict rules.

**Target features:**

- Subscription records in central YAML (link project ↔ catalog, source/dest paths, sync direction).
- CLI to add, list, show, and remove subscriptions; validation against existing projects and catalogs.
- `vira sync` (project-scoped) that performs filesystem sync per subscription direction: catalog→workspace, workspace→catalog, or bidirectional.
- Documented conflict policy when both sides change (e.g. fail with diff summary, or last-write-wins with opt-in flags) — see `.planning/REQUIREMENTS.md`.
- Tests and docs for subscription + sync flows.

## Requirements

### Validated (v1.0 — shipped 2026-04-04)

- ✓ CLI scaffold with Micronaut DI + picocli; Maven + JUnit 5
- ✓ Config init/show; XDG YAML central config; JSONL logging
- ✓ Catalogs, patterns (Freemarker params), projects, mappings
- ✓ `vira generate` with skip-existing, dry-run, verbose
- ✓ Full detail: `.planning/milestones/v1.0-REQUIREMENTS.md`

### Validated (v2.0 — partial)

- ✓ Subscription rows in central YAML (`SubscriptionEntry` under `ProjectEntry`); `vira subscription` add, list, show, remove with plain + JSON output (Phase 5)

### Validated (v2.0 — 2026-04-04)

- ✓ Sync engine (`DefaultSyncService`) with direction, conflict detection, hidden-path parity (Phase 6)
- ✓ `vira sync --project-name` with `--subscription`, `--dry-run`, `--verbose`, `--json`; README subscriptions/sync section; integration tests (Phase 7)

### Active (v2.0)

See `.planning/REQUIREMENTS.md` for traceability and any follow-up items.

### Out of Scope

- Remote catalogs (HTTP/Git) — local filesystem paths only (same as v1 until a later milestone).
- **Watch mode / background daemon** — deferred to v2.1+ (one-shot `vira sync` is in v2.0).
- Graal native image, multiple config profiles — unchanged from v1 deferrals.

## Context

**Shipped v1.0 (2026-04-04):** Full CLI per roadmap — config init/show, catalogs, patterns with Freemarker params, projects and mappings, and `vira generate` with skip-existing, dry-run, and verbose output. Requirements archive: `.planning/milestones/v1.0-REQUIREMENTS.md`.

**Current:** v2.0 milestone shipped (Phases 5–7): subscriptions, sync engine, `vira sync`, and docs. Roadmap: `.planning/ROADMAP.md`.

The project is named "viracocha" (package: `org.saltations`). The CLI binary is `vira`. The existing skeleton uses Micronaut for dependency injection alongside picocli — this is intentional and should be preserved.

Patterns use Apache Freemarker for template expansion. Variables appear in file content AND in folder/file names. Parameter names are extracted from the pattern source at registration time (by scanning Freemarker directives).

Central config is a single YAML file following the schema in the project description — `version`, `patterns[]`, `catalogs[]`, `projects[]`. The XDG config path would be `$XDG_CONFIG_HOME/viracocha/config.yaml` (defaulting to `~/.config/viracocha/config.yaml`).

Project params provide defaults; mapping `values` override per-mapping.

## Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — unchanged for v2.0
- **Config format**: YAML only for central config; subscription schema evolves with a `version` field bump if needed
- **Regeneration**: `vira generate` keeps skip-existing semantics; sync is a separate code path with its own overwrite/conflict rules
- **Scope**: Local filesystem only — no network, no Git operations in v2.0

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep Micronaut DI | Already in skeleton; provides clean DI for services without manual wiring | — Pending |
| Skip-existing on generate | Protects hand-edited workspace files from being overwritten | — Pending |
| Subscriptions deferred to v2 | Adds significant complexity (sync semantics, conflict resolution); generate + patterns covers the core use case first | **Superseded** — v2.0 implements subscriptions + sync |
| XDG config paths | Standard for Linux CLI tools; consistent with tool ecosystem | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-04 — v2.0 milestone complete (`vira sync`, engine, README)*
