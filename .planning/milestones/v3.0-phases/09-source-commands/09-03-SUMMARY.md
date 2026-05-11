---
phase: 09-source-commands
plan: "03"
subsystem: cli
tags: [picocli, java, source-commands, freemarker, jackson]

# Dependency graph
requires:
  - phase: 09-source-commands/09-01
    provides: SourceService (getSource, removeSource), SourceEntry model
  - phase: 09-source-commands/09-02
    provides: test harness setUp() pattern reused in this plan

provides:
  - SourceShowCommand: vira source show NAME [--json] with D-05/D-06/D-07 output format
  - SourceRemoveCommand: vira source remove NAME with D-16 error handling
  - SourceShowCommandTest: 8 integration tests covering all output paths
  - SourceRemoveCommandTest: 5 integration tests covering success, deletion, not-found, uninit, missing arg

affects: [10-source-command-registration, SourceCommand parent subcommand wiring]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Positional @Parameters(index=\"0\") for NAME arg (not --name option)"
    - "D-06 conditional block: isTemplates() && !getParameters().isEmpty()"
    - "D-07 JSON via new ObjectMapper().writeValueAsString(entry)"
    - "D-16 error: spec.commandLine().getErr().println(\"Source '<name>' not found.\")"

key-files:
  created:
    - src/main/java/org/saltations/source/SourceShowCommand.java
    - src/main/java/org/saltations/source/SourceRemoveCommand.java
    - src/test/java/org/saltations/source/SourceShowCommandTest.java
    - src/test/java/org/saltations/source/SourceRemoveCommandTest.java
  modified: []

key-decisions:
  - "Positional NAME parameter not option flag — consistent with show/remove commands in other subsystems"
  - "D-06: Parameters block gated on both isTemplates()=true AND non-empty parameters list"
  - "D-07: Jackson ObjectMapper used for single JSON object output (not JSONL)"

patterns-established:
  - "show command: D-05 key-value block order is Name/Path/Templates/Parameters (exact)"
  - "remove command: boolean return from service maps to exit code (false=1, true=0)"
  - "Both commands catch ConfigNotInitializedException separately from IOException"

requirements-completed: [SRC-03, SRC-04]

# Metrics
duration: 8min
completed: 2026-05-09
---

# Phase 09 Plan 03: Source Show and Remove Commands Summary

**SourceShowCommand with D-05/D-06/D-07 multi-line and JSON output, SourceRemoveCommand with D-16 not-found handling — 13 integration tests all passing**

## Performance

- **Duration:** 8 min
- **Started:** 2026-05-09T20:47:26Z
- **Completed:** 2026-05-09T20:55:00Z
- **Tasks:** 2
- **Files modified:** 4 (created)

## Accomplishments
- SourceShowCommand: D-05 ordered key-value block (Name/Path/Templates), D-06 conditional Parameters block, D-07 --json single JSON object, D-16 exit 1 with exact error for missing sources
- SourceRemoveCommand: positional NAME arg, delegates to sourceService.removeSource(), D-16 not-found error, handles ConfigNotInitializedException
- 8 integration tests for show (all format paths, --json, missing source) and 5 for remove (success, config deletion, not-found, uninit, missing arg)
- Full test suite green with no regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: SourceShowCommand with integration tests** - `41aec1e` (feat)
2. **Task 2: SourceRemoveCommand with integration tests** - `fee4ff3` (feat)

## Files Created/Modified
- `src/main/java/org/saltations/source/SourceShowCommand.java` - vira source show NAME [--json] with D-05/D-06/D-07 logic
- `src/main/java/org/saltations/source/SourceRemoveCommand.java` - vira source remove NAME with D-16 not-found handling
- `src/test/java/org/saltations/source/SourceShowCommandTest.java` - 8 integration tests covering all output format and error paths
- `src/test/java/org/saltations/source/SourceRemoveCommandTest.java` - 5 integration tests covering success, deletion, not-found, uninit config, missing arg

## Decisions Made
- Positional @Parameters(index="0") for NAME argument — consistent with show/remove convention (not --name option flag)
- D-06 Parameters block gated on both isTemplates()=true AND non-empty parameters list (double guard prevents spurious "Parameters:" header)
- D-07 Jackson ObjectMapper used for single JSON object output matching plan specification

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None - both commands compiled and all 13 tests passed on first run.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- SourceShowCommand and SourceRemoveCommand complete; ready to wire into SourceCommand parent as subcommands
- Full source CRUD command set now available: add (09-02), list (09-02), show (09-03), remove (09-03)
- All commands follow consistent patterns: @Spec spec, @Parameters(index="0"), delegating to SourceService

---
*Phase: 09-source-commands*
*Completed: 2026-05-09*
