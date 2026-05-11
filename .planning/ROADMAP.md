# Roadmap: Viracocha

## Milestones

- ✅ **v1.0 MVP** — Phases 1–4 (shipped 2026-04-04) — [`milestones/v1.0-ROADMAP.md`](milestones/v1.0-ROADMAP.md)
- ✅ **v2.0 Subscriptions & sync** — Phases 5–7 (shipped 2026-04-04) — [`milestones/v2.0-ROADMAP.md`](milestones/v2.0-ROADMAP.md)
- 🚧 **v3.0 Unified Sources & Destinations** — Phases 8–12 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–4) — SHIPPED 2026-04-04</summary>

- [x] Phase 1: Foundation (2/2 plans) — completed 2026-03-28
- [x] Phase 2: Catalogs and Archetypes (3/3 plans) — completed 2026-04-04
- [x] Phase 3: Projects and Mappings (3/3 plans) — completed 2026-04-04
- [x] Phase 4: Workspace Generation (3/3 plans) — completed 2026-04-04

</details>

<details>
<summary>✅ v2.0 Subscriptions & sync (Phases 5–7) — SHIPPED 2026-04-04</summary>

- [x] Phase 5: Subscription model & CLI (3/3 plans) — completed 2026-04-04
- [x] Phase 6: Sync engine (3/3 plans) — completed 2026-04-04
- [x] Phase 7: `vira sync` & docs (3/3 plans) — completed 2026-04-04

</details>

### 🚧 v3.0 Unified Sources & Destinations (In Progress)

**Milestone Goal:** Replace the catalog/archetype/project/subscription model with a unified sources/destinations schema. Every v2 command package is removed. `vira generate` and `vira sync` are rewritten against the new model.

- [ ] **Phase 8: Model & Config Foundation** - Delete v2 packages, introduce v3 POJOs, add version guard
- [x] **Phase 9: Source Commands** - `vira source` CRUD with Freemarker param extraction (completed 2026-05-09)
- [x] **Phase 10: Destination & Mapping Commands** - `vira destination` CRUD plus mapping subcommands and GlobMatcher (completed 2026-05-10)
- [x] **Phase 11: Generate Rewrite** - Full GeneratorService rewrite for v3 schema (completed 2026-05-10)
- [x] **Phase 12: Sync Rewrite** - DefaultSyncService rewrite for source-to-destination, mapping-driven sync (completed 2026-05-11)

## Phase Details

### Phase 8: Model & Config Foundation
**Goal**: The v3 config schema is fully defined, YAML round-trips without data loss, old v2 command packages are gone, and the tool guards against stale v2 config files
**Depends on**: Phase 7 (v2.0 complete)
**Requirements**: CFG-01, CFG-02, CFG-03
**Success Criteria** (what must be TRUE):
  1. `vira config show` displays a v3 config with `sources` and `destinations` lists without error
  2. A v2 config file (missing version field) causes `vira` to print a clear error message instructing the user to recreate their config and exits with a non-zero code
  3. Running any old v2 command name (`vira catalog`, `vira archetype`, `vira project`, `vira subscription`) produces an "unknown command" error
  4. All v3 POJO fields (SourceEntry, DestinationEntry, MappingEntry) survive a YAML write-then-read cycle with no data loss
**Plans**: 2 plans
Plans:
- [x] 08-01-PLAN.md — Infra relocation, v3 POJOs, ViracochaConfig replacement, test scaffolding (CFG-01)
- [x] 08-02-PLAN.md — Version guard (ConfigVersionException), v2 package deletion, ViracochaCommand cleanup (CFG-02, CFG-03)

### Phase 9: Source Commands
**Goal**: Users can register and manage named local directory sources, including automatic Freemarker variable extraction for template sources
**Depends on**: Phase 8
**Requirements**: SRC-01, SRC-02, SRC-03, SRC-04, SRC-05, SRC-06, SRC-07
**Success Criteria** (what must be TRUE):
  1. User can run `vira source add --name my-source --path /some/dir` and see the source persisted in config
  2. User can run `vira source add --name my-source --path /some/dir --templates` and see extracted Freemarker variable names listed in `vira source show my-source`
  3. `vira source list` displays all registered sources in plain and `--json` output formats
  4. `vira source remove my-source` removes the source and confirms deletion
  5. Adding a duplicate source name or a path containing `..` produces a clear error and leaves config unchanged
**Plans**: 4 plans
Plans:
- [x] 09-01-PLAN.md — @Singleton on FreemarkerVariableExtractor, SourceService with full CRUD and validation (SRC-05, SRC-06, SRC-07)
- [x] 09-02-PLAN.md — SourceAddCommand and SourceListCommand with integration tests (SRC-01, SRC-02)
- [x] 09-03-PLAN.md — SourceShowCommand and SourceRemoveCommand with integration tests (SRC-03, SRC-04)
- [x] 09-04-PLAN.md — SourceCommand group wiring into ViracochaCommand, end-to-end smoke test (SRC-01, SRC-02, SRC-03, SRC-04)

