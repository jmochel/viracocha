---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: Unified Sources & Destinations
status: executing
stopped_at: Completed 09-source-commands/09-03-PLAN.md
last_updated: "2026-05-09T20:50:11.125Z"
last_activity: 2026-05-09
progress:
  total_phases: 5
  completed_phases: 1
  total_plans: 6
  completed_plans: 5
  percent: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-05-08)

**Core value:** A developer registers sources and destinations once, then populates any workspace with a single command — and regeneration is safe (skips existing files). Mappings with `sync: true` keep destination copies up to date on demand via `vira sync`.

**Current focus:** Phase 09 — source-commands

## Current Position

Phase: 09 (source-commands) — EXECUTING
Plan: 3 of 4
Status: Ready to execute
Last activity: 2026-05-09

Progress: [░░░░░░░░░░] 0% (v3.0 — 0/5 phases complete)

## Accumulated Context

### Decisions

Decisions logged in PROJECT.md Key Decisions table. Recent v3.0 decisions:

- [v3.0 design]: Collapse catalogs+archetypes into sources with `templates: true/false` flag
- [v3.0 design]: Collapse projects into destinations; eliminate subscriptions in favor of per-mapping `sync: true`
- [v3.0 design]: v2 config files trigger fail-with-instructions (no auto-migration)
- [Phase 8]: Delete v2 packages in dependency order — move shared infra first, then delete packages one at a time compiling after each
- [Phase 08]: Deleted v2 command packages (archetype/catalog/project/subscription) in Plan 01 to achieve compile-clean state after v2 model class deletion
- [Phase 08]: GeneratorService and DefaultSyncService stubbed with UnsupportedOperationException referencing rewrite phases (11 and 12)
- [Phase 08]: Updated GenerateCommand and SyncCommand to v3 terminology so assertFalse checks on v2 command names pass
- [Phase 09-01]: Raw-string traversal check (rawPath.contains('..')) before Path.of() to prevent normalization bypass when validating source paths
- [Phase 09-01]: SourceService stores absolute normalized path in SourceEntry.path for consistent lookup regardless of original input format
- [Phase 09-source-commands]: Positional NAME parameter for show/remove commands, D-06 double guard for Parameters block, D-07 Jackson ObjectMapper for JSON output

### Pending Todos

None.

### Blockers/Concerns

- Phase 8: v2 package deletion requires careful ordering; `ArchetypePathUtils` must move to `infra/` before pattern/catalog/project/subscription packages are removed
- Phase 11: Binary file copy (GEN-04) needs a dedicated integration test with a non-text file to verify no corruption

## Session Continuity

Last session: 2026-05-09T20:50:11.123Z
Stopped at: Completed 09-source-commands/09-03-PLAN.md
Resume file: None
