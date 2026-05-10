---
phase: 11-generate-rewrite
plan: "02"
subsystem: generate
tags: [picocli, freemarker, generate, cli, tdd]

requires:
  - phase: 11-01
    provides: GeneratorService with traversal engine, GenerationResult, PathExpander, GenerateCommand scaffold

provides:
  - GeneratorService 5-arg overload with interactive destination-creation prompt (D-05 through D-09)
  - GenerateCommand wired to pass spec.commandLine().getOut() and System.in to service
  - GenerateCommandTest fully enabled with 6 passing tests (GEN-05, GEN-06, GEN-07)
  - Complete end-to-end vira generate behavior all seven GEN requirements satisfied and tested

affects: [vira-generate-integration, phase-12-if-any]

tech-stack:
  added: []
  patterns:
    - "5-arg method overload for testability: keep 3-arg convenience, add InputStream/PrintWriter params"
    - "picocli test harness: CommandLine rooted at leaf command — execute() args are OPTIONS not subcommand names"
    - "Dry-run direct output: print Would-create lines to PrintWriter in dry-run, not just verbose-lines"

key-files:
  created: []
  modified:
    - src/main/java/org/saltations/generate/GeneratorService.java
    - src/main/java/org/saltations/generate/GenerateCommand.java
    - src/test/java/org/saltations/generate/GenerateCommandTest.java

key-decisions:
  - "5-arg generate() overload keeps 3-arg for backward compat; service tests continue to use 3-arg signature"
  - "Dry-run should always print Would-create lines (GEN-06), not only when --verbose is set"
  - "CommandLine in tests is rooted at GenerateCommand directly — execute() args are options not subcommand names"

patterns-established:
  - "Interactive-prompt testability: pass PrintWriter and InputStream as method params, not constructor params"
  - "Picocli leaf-command tests: build CommandLine from the command class, pass options without command name"

requirements-completed: [GEN-05, GEN-06, GEN-07]

duration: 4min
completed: 2026-05-10
---

# Phase 11 Plan 02: Generate Command Integration Summary

**Interactive destination-creation prompt, 5-arg GeneratorService overload, and 6/6 GenerateCommandTest passing for GEN-05 through GEN-07**

## Performance

- **Duration:** 4 min
- **Started:** 2026-05-10T20:01:55Z
- **Completed:** 2026-05-10T20:06:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Added `generate(String, boolean, boolean, PrintWriter, InputStream)` overload with full D-05 through D-09 interactive prompt (flush before readLine, y/Y confirm, exit 0 on decline, dry-run skips prompt and prints "Would create: <path>")
- Updated `GenerateCommand.call()` to pass `spec.commandLine().getOut()` and `System.in` to 5-arg service overload
- Removed all `@Disabled` annotations from `GenerateCommandTest` and fixed execute() call pattern; all 6 tests pass
- Full test suite green: 148 tests, 0 failures, 0 errors

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend GeneratorService.generate() with interactive destination-creation prompt** - `d9d39d2` (feat)
2. **Task 2: Update GenerateCommand to pass output writer and stdin to service** - `d0d532e` (feat)
3. **Task 3: Enable and pass GenerateCommandTest — GEN-05, GEN-06, GEN-07** - `1903873` (test)

## Files Created/Modified

- `src/main/java/org/saltations/generate/GeneratorService.java` - Added 5-arg overload with interactive destination prompt; 3-arg delegates; dry-run prints "Would create" to PrintWriter directly
- `src/main/java/org/saltations/generate/GenerateCommand.java` - Wires spec.commandLine().getOut() and System.in to 5-arg service call; adds PrintWriter import
- `src/test/java/org/saltations/generate/GenerateCommandTest.java` - Remove @Disabled from 5 tests; fix execute() arg pattern; 6/6 tests passing

## Decisions Made

- Dry-run should print "Would create" per-file lines directly to `out` (GEN-06) without requiring `--verbose`. The existing implementation only added lines to `verboseLines` (which require `--verbose` to appear). Fixed as Rule 1 auto-fix.
- `CommandLine` in tests is built from the leaf `GenerateCommand` (not `ViracochaCommand`), so `execute()` args are options not subcommand names. Plan scaffolded tests with `execute("generate", "--destination-name", ...)` which would fail — fixed to `execute("--destination-name", ...)`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Dry-run "Would create" only appeared with --verbose**
- **Found during:** Task 3 (GenerateCommandTest dry-run test)
- **Issue:** `generate()` added "Would create" lines only to `verboseLines`, which are only printed when `verbose=true`. GEN-06 requires dry-run to always report "Would create" lines without requiring `--verbose`.
- **Fix:** Added `out.println("Would create " + destPath)` directly to the dry-run branch in `GeneratorService.generate()` so it always outputs regardless of `verbose` flag.
- **Files modified:** `src/main/java/org/saltations/generate/GeneratorService.java`
- **Verification:** `generateCommandDryRunReportsActionsWithoutWriting` passes; GeneratorServiceTest still passes (8/8)
- **Committed in:** `1903873` (Task 3 commit, with test file)

**2. [Rule 1 - Bug] Test execute() calls passed "generate" as first arg to leaf CommandLine**
- **Found during:** Task 3 (analysis of test execute patterns)
- **Issue:** Plan scaffolded `commandLine.execute("generate", "--destination-name", ...)` but `commandLine` is rooted at `GenerateCommand` directly. Passing "generate" would be treated as an unmatched argument, causing exit 2.
- **Fix:** Changed all execute() calls in GenerateCommandTest to omit "generate" prefix: `commandLine.execute("--destination-name", ...)`.
- **Files modified:** `src/test/java/org/saltations/generate/GenerateCommandTest.java`
- **Verification:** All 6 GenerateCommandTest tests pass
- **Committed in:** `1903873` (Task 3 commit)

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both fixes necessary for correct GEN-06 behavior and working tests. No scope creep.

## Issues Encountered

None beyond the auto-fixed deviations above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All seven GEN requirements (GEN-01 through GEN-07) are now implemented and tested
- `vira generate --destination-name <name>` works end-to-end: routing, dry-run, verbose, skip-existing, interactive destination-creation prompt
- Phase 11 generate-rewrite is complete (Plans 00, 01, 02 all done)
- Ready for next milestone or feature phase

---
*Phase: 11-generate-rewrite*
*Completed: 2026-05-10*
