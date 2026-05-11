# Milestones

## v3.0 Unified Sources & Destinations (Shipped: 2026-05-11)

**Phases completed:** 5 phases, 16 plans, 32 tasks

**Key accomplishments:**

- Relocated HiddenPathFilter/FreemarkerVariableExtractor to infra/, introduced v3 SourceEntry/DestinationEntry/MappingEntry/ViracochaConfig POJOs, and stubbed GeneratorService/DefaultSyncService — 32 tests green
- Added ConfigVersionException with version pre-read guard in ConfigService.load(), updated GenerateCommand/SyncCommand to v3 terminology — 37 tests green
- SourceService with path validation and Freemarker extraction wired to ConfigService via injectable @Singleton FreemarkerVariableExtractor
- SourceAddCommand and SourceListCommand as thin picocli wrappers over SourceService with 14 integration tests covering add/list/validation/JSONL output
- SourceShowCommand with D-05/D-06/D-07 multi-line and JSON output, SourceRemoveCommand with D-16 not-found handling — 13 integration tests all passing
- SourceCommand group wired into ViracochaCommand completing the full `vira source` / `vira src` CLI tree with all four leaf commands and smoke-tested via built JAR
- GlobMatcher utility via JDK PathMatcher plus DestinationService with CRUD and mapping operations, 21 tests green
- Four destination CRUD command classes (Add, List, Show, Remove) plus DestinationCommand group with 7 subcommands (3 stubs for Plan 03), all with integration tests passing
- Three full mapping command implementations (add-mapping, list-mappings, remove-mapping) wired into DestinationCommand group, completing all MAP-0x requirements with 136 passing tests.
- Wave 0 test scaffold for Phase 11: 14 named test methods across GeneratorServiceTest and GenerateCommandTest covering GEN-01 through GEN-07, plus a 9-byte binary fixture for byte-integrity testing
- GeneratorService.generate() fully implemented with 6-step traversal algorithm covering flat/recursive copy, skip-existing, glob filtering, hidden path exclusion, Freemarker template expansion, and binary byte-copy — 8/8 tests green
- Interactive destination-creation prompt, 5-arg GeneratorService overload, and 6/6 GenerateCommandTest passing for GEN-05 through GEN-07
- Wave 0 test stub scaffolding for sync rewrite — 13 @Disabled test methods across DefaultSyncServiceTest and SyncCommandTest enabling Nyquist-compliant automated verification for all Phase 12 plans
- v2-to-v3 sync model surgery: new SyncResult record, adapted SyncConflictRecord, redesigned SyncService interface, and compile-clean DefaultSyncService/SyncCommand against v3 types
- Full v3 DefaultSyncService.sync() implementation with timestamp conflict detection and 6 enabled green tests for SYN-01 and SYN-02
- SyncCommandTest fully enabled: 7 tests proving SYN-02 conflict exit-1, SYN-03 required destination, SYN-04 dry-run, SYN-05 verbose per-file, SYN-06 JSON output, SYN-07 summary always printed

---

## v2.0 Subscriptions & sync (Shipped: 2026-05-06)

**Phases completed:** 3 phases, 9 plans, 19 tasks

**Key accomplishments:**

- Subscription model types and YAML round-trip for nested subscriptions under projects
- `vira subscription add` with validation, path guards, and persistence tests
- List, show, and remove subscription subcommands with plain + JSON output and integration test
- Stable `org.saltations.sync` API: conflict kinds, per-subscription results, `SyncEngineResult`, and `SyncService.syncProject` — no filesystem logic yet
- `DefaultSyncService` one-way sync with hidden filtering, lexicographic walks, and conflict detection via `Files.mismatch`
- Bidirectional two-phase sync: analyze union of paths for conflicts; apply catalog→workspace then workspace→catalog
- Sync engine accepts subscription filter, dry-run, and verbose; counts and conflicts match prior behavior when using defaults.
- `vira sync` invokes the sync engine with project, optional subscription, dry-run, verbose, and JSON output; exit 1 on conflicts or failures.
- README documents subscriptions and `vira sync`; integration tests cover publish-to-workspace, workspace-to-publish, and bidirectional conflict exit code.

---

## v1.0 MVP (Shipped: 2026-04-04)

**Phases completed:** 4 phases, 11 plans, 17 tasks

**Key accomplishments:**

- jackson-dataformat-yaml + logstash-logback-encoder wired to pom.xml, JSONL-only logback, XdgPaths XDG utility, and ViracochaConfig round-trip YAML POJO — 6 tests pass
- ConfigService (init/load/save over XDG YAML), full picocli command hierarchy (vira -> config -> init|show), and 17 passing tests using @Spec-based testable output
- Freemarker 2.3.34 added to classpath, typed CatalogEntry/PatternEntry POJOs created, ViracochaConfig upgraded to typed lists, and publisher/pattern group command stubs wired into the full subcommand hierarchy
- All four catalog leaf commands implemented with tests: register (path + duplicate validation), list (plain + JSON), show (plain + JSON), unregister — PUB-01 through PUB-05.
- FreemarkerVariableExtractor walks pattern trees (content + path segments), enforces D-05 malformed `${`, skips hidden subtrees; pattern commands register/list/show/unregister with extracted parameters persisted in YAML.
- Typed `projects` in config and full `vira project` command tree implemented in one pass with plans 02–03.
- `project create`, `list`, and `unregister` with ConfigService persistence and `ProjectCommandsTest` coverage.
- `project add-mapping` with repeatable `--param key=value`, `project show` (plain + JSON), and integration tests.
- Project-level default parameters on `ProjectEntry` plus a Freemarker-backed `PathExpander` for path segments, with YAML round-trip and unit tests.
- GeneratorService walks pattern trees (skipping hidden segments), merges project and mapping parameters, expands path segments and template bodies, supports dry-run and skip-existing, and returns aggregate counts plus optional verbose action lines.
- `vira generate` delegates to GeneratorService, prints workspace-relative verbose actions when requested, always prints the summary line, and includes integration tests for guard rails and flags.

---
