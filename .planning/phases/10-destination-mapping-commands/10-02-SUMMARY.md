---
phase: 10-destination-mapping-commands
plan: 02
subsystem: cli
tags: [picocli, destination, crud, tdd, java]

# Dependency graph
requires:
  - phase: 10-destination-mapping-commands/10-01
    provides: DestinationService with addDestination/listDestinations/getDestination/removeDestination/addMapping/removeMapping

provides:
  - DestinationAddCommand: vira destination add --name --path with traversal+duplicate validation
  - DestinationListCommand: vira destination list [--json] aligned columns and JSONL output
  - DestinationShowCommand: vira destination show NAME [--json] with Parameters(D-11)/Mappings(D-12)/(all files)(D-13)
  - DestinationRemoveCommand: vira destination remove NAME with boolean not-found check
  - DestinationCommand: group command name="destination" aliases={"dest"} with 7 subcommands (3 stubs for Plan 03)
  - 4 test classes covering all CRUD commands

affects: [10-destination-mapping-commands/10-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Thin command wrappers over service layer; all validation in service, all output in command"
    - "TDD: test files written first (RED compile failure), then implementation (GREEN pass)"
    - "Stub pattern for future commands: @Command + @Singleton + Callable<Integer> returning 0"
    - "D-11: Parameters block omitted when map empty (destination show)"
    - "D-12: Mappings section always rendered; Mappings: (none) when empty"
    - "D-13: null glob displayed as (all files)"

key-files:
  created:
    - src/main/java/org/saltations/destination/DestinationAddCommand.java
    - src/main/java/org/saltations/destination/DestinationListCommand.java
    - src/main/java/org/saltations/destination/DestinationShowCommand.java
    - src/main/java/org/saltations/destination/DestinationRemoveCommand.java
    - src/main/java/org/saltations/destination/DestinationCommand.java
    - src/main/java/org/saltations/destination/DestinationAddMappingCommand.java
    - src/main/java/org/saltations/destination/DestinationListMappingsCommand.java
    - src/main/java/org/saltations/destination/DestinationRemoveMappingCommand.java
    - src/test/java/org/saltations/destination/DestinationAddCommandTest.java
    - src/test/java/org/saltations/destination/DestinationListCommandTest.java
    - src/test/java/org/saltations/destination/DestinationShowCommandTest.java
    - src/test/java/org/saltations/destination/DestinationRemoveCommandTest.java
  modified: []

key-decisions:
  - "Stub pattern for Plan 03 mapping commands: @Command + @Singleton + Callable<Integer> returning 0 allows DestinationCommand group to compile with all 7 subcommands declared"
  - "DestinationAddCommand omits --templates flag and Files.exists check (D-04: destinations may not exist at registration time)"

patterns-established:
  - "Destination CRUD commands mirror Source CRUD commands with destination-specific names and messages"
  - "Show command renders Parameters block conditionally (D-11) and Mappings section unconditionally (D-12)"

requirements-completed: [DEST-01, DEST-02, DEST-03, DEST-04, DEST-05, DEST-06]

# Metrics
duration: 3min
completed: 2026-05-10
---

# Phase 10 Plan 02: Destination CRUD Commands Summary

**Four destination CRUD command classes (Add, List, Show, Remove) plus DestinationCommand group with 7 subcommands (3 stubs for Plan 03), all with integration tests passing**

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-10T14:20:03Z
- **Completed:** 2026-05-10T14:23:25Z
- **Tasks:** 2
- **Files modified:** 12 created

## Accomplishments
- DestinationAddCommand and DestinationListCommand with tests (Task 1, TDD)
- DestinationShowCommand, DestinationRemoveCommand, DestinationCommand group with tests (Task 2, TDD)
- Three stub commands (AddMapping, ListMappings, RemoveMapping) enabling DestinationCommand group to compile with all 7 subcommands declared
- Full Maven test suite passes without regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: DestinationAddCommand, DestinationListCommand, and their tests** - `9f38139` (feat)
2. **Task 2: DestinationShowCommand, DestinationRemoveCommand, DestinationCommand group, and their tests** - `1a1c2b0` (feat)

**Plan metadata:** (to be added after state update commit)

_Note: TDD tasks have test (RED) verified by compile failure, then implementation (GREEN) verified by test pass._

## Files Created/Modified
- `src/main/java/org/saltations/destination/DestinationAddCommand.java` - vira destination add --name --path, no existence check, 3 exception catches
- `src/main/java/org/saltations/destination/DestinationListCommand.java` - aligned columns + --json JSONL output
- `src/main/java/org/saltations/destination/DestinationShowCommand.java` - Name/Path/Parameters(D-11)/Mappings(D-12) output, (all files) null glob (D-13)
- `src/main/java/org/saltations/destination/DestinationRemoveCommand.java` - boolean not-found guard, exact error messages
- `src/main/java/org/saltations/destination/DestinationCommand.java` - group command name="destination" aliases={"dest"}, 7 subcommands
- `src/main/java/org/saltations/destination/DestinationAddMappingCommand.java` - stub for Plan 03
- `src/main/java/org/saltations/destination/DestinationListMappingsCommand.java` - stub for Plan 03
- `src/main/java/org/saltations/destination/DestinationRemoveMappingCommand.java` - stub for Plan 03
- `src/test/java/org/saltations/destination/DestinationAddCommandTest.java` - 7 tests: success, persist, traversal, duplicate, tilde, missing options
- `src/test/java/org/saltations/destination/DestinationListCommandTest.java` - 4 tests: empty, single, multi plain, JSONL
- `src/test/java/org/saltations/destination/DestinationShowCommandTest.java` - 8 tests: all output sections, null/set glob, JSON mode, not-found
- `src/test/java/org/saltations/destination/DestinationRemoveCommandTest.java` - 3 tests: success, not-found, actual removal

## Decisions Made
- Stub pattern for Plan 03 mapping commands allows DestinationCommand group to declare all 7 subcommands at compile time; Plan 03 replaces stubs with full implementations
- DestinationAddCommand has no --templates flag and no Files.exists check — destinations differ from sources in that they may not exist at registration time (D-04)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required

None - no external service configuration required.

## Known Stubs

Three stub commands exist pending Plan 03 implementation:
- `src/main/java/org/saltations/destination/DestinationAddMappingCommand.java` - stub, always returns 0
- `src/main/java/org/saltations/destination/DestinationListMappingsCommand.java` - stub, always returns 0
- `src/main/java/org/saltations/destination/DestinationRemoveMappingCommand.java` - stub, always returns 0

These stubs do not prevent Plan 02's goal (CRUD commands + group compiling) — they are intentional placeholders per the plan spec. Plan 03 will replace them with full implementations.

## Next Phase Readiness
- Plan 03 can now import DestinationCommand and replace the three stubs with full mapping command implementations
- DestinationCommand group is ready for wiring into ViracochaCommand (also Plan 03)

---
*Phase: 10-destination-mapping-commands*
*Completed: 2026-05-10*
