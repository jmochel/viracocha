# Viracocha

## What This Is

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable Freemarker-templated patterns and folder sources (publishers), then generating workspace content from those registrations. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

## Core Value

A developer can register patterns and publishers once, then generate a correctly-structured workspace with a single command — and regenerating is safe (skips existing files).

## Requirements

### Validated

- ✓ CLI scaffold with Micronaut DI + picocli running — existing
- ✓ Maven build pipeline with JUnit 5 test infrastructure — existing
- ✓ **Phase 3 — Project management:** create/list/show/unregister project; add-mapping with pattern validation and params; persists in central YAML (`PROJ-01`..`PROJ-06`)

### Active

**Config**
- [ ] `vira config init` initializes XDG-compliant config directory and empty central YAML config file

**Publisher Management**
- [ ] `vira publisher register -name <name> -path <path>` registers a named publisher in central config
- [ ] `vira publisher list` displays all registered publishers

**Pattern Management**
- [ ] `vira pattern register -name <name> -path <path>` registers a named pattern in central config and extracts its Freemarker parameters
- [ ] `vira pattern list` displays all registered patterns with their required parameters
- [ ] `vira pattern show -name <name>` displays pattern details including extracted parameter names

**Workspace Generation**
- [ ] `vira generate -project-name <name>` expands all project mappings using Freemarker, resolving pattern + project-level params, writing files to workspace path
- [ ] Generate skips files that already exist (idempotent create behavior)
- [ ] Generate creates intermediate directories as needed
- [ ] Folder name and file name variables are expanded in addition to file content

**Logging & Config**
- [ ] JSONL format for structured log output
- [ ] Logback for log implementation
- [ ] Central config stored as YAML (jackson-dataformat-yaml)
- [ ] XDG Base Directory Specification for config path resolution

### Out of Scope

- Subscriptions (sync from publisher to workspace) — deferred to v2
- `vira sync` command — deferred to v2
- Watch mode (background file change detection) — deferred to v2
- Bidirectional sync — deferred to v2
- Remote publishers (HTTP/Git) — not in scope; local paths only

## Context

**Shipped v1.0 (2026-04-04):** Full CLI per roadmap — config init/show, publishers, patterns with Freemarker params, projects and mappings, and `vira generate` with skip-existing, dry-run, and verbose output. Requirements archive: `.planning/milestones/v1.0-REQUIREMENTS.md`.

The project is named "viracocha" (package: `org.saltations`). The CLI binary is `vira`. The existing skeleton uses Micronaut for dependency injection alongside picocli — this is intentional and should be preserved.

Patterns use Apache Freemarker for template expansion. Variables appear in file content AND in folder/file names. Parameter names are extracted from the pattern source at registration time (by scanning Freemarker directives).

Central config is a single YAML file following the schema in the project description — `version`, `patterns[]`, `publishers[]`, `projects[]`. The XDG config path would be `$XDG_CONFIG_HOME/viracocha/config.yaml` (defaulting to `~/.config/viracocha/config.yaml`).

Project params provide defaults; mapping `values` override per-mapping.

## Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — no deviations in v1
- **Config format**: YAML only (no JSON, TOML, or properties files for central config)
- **Regeneration**: Generate must never overwrite existing workspace files (skip-existing semantics)
- **Scope**: Local filesystem only — no network, no Git operations in v1

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep Micronaut DI | Already in skeleton; provides clean DI for services without manual wiring | — Pending |
| Skip-existing on generate | Protects hand-edited workspace files from being overwritten | — Pending |
| Subscriptions deferred to v2 | Adds significant complexity (sync semantics, conflict resolution); generate + patterns covers the core use case first | — Pending |
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
*Last updated: 2026-04-04 — v1.0 MVP milestone archived*