### Phase 10: Destination & Mapping Commands
**Goal**: Users can register destinations and attach mappings that reference sources, with glob filtering, recurse, and sync flags per mapping
**Depends on**: Phase 9
**Requirements**: DEST-01, DEST-02, DEST-03, DEST-04, DEST-05, DEST-06, MAP-01, MAP-02, MAP-03, MAP-04, MAP-05
**Success Criteria** (what must be TRUE):
  1. User can run `vira destination add --name my-ws --path ~/workspace` and see it in `vira destination list`
  2. User can run `vira destination add-mapping my-ws --source my-source --glob "**/*.md" --recurse --sync` and see the mapping in `vira destination show my-ws`
  3. `vira destination remove-mapping my-ws 0` removes the first mapping by index
  4. Adding a mapping with an unknown source name produces a clear error; config is not modified
  5. A glob pattern containing `+` is treated as a literal character (not a regex quantifier) by GlobMatcher
  6. Adding a duplicate destination name or a path containing `..` produces a clear error
**Plans**: 3 plans
Plans:
- [x] 10-01-PLAN.md — GlobMatcher infra utility and DestinationService with full CRUD and mapping operations (MAP-05, DEST-01, DEST-05, DEST-06)
- [x] 10-02-PLAN.md — Destination CRUD commands (Add, List, Show, Remove) with DestinationCommand group (DEST-01, DEST-02, DEST-03, DEST-04, DEST-05, DEST-06)
- [x] 10-03-PLAN.md — Mapping commands (AddMapping, ListMappings, RemoveMapping), ViracochaCommand wiring (MAP-01, MAP-02, MAP-03, MAP-04, MAP-05)

### Phase 11: Generate Rewrite
**Goal**: `vira generate` traverses the v3 destinations/mappings/sources structure, applies glob and recurse filters, expands Freemarker templates in paths and content, and skips existing destination files
**Depends on**: Phase 10
**Requirements**: GEN-01, GEN-02, GEN-03, GEN-04, GEN-05, GEN-06, GEN-07
**Success Criteria** (what must be TRUE):
  1. `vira generate` copies files from all mapped sources into their destination paths
  2. Re-running `vira generate` on a destination that already contains files skips every existing file and reports the skip count
  3. Files from a source with `templates: true` have Freemarker variables expanded in both file content and path segments
  4. Files from a source with `templates: false` are byte-copied without string processing, preserving binary files intact
  5. `vira generate --destination-name my-ws` limits generation to that single destination
  6. `vira generate --dry-run` reports all planned actions without writing any files
**Plans**: 3 plans
Plans:
- [x] 11-00-PLAN.md — Test scaffolding: GeneratorServiceTest and GenerateCommandTest stubs + binary fixture (GEN-01..GEN-07)
- [x] 11-01-PLAN.md — GeneratorService.generate() v3 traversal implementation (GEN-01, GEN-02, GEN-03, GEN-04)
- [x] 11-02-PLAN.md — GenerateCommand interactive prompt, dry-run, verbose wiring (GEN-05, GEN-06, GEN-07)

### Phase 12: Sync Rewrite
**Goal**: `vira sync` copies changed source files to destinations for all mappings with `sync: true`, detects conflicts, and reports counts — source-to-destination direction only
**Depends on**: Phase 11
**Requirements**: SYN-01, SYN-02, SYN-03, SYN-04, SYN-05, SYN-06, SYN-07
**Success Criteria** (what must be TRUE):
  1. `vira sync` updates destination files whose source content has changed, for all mappings flagged `sync: true`
  2. When a destination file differs from the source and has also been locally modified, `vira sync` aborts with a conflict error and exits with code 1
  3. `vira sync --destination-name my-ws` limits sync to that single destination
  4. `vira sync --dry-run` reports what would be copied without touching the filesystem
  5. `vira sync --json` outputs a machine-readable summary; `vira sync` always prints a summary line with copied/skipped/failed/conflict counts
**Plans**: 4 plans
Plans:
- [x] 12-00-PLAN.md — Test scaffolding: DefaultSyncServiceTest and SyncCommandTest stubs (SYN-01..SYN-07)
- [x] 12-01-PLAN.md — Model surgery: SyncResult record, SyncService interface v3, delete v2 artifacts (SYN-01..SYN-07)
- [x] 12-02-PLAN.md — DefaultSyncService.sync() traversal with timestamp conflict detection (SYN-01, SYN-02)
- [x] 12-03-PLAN.md — SyncCommand integration: enable SyncCommandTest assertions (SYN-03, SYN-04, SYN-05, SYN-06, SYN-07)

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 2/2 | Complete | 2026-03-28 |
| 2. Catalogs and Archetypes | v1.0 | 3/3 | Complete | 2026-04-04 |
| 3. Projects and Mappings | v1.0 | 3/3 | Complete | 2026-04-04 |
| 4. Workspace Generation | v1.0 | 3/3 | Complete | 2026-04-04 |
| 5. Subscription model & CLI | v2.0 | 3/3 | Complete | 2026-04-04 |
| 6. Sync engine | v2.0 | 3/3 | Complete | 2026-04-04 |
| 7. `vira sync` & docs | v2.0 | 3/3 | Complete | 2026-04-04 |
| 8. Model & Config Foundation | v3.0 | 0/2 | Not started | - |
| 9. Source Commands | v3.0 | 4/4 | Complete   | 2026-05-09 |
| 10. Destination & Mapping Commands | v3.0 | 3/3 | Complete    | 2026-05-10 |
| 11. Generate Rewrite | v3.0 | 3/3 | Complete    | 2026-05-10 |
| 12. Sync Rewrite | v3.0 | 4/4 | Complete    | 2026-05-11 |
