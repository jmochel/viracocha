---
phase: 10-destination-mapping-commands
plan: 03
subsystem: cli
tags: [picocli, destination, mapping, tdd, jackson, jsonl]

# Dependency graph
requires:
  - phase: 10-01
    provides: DestinationService with addMapping/listMappings/removeMapping, GlobMatcher, MappingEntry model
  - phase: 10-02
    provides: DestinationCommand group with stub mapping command classes (add-mapping, list-mappings, remove-mapping)
provides:
  - Full DestinationAddMappingCommand (add-mapping subcommand with --source required, --glob, --recurse, --sync)
  - Full DestinationListMappingsCommand (list-mappings with numbered blocks, null-glob display, --json JSONL mode)
  - Full DestinationRemoveMappingCommand (remove-mapping with two positional params, IndexOutOfBoundsException handling)
  - DestinationCommand wired into ViracochaCommand subcommands — `vira destination` accessible from root CLI
  - 16 integration tests covering all MAP-0x requirements
affects: [phase-11-generate, phase-12-sync, ViracochaCommand]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Two-positional-parameter pattern: @Parameters(index=0/1) for NAME+INDEX commands — order is explicit and unambiguous"
    - "Mapping block output format: numbered Mapping N: blocks with Source/Glob/Recurse/Sync fields"
    - "Null glob displayed as '(all files)' in human-readable output"
    - "IndexOutOfBoundsException from service layer caught at command layer and printed to stderr"

key-files:
  created:
    - src/main/java/org/saltations/destination/DestinationAddMappingCommand.java
    - src/main/java/org/saltations/destination/DestinationListMappingsCommand.java
    - src/main/java/org/saltations/destination/DestinationRemoveMappingCommand.java
    - src/test/java/org/saltations/destination/DestinationAddMappingCommandTest.java
    - src/test/java/org/saltations/destination/DestinationListMappingsCommandTest.java
    - src/test/java/org/saltations/destination/DestinationRemoveMappingCommandTest.java
  modified:
    - src/main/java/org/saltations/ViracochaCommand.java

key-decisions:
  - "Two explicit @Parameters(index=0/1) for remove-mapping — avoids picocli's undefined binding order for multiple positional params"
  - "null glob stored and displayed as '(all files)' — picocli leaves @Option field null when flag not provided"
  - "DestinationCommand added between SourceCommand and GenerateCommand in ViracochaCommand subcommands array"

patterns-established:
  - "Mapping output format (Mapping N:, Source:, Glob:, Recurse:, Sync:) established for consistent display across list-mappings and show commands"
  - "Service returns false for not-found vs throws IndexOutOfBoundsException for invalid index — command layer distinguishes both cases"

requirements-completed: [MAP-01, MAP-02, MAP-03, MAP-04, MAP-05]

# Metrics
duration: 3min
completed: 2026-05-10
---

# Phase 10 Plan 03: Destination Mapping Commands Summary

**Three full mapping command implementations (add-mapping, list-mappings, remove-mapping) wired into DestinationCommand group, completing all MAP-0x requirements with 136 passing tests.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-10T14:26:12Z
- **Completed:** 2026-05-10T14:29:13Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Replaced all three mapping command stubs with full picocli implementations calling DestinationService
- Added 16 integration tests covering success paths, persistence verification, not-found errors, and index bounds errors
- Wired DestinationCommand into ViracochaCommand — `vira destination` is now accessible from the root CLI
- Full Maven test suite passes: 136 tests, 0 failures

## Task Commits

Each task was committed atomically:

1. **Task 1: DestinationAddMappingCommand, DestinationListMappingsCommand, and their tests** - `8905d31` (feat)
2. **Task 2: DestinationRemoveMappingCommand, ViracochaCommand wiring, final smoke test** - `a68631e` (feat)

**Plan metadata:** (docs commit — see below)

_Note: TDD tasks followed RED (failing tests) then GREEN (implementation) pattern._

## Files Created/Modified

- `src/main/java/org/saltations/destination/DestinationAddMappingCommand.java` - Full add-mapping: @Parameters destName, @Option --source (required), --glob, --recurse, --sync; calls DestinationService.addMapping
- `src/main/java/org/saltations/destination/DestinationListMappingsCommand.java` - Full list-mappings: @Parameters destName, @Option --json; numbered blocks with null-glob as '(all files)'; JSONL mode via Jackson ObjectMapper
- `src/main/java/org/saltations/destination/DestinationRemoveMappingCommand.java` - Full remove-mapping: two @Parameters(index=0/1) for destName and int index; catches IndexOutOfBoundsException for bounds error
- `src/main/java/org/saltations/ViracochaCommand.java` - Added DestinationCommand.class import and to subcommands array
- `src/test/java/org/saltations/destination/DestinationAddMappingCommandTest.java` - 6 tests: success+confirmation, persistence, all-flags persistence, unknown dest, unknown source, missing --source
- `src/test/java/org/saltations/destination/DestinationListMappingsCommandTest.java` - 5 tests: mapping block output, null glob display, empty message, JSONL output, recurse/sync fields
- `src/test/java/org/saltations/destination/DestinationRemoveMappingCommandTest.java` - 5 tests: success+confirmation, actual removal, unknown dest, out-of-range index, empty dest

## Decisions Made

- Used explicit `@Parameters(index="0")` and `@Parameters(index="1")` for remove-mapping to guarantee deterministic binding of destName and int index.
- null glob stored as null (picocli default when @Option not provided) and displayed as '(all files)' in list-mappings — no sentinel string stored in config.
- DestinationCommand placed between SourceCommand and GenerateCommand in ViracochaCommand subcommands for logical grouping (sources before destinations before generate).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 10 is complete: all 9 Phase 10 test classes exist and pass (DestinationServiceTest, GlobMatcherTest, DestinationAddCommandTest, DestinationListCommandTest, DestinationShowCommandTest, DestinationRemoveCommandTest, DestinationAddMappingCommandTest, DestinationListMappingsCommandTest, DestinationRemoveMappingCommandTest)
- `vira destination` is fully accessible from root CLI with all 7 subcommands (add, list, show, remove, add-mapping, list-mappings, remove-mapping)
- Phase 11 (generate) and Phase 12 (sync) can now use DestinationService.listMappings() to iterate over mappings for generation/sync operations

---
*Phase: 10-destination-mapping-commands*
*Completed: 2026-05-10*
