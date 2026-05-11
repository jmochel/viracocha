---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: Unified Sources & Destinations
status: executing
stopped_at: Completed 12-00-PLAN.md
last_updated: "2026-05-11T13:44:35.481Z"
last_activity: 2026-05-11
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 16
  completed_plans: 13
  percent: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-05-08)

**Core value:** A developer registers sources and destinations once, then populates any workspace with a single command — and regeneration is safe (skips existing files). Mappings with `sync: true` keep destination copies up to date on demand via `vira sync`.

**Current focus:** Phase 12 — sync-rewrite

## Current Position

Phase: 12 (sync-rewrite) — EXECUTING
Plan: 2 of 4
Status: Ready to execute
Last activity: 2026-05-11

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
- [Phase 09-source-commands]: SourceCommand alias 'src' per D-10 locked decision; call() returns 0 with picocli auto-help
- [Phase 10-01]: GlobMatcher prepends 'glob:' internally — callers pass clean patterns without prefix
- [Phase 10-01]: Destination paths stored as-is with no normalization or existence check (D-04: destinations may not exist at registration time)
- [Phase 10-01]: Raw-string traversal check before Path.of() in DestinationService mirrors SourceService DEST-06/D-01 pattern
- [Phase 10-destination-mapping-commands]: Stub pattern for Plan 03 mapping commands: @Command+@Singleton+Callable<Integer> returning 0 allows DestinationCommand group to compile with all 7 subcommands declared
- [Phase 10-destination-mapping-commands]: DestinationAddCommand omits --templates and Files.exists check (D-04: destinations may not exist at registration time; differs from SourceAddCommand intentionally)
- [Phase 10-destination-mapping-commands]: Two explicit @Parameters(index=0/1) for remove-mapping NAME INDEX — avoids picocli undefined binding order for multiple positional params
- [Phase 10-destination-mapping-commands]: null glob stored as null and displayed as '(all files)' in list-mappings — no sentinel string in config YAML
- [Phase 11-generate-rewrite]: Auto-create destination directory silently in GeneratorService.generate() — interactive prompt deferred to Plan 02 GenerateCommand integration
- [Phase 11-generate-rewrite]: Template test parameters set on DestinationEntry before addMapping() — parameters live on destination, not mapping
- [Phase 11-generate-rewrite]: Wave 0 uses @Disabled stubs so test scaffold compiles/passes while GeneratorService throws UnsupportedOperationException
- [Phase 11]: 5-arg generate() overload keeps 3-arg for backward compat; dry-run always prints Would-create lines without requiring --verbose
- [Phase 11]: Picocli leaf-command tests: CommandLine rooted at command class, execute() args are options not subcommand names
- [Phase 12-sync-rewrite]: Wave 0 stubs: Javadoc must not contain literal '@Disabled' to satisfy grep -c @Disabled acceptance criteria

### Pending Todos

None.

### Blockers/Concerns

- Phase 8: v2 package deletion requires careful ordering; `ArchetypePathUtils` must move to `infra/` before pattern/catalog/project/subscription packages are removed
- Phase 11: Binary file copy (GEN-04) needs a dedicated integration test with a non-text file to verify no corruption

## Session Continuity

Last session: 2026-05-11T13:44:35.479Z
Stopped at: Completed 12-00-PLAN.md
Resume file: None
