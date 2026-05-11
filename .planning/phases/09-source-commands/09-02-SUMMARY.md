---
phase: 09-source-commands
plan: "02"
subsystem: cli
tags: [picocli, micronaut, source-commands, tdd, jsonl, column-alignment]

# Dependency graph
requires:
  - phase: 09-source-commands
    plan: "01"
    provides: SourceService with addSource/listSources/getSource/removeSource
provides:
  - SourceAddCommand: vira source add --name --path [--templates]
  - SourceListCommand: vira source list [--json]
  - 14 integration tests covering SRC-01, SRC-02, SRC-05, SRC-06, SRC-07
affects: [SourceCommand group wiring, 09-03-show-remove-commands]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Thin command pattern: commands parse args, call service, format output — no business logic in commands"
    - "TDD red-green: test file committed (compilation failure) then implementation committed (all tests pass)"
    - "XdgPaths stub + @TempDir isolation: inline anonymous XdgPaths in @BeforeEach, no @MicronautTest"
    - "JSONL output: ObjectMapper.writeValueAsString per entry, one println per source"
    - "Column alignment: maxNameWidth computed via stream before printing"

key-files:
  created:
    - src/main/java/org/saltations/source/SourceAddCommand.java
    - src/main/java/org/saltations/source/SourceListCommand.java
    - src/test/java/org/saltations/source/SourceAddCommandTest.java
    - src/test/java/org/saltations/source/SourceListCommandTest.java
  modified: []

key-decisions:
  - "Validation stays in SourceService — SourceAddCommand delegates entirely without re-checking path or name rules"
  - "Empty list exits 0 with no output (no header, no placeholder text) per D-03"
  - "JSONL format uses fresh ObjectMapper per call (same pattern as SyncCommand)"

patterns-established:
  - "Thin command pattern: all validation in service layer, command only calls service + formats output"
  - "spec.commandLine().getOut()/.getErr() for all output — never System.out/System.err (D-11)"
  - "JSONL: one JSON object per println, not a JSON array"

requirements-completed: [SRC-01, SRC-02, SRC-05, SRC-06, SRC-07]

# Metrics
duration: 2min
completed: 2026-05-09
---

# Phase 09, Plan 02: Source Add and List Commands Summary

**SourceAddCommand and SourceListCommand as thin picocli wrappers over SourceService with 14 integration tests covering add/list/validation/JSONL output**

## Performance

- **Duration:** 2 min
- **Started:** 2026-05-09T20:43:19Z
- **Completed:** 2026-05-09T20:45:42Z
- **Tasks:** 2 (each with TDD RED + GREEN commits)
- **Files modified:** 4

## Accomplishments
- Created SourceAddCommand with --name (required), --path (required), --templates (flag) options
- Created SourceListCommand with --json flag for JSONL output and aligned columns for plain output
- 8 integration tests for SourceAddCommand covering all success and error paths (SRC-01, SRC-05, SRC-06, SRC-07)
- 6 integration tests for SourceListCommand covering empty/populated list, plain/JSON output, config-not-init error
- Full test suite (63 tests) passes with no regressions

## Task Commits

Each task was committed atomically with TDD RED then GREEN:

1. **Task 1 RED: SourceAddCommandTest (failing)** - `9183c48` (test)
2. **Task 1 GREEN: SourceAddCommand implementation** - `2ea1cba` (feat)
3. **Task 2 RED: SourceListCommandTest (failing)** - `de6dc46` (test)
4. **Task 2 GREEN: SourceListCommand implementation** - `24ca029` (feat)

_TDD tasks committed in RED then GREEN order — test file committed when class doesn't exist yet (compilation failure = RED confirmed)_

## Files Created/Modified
- `src/main/java/org/saltations/source/SourceAddCommand.java` - New: picocli Callable command for `vira source add`, delegates to SourceService.addSource()
- `src/main/java/org/saltations/source/SourceListCommand.java` - New: picocli Callable command for `vira source list`, delegates to SourceService.listSources() with aligned columns or JSONL
- `src/test/java/org/saltations/source/SourceAddCommandTest.java` - New: 8 integration tests using XdgPaths stub + @TempDir
- `src/test/java/org/saltations/source/SourceListCommandTest.java` - New: 6 integration tests using XdgPaths stub + @TempDir

## Decisions Made
- Validation stays entirely in SourceService — SourceAddCommand never re-checks path rules or name uniqueness (thin command pattern maintained)
- Empty list exits 0 with empty output (no header, no placeholder) — matches D-03 requirement
- JSONL uses a fresh `new ObjectMapper()` per command execution (consistent with SyncCommand pattern already in codebase)
- Column alignment computed with `maxNameWidth` via stream before printing — handles variable-length names cleanly

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- SourceAddCommand and SourceListCommand are ready for wiring into SourceCommand group (09-03 or final wiring plan)
- SourceService pattern proven for thin command integration — same approach applies directly to SourceShowCommand and SourceRemoveCommand
- No blockers.

---
*Phase: 09-source-commands*
*Completed: 2026-05-09*
