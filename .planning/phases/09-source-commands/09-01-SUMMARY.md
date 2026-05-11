---
phase: 09-source-commands
plan: "01"
subsystem: infra
tags: [micronaut, freemarker, source-service, dependency-injection, validation]

# Dependency graph
requires:
  - phase: 08-model-config-foundation
    provides: ConfigService, ViracochaConfig, SourceEntry model classes
provides:
  - FreemarkerVariableExtractor as injectable @Singleton bean
  - SourceService with addSource/listSources/getSource/removeSource
  - Path validation (traversal, existence, directory type, duplicate name)
  - Freemarker variable extraction on template sources
affects: [10-destination-commands, 11-generate-command, source CLI command wiring]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Service layer pattern: commands are thin, all business rules live in *Service classes"
    - "TDD red-green: write failing tests first, then implement to make them pass"
    - "XdgPaths stub pattern: inline anonymous subclass with @TempDir for isolated unit tests"
    - "Load-fresh pattern: configService.load() called per operation, never cached"

key-files:
  created:
    - src/main/java/org/saltations/source/SourceService.java
    - src/test/java/org/saltations/source/SourceServiceTest.java
  modified:
    - src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java

key-decisions:
  - "Raw-string traversal check: rawPath.contains('..') before Path.of() to prevent normalization bypass (D-13)"
  - "Store absolute normalized path in SourceEntry.path to ensure consistent lookup across working directories"
  - "templates=false results in empty parameters list, not null, for consistent YAML serialization"

patterns-established:
  - "Service validation order: traversal check -> existence -> directory type -> duplicate name -> business logic"
  - "XdgPaths stub for isolation: override configFile/configDir/dataDir in anonymous subclass inside @BeforeEach"

requirements-completed: [SRC-05, SRC-06, SRC-07]

# Metrics
duration: 2min
completed: 2026-05-09
---

# Phase 09, Plan 01: Source Service Summary

**SourceService with path validation and Freemarker extraction wired to ConfigService via injectable @Singleton FreemarkerVariableExtractor**

## Performance

- **Duration:** 2 min
- **Started:** 2026-05-09T20:39:19Z
- **Completed:** 2026-05-09T20:41:29Z
- **Tasks:** 2 (TDD: RED + GREEN)
- **Files modified:** 3

## Accomplishments
- Added `@Singleton` to `FreemarkerVariableExtractor` making it injectable by Micronaut DI
- Created `SourceService` with complete source CRUD: addSource, listSources, getSource, removeSource
- Path traversal check on raw string before `Path.of()` prevents normalization bypass (D-13)
- All 12 behavioral tests pass covering SRC-05, SRC-06, SRC-07, and round-trip operations

## Task Commits

Each task was committed atomically:

1. **Task 2: SourceServiceTest (RED)** - `c5658bb` (test)
2. **Task 1+2: FreemarkerVariableExtractor @Singleton + SourceService (GREEN)** - `8b07d84` (feat)

_Note: TDD tasks committed in RED then GREEN order — test file committed first when class didn't exist._

## Files Created/Modified
- `src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java` - Added `@Singleton` annotation and `jakarta.inject.Singleton` import
- `src/main/java/org/saltations/source/SourceService.java` - New: business logic for source CRUD with path validation
- `src/test/java/org/saltations/source/SourceServiceTest.java` - New: 12 unit tests using XdgPaths stub + @TempDir isolation

## Decisions Made
- Raw-string traversal check (`rawPath.contains("..")`) must happen before `Path.of()` because `Path.of("/tmp/../etc").normalize()` produces `/etc`, silently defeating the check
- SourceEntry stores the absolute normalized path string so callers always get a canonical path regardless of how the original was specified
- `templates=false` paths produce an empty `ArrayList` (not null) for `parameters` to ensure clean YAML serialization

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- SourceService is ready for CLI command wiring in Phase 09 Plans 02-04 (AddSourceCommand, ListSourcesCommand, ShowSourceCommand, RemoveSourceCommand)
- FreemarkerVariableExtractor is now injectable so SourceService can be wired via Micronaut DI in the full application context
- No blockers.

---
*Phase: 09-source-commands*
*Completed: 2026-05-09*
