---
phase: 11-generate-rewrite
plan: "00"
subsystem: testing
tags: [junit5, picocli, freemarker, generate, test-scaffold]

# Dependency graph
requires:
  - phase: 10-destination-mapping-commands
    provides: DestinationService, SourceService, ConfigService, XdgPaths stub pattern
  - phase: 11-generate-rewrite
    provides: GeneratorService stub, GenerateCommand, PathExpander, GenerationResult

provides:
  - GeneratorServiceTest with 8 named test methods for GEN-01 through GEN-04
  - GenerateCommandTest with 6 named test methods for GEN-05 through GEN-07
  - src/test/resources/fixtures/sample.bin binary fixture with 9 known non-UTF8 bytes

affects: [11-01, 11-02]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Plain JUnit 5 with inline XdgPaths stub + @TempDir — no @MicronautTest for service unit tests"
    - "CommandLine.execute() test harness with ByteArrayOutputStream capture for command tests"

key-files:
  created:
    - src/test/java/org/saltations/generate/GeneratorServiceTest.java
    - src/test/java/org/saltations/generate/GenerateCommandTest.java
    - src/test/resources/fixtures/sample.bin
  modified: []

key-decisions:
  - "Used @Disabled on 7 of 8 GeneratorServiceTest methods and 5 of 6 GenerateCommandTest methods so Wave 0 compiles and passes while GeneratorService stub throws UnsupportedOperationException"
  - "generateCommandRequiresDestinationName test runs immediately (not @Disabled) since the null check is already wired in GenerateCommand.call()"
  - "Fixed picocli test harness: commandLine.execute() without 'generate' argument since CommandLine is rooted at GenerateCommand directly"
  - "Binary fixture sample.bin created with 9 bytes including 0x00, 0xFF, and PNG magic bytes to test non-UTF8 byte integrity"

patterns-established:
  - "Test service by direct instantiation: new GeneratorService(configService, pathExpander) — mirrors DestinationServiceTest pattern"
  - "Command test harness: new CommandLine(cmd) + setOut/setErr with ByteArrayOutputStream — mirrors DestinationAddCommandTest pattern"

requirements-completed: [GEN-01, GEN-02, GEN-03, GEN-04, GEN-05, GEN-06, GEN-07]

# Metrics
duration: 4min
completed: 2026-05-10
---

# Phase 11 Plan 00: Generate-Rewrite Wave 0 Summary

**Wave 0 test scaffold for Phase 11: 14 named test methods across GeneratorServiceTest and GenerateCommandTest covering GEN-01 through GEN-07, plus a 9-byte binary fixture for byte-integrity testing**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-10T19:54:25Z
- **Completed:** 2026-05-10T19:58:06Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Replaced GeneratorServiceTest placeholder stub with 8 named test methods covering flat copy, recursive walk, skip-existing, glob filter, hidden path skipping, template expansion, binary copy, and destination-not-found (GEN-01 through GEN-04)
- Replaced GenerateCommandTest placeholder stub with 6 named test methods covering destination-name routing, dry-run, verbose output, summary line, and unknown destination (GEN-05 through GEN-07)
- Created `src/test/resources/fixtures/sample.bin` with 9 known binary bytes including null byte (0x00), high byte (0xFF), and PNG magic bytes for byte-integrity tests
- Full Maven test suite remains green: 148 tests, 0 failures, 5 skipped (the @Disabled stubs)

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace GeneratorServiceTest stub with full scaffold** - `964cb76` (test)
2. **Task 2: Replace GenerateCommandTest stub with full scaffold, and create binary fixture** - `60e0c47` (test)

**Plan metadata:** (committed with final docs commit)

## Files Created/Modified

- `src/test/java/org/saltations/generate/GeneratorServiceTest.java` - 8 named test methods covering GEN-01 through GEN-04, uses plain JUnit 5 + XdgPaths stub + @TempDir
- `src/test/java/org/saltations/generate/GenerateCommandTest.java` - 6 named test methods covering GEN-05 through GEN-07, uses picocli CommandLine test harness with captured stdout/stderr
- `src/test/resources/fixtures/sample.bin` - 9-byte binary fixture with non-UTF8 content for GEN-04 byte-integrity tests

## Decisions Made

- Used `@Disabled("Wave 1: implement GeneratorService first")` on 7 of 8 service tests and `@Disabled("Wave 2: implement after Plan 01")` on 5 of 6 command tests so Wave 0 compiles and passes immediately
- The `generateCommandRequiresDestinationName` test was intentionally NOT disabled since the null check exists in `GenerateCommand.call()` already — it passes immediately
- Fixed picocli test harness issue: when wrapping `GenerateCommand` directly in `new CommandLine(cmd)`, `execute()` is called without "generate" argument (the command IS the root, not a subcommand)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed picocli test harness argument passing**

- **Found during:** Task 2 (GenerateCommandTest - generateCommandRequiresDestinationName)
- **Issue:** Plan specified `commandLine.execute("generate")` but when `new CommandLine(new GenerateCommand(...))` is the root, passing "generate" routes to a non-existent subcommand and fails with exit code 2 for wrong reason (unrecognized argument), not the missing-option check
- **Fix:** Changed to `commandLine.execute()` (no arguments) so picocli calls `GenerateCommand.call()` directly, which then checks for missing `--destination-name`
- **Files modified:** src/test/java/org/saltations/generate/GenerateCommandTest.java
- **Verification:** Test passes with exit code 2 and correct stderr message
- **Committed in:** `60e0c47` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in test harness setup)
**Impact on plan:** Fix required for the non-disabled test to pass correctly. No scope creep.

## Issues Encountered

- The Plan 01 agent ran concurrently and modified `GeneratorServiceTest.java` after Task 1's commit, removing `@Disabled` annotations and implementing real tests against the fully implemented `GeneratorService`. This is expected parallel execution behavior — Plan 01 builds on Plan 00's scaffold. The full test suite remains green with all 8 real tests passing.

## Next Phase Readiness

- Wave 0 scaffold is complete — Plan 01 (implement GeneratorService) can use these test scaffolds to drive TDD
- Wave 1 tests in `GeneratorServiceTest` are ready for Plan 01 to implement real assertions against
- Wave 2 tests in `GenerateCommandTest` are ready for Plan 02 to verify command integration after GeneratorService is complete
- Binary fixture at `src/test/resources/fixtures/sample.bin` is available for binary byte-integrity tests

---
*Phase: 11-generate-rewrite*
*Completed: 2026-05-10*
