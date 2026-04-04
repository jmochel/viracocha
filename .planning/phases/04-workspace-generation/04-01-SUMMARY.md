---
phase: 04-workspace-generation
plan: 01
subsystem: generate
tags: [freemarker, yaml, lombok]

requires:
  - phase: 03-projects-and-mappings
    provides: ProjectEntry, MappingEntry, YAML round-trip
provides:
  - Optional project-level Map<String, String> parameters on ProjectEntry
  - PathExpander.expandSegment for ${var} in path segments
affects:
  - 04-02 GeneratorService merge model
  - 04-03 generate command

tech-stack:
  added: []
  patterns:
    - Freemarker StringTemplateLoader per segment for path expansion

key-files:
  created:
    - src/main/java/org/saltations/generate/PathExpander.java
    - src/test/java/org/saltations/generate/PathExpanderTest.java
  modified:
    - src/main/java/org/saltations/model/ProjectEntry.java
    - src/main/java/org/saltations/project/CreateProjectCommand.java
    - src/test/java/org/saltations/model/ViracochaConfigProjectTypedListTest.java

key-decisions:
  - "Missing Freemarker variables in a segment throw IllegalArgumentException wrapping TemplateException"
  - "Legacy YAML without `parameters` under a project deserializes to an empty map"

patterns-established:
  - "Path segments processed as Freemarker templates with string map model"

requirements-completed: [GEN-02, GEN-03]

duration: 15min
completed: 2026-04-04
---

# Phase 4: Workspace Generation — Plan 01 Summary

**Project-level default parameters on `ProjectEntry` plus a Freemarker-backed `PathExpander` for path segments, with YAML round-trip and unit tests.**

## Performance

- **Duration:** ~15 min
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- `ProjectEntry` carries optional `parameters` (same style as mapping params); `CreateProjectCommand` seeds an empty map.
- `PathExpander.expandSegment` uses Freemarker 2.3.34 with strict template exception handling for failures.
- Tests cover YAML without `parameters`, missing variable errors, and multi-variable segments.

## Task Commits

1. **Task 1: ProjectEntry parameters** — `c233290` (feat)
2. **Task 2: PathExpander** — `f350435` (feat)

## Files Created/Modified

- `src/main/java/org/saltations/generate/PathExpander.java` — segment expansion
- `src/test/java/org/saltations/generate/PathExpanderTest.java` — unit tests
- `src/main/java/org/saltations/model/ProjectEntry.java` — `parameters` field
- `src/main/java/org/saltations/project/CreateProjectCommand.java` — constructor call
- `src/test/java/org/saltations/model/ViracochaConfigProjectTypedListTest.java` — round-trip and legacy YAML

## Decisions Made

- Followed plan: Freemarker `RETHROW_HANDLER` for clear failures; wrap in `IllegalArgumentException` with segment text.

## Deviations from Plan

None — plan executed as written.

## Issues Encountered

None.

## Next Phase Readiness

- PathExpander and project defaults are ready for `GeneratorService` merge logic in 04-02.

---
*Phase: 04-workspace-generation*
*Completed: 2026-04-04*
