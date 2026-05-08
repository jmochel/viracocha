---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: Unified Sources & Destinations
status: active
stopped_at: —
last_updated: "2026-05-08T00:00:00.000Z"
last_activity: 2026-05-08
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-05-08)

**Core value:** A developer registers sources and destinations once, then populates any workspace with a single command — and regeneration is safe (skips existing files). Mappings with `sync: true` keep destination copies up to date on demand via `vira sync`.

**Current focus:** Phase 8 — Model & Config Foundation

## Current Position

Phase: 8 of 12 (Model & Config Foundation)
Plan: 0 of TBD in current phase
Status: Ready to plan
Last activity: 2026-05-08 — v3.0 roadmap created; phases 8–12 defined

Progress: [░░░░░░░░░░] 0% (v3.0 — 0/5 phases complete)

## Accumulated Context

### Decisions

Decisions logged in PROJECT.md Key Decisions table. Recent v3.0 decisions:

- [v3.0 design]: Collapse catalogs+archetypes into sources with `templates: true/false` flag
- [v3.0 design]: Collapse projects into destinations; eliminate subscriptions in favor of per-mapping `sync: true`
- [v3.0 design]: v2 config files trigger fail-with-instructions (no auto-migration)
- [Phase 8]: Delete v2 packages in dependency order — move shared infra first, then delete packages one at a time compiling after each

### Pending Todos

None.

### Blockers/Concerns

- Phase 8: v2 package deletion requires careful ordering; `ArchetypePathUtils` must move to `infra/` before pattern/catalog/project/subscription packages are removed
- Phase 11: Binary file copy (GEN-04) needs a dedicated integration test with a non-text file to verify no corruption

## Session Continuity

Last session: 2026-05-08
Stopped at: Roadmap created — ready to plan Phase 8
Resume file: —
