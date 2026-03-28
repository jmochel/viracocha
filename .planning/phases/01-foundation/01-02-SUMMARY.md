---
phase: 01-foundation
plan: "02"
subsystem: config
tags: [picocli, micronaut, jackson-dataformat-yaml, xdg, cli, config]

requires:
  - phase: 01-foundation/01-01
    provides: XdgPaths singleton, ViracochaConfig POJO, project skeleton with Micronaut DI

provides:
  - ConfigService with init(), load(), save() over XDG config file
  - ConfigNotInitializedException thrown by load() when config missing
  - ViracochaCommand rewritten as Callable<Integer> with ConfigCommand subcommand
  - ConfigCommand group command with InitCommand and ShowConfigCommand
  - InitCommand: vira config init (idempotent, prints confirmation path)
  - ShowConfigCommand: vira config show (prints path + raw YAML, exits 1 if not initialized)

affects: [02-publishers, 03-patterns, 04-generate, any phase that reads/writes central config]

tech-stack:
  added: []
  patterns:
    - "Commands implement Callable<Integer> (not Runnable) to propagate exit codes via picocli execute()"
    - "CommandSpec @Spec injection for testable output via setOut()/setErr() PrintWriter"
    - "ConfigService injected into commands via @Inject constructor injection"
    - "TempDir-backed XdgPaths anonymous subclass for test isolation without real filesystem side effects"

key-files:
  created:
    - src/main/java/org/saltations/config/ConfigService.java
    - src/main/java/org/saltations/config/ConfigNotInitializedException.java
    - src/main/java/org/saltations/config/ConfigCommand.java
    - src/main/java/org/saltations/config/InitCommand.java
    - src/main/java/org/saltations/config/ShowConfigCommand.java
    - src/test/java/org/saltations/config/ConfigServiceTest.java
    - src/test/java/org/saltations/config/InitCommandTest.java
    - src/test/java/org/saltations/config/ShowConfigCommandTest.java
  modified:
    - src/main/java/org/saltations/ViracochaCommand.java
    - src/test/java/org/saltations/ViracochaCommandTest.java

key-decisions:
  - "Use picocli @Spec CommandSpec to write output via spec.commandLine().getOut()/getErr() instead of System.out/err — required for test capture via CommandLine.setOut(PrintWriter)"
  - "InitCommand checks file existence before calling configService.init() to distinguish new vs already-initialized cases for correct messaging"
  - "ShowConfigCommand calls configService.load() first (throws ConfigNotInitializedException if missing) then reads raw YAML bytes for display — avoids double deserialization"
  - "All command classes annotated @Singleton for Micronaut DI wiring through MicronautFactory"

patterns-established:
  - "Callable<Integer> pattern: all command classes implement Callable<Integer> with return codes 0=success, 1=error"
  - "Picocli output pattern: use @Spec CommandSpec + spec.commandLine().getOut()/getErr() for testable output"
  - "Test isolation pattern: anonymous XdgPaths subclass overriding configFile/configDir/dataDir to @TempDir for filesystem isolation"
  - "TDD pattern: test file written first (fails compilation), then implementation written to pass"

requirements-completed: [CONF-01, CONF-02, CONF-03]

duration: 5min
completed: "2026-03-28"
---

# Phase 01 Plan 02: Config Subsystem Summary

**ConfigService (init/load/save over XDG YAML), full picocli command hierarchy (vira -> config -> init|show), and 17 passing tests using @Spec-based testable output**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-28T14:04:40Z
- **Completed:** 2026-03-28T14:09:19Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- ConfigService @Singleton with init() (creates XDG dirs + default YAML), load() (throws ConfigNotInitializedException if missing), save() round-trip via jackson-dataformat-yaml
- Full command hierarchy wired: ViracochaCommand (Callable) -> ConfigCommand (group) -> InitCommand + ShowConfigCommand
- 17 tests passing: ConfigServiceTest (5), InitCommandTest (2), ShowConfigCommandTest (3), ViracochaCommandTest (1), plus 4 XdgPathsTest and 2 ViracochaModelTest from prior plan

## Task Commits

Each task was committed atomically:

1. **Task 1: ConfigService, ConfigNotInitializedException, unit tests** - `59f9f5b` (feat)
2. **Task 2: Full command hierarchy and integration tests** - `16ee43c` (feat)

**Plan metadata:** (docs commit - pending)

_Note: TDD tasks had RED (compilation failure) verified before GREEN implementation_

## Files Created/Modified

- `src/main/java/org/saltations/config/ConfigService.java` - @Singleton managing config.yaml: init/load/save
- `src/main/java/org/saltations/config/ConfigNotInitializedException.java` - RuntimeException thrown by load() when config absent
- `src/main/java/org/saltations/config/ConfigCommand.java` - Group command: vira config
- `src/main/java/org/saltations/config/InitCommand.java` - vira config init (idempotent, prints path)
- `src/main/java/org/saltations/config/ShowConfigCommand.java` - vira config show (prints path + YAML)
- `src/main/java/org/saltations/ViracochaCommand.java` - Rewritten: Callable<Integer>, subcommands={ConfigCommand}
- `src/test/java/org/saltations/config/ConfigServiceTest.java` - 5 unit tests for ConfigService
- `src/test/java/org/saltations/config/InitCommandTest.java` - 2 integration tests for InitCommand
- `src/test/java/org/saltations/config/ShowConfigCommandTest.java` - 3 integration tests for ShowConfigCommand
- `src/test/java/org/saltations/ViracochaCommandTest.java` - Smoke test: --help exits 0 and lists 'config'

## Decisions Made

- Use `@Spec CommandSpec` + `spec.commandLine().getOut()/getErr()` for picocli output — required so tests using `CommandLine.setOut(PrintWriter)` can capture command output (System.out is not redirected by picocli's setOut)
- `InitCommand` checks `Files.exists(configFile)` before calling `configService.init()` to determine whether the "already initialized" or "initialized" message should print
- `ShowConfigCommand` uses `configService.load()` to get the not-initialized check for free, then re-reads raw file bytes to print actual YAML without re-serializing

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed PrintStream/PrintWriter type mismatch in tests**
- **Found during:** Task 2 (writing InitCommandTest, ShowConfigCommandTest, ViracochaCommandTest)
- **Issue:** Plan template used `commandLine.setOut(new PrintStream(stdout))` but picocli's `CommandLine.setOut()` accepts `PrintWriter`, not `PrintStream` — compilation error
- **Fix:** Changed all test output capture to `new PrintWriter(stdout, true)` with autoFlush=true
- **Files modified:** InitCommandTest.java, ShowConfigCommandTest.java, ViracochaCommandTest.java
- **Verification:** Compilation succeeds, tests pass
- **Committed in:** `16ee43c` (Task 2 commit)

**2. [Rule 1 - Bug] Fixed missing flush() causing YAML content not written to test output**
- **Found during:** Task 2 (ShowConfigCommandTest.showAfterInitContainsYamlVersion)
- **Issue:** Plan template used `System.out.print(rawYaml)` but after switching to picocli PrintWriter, `print()` without newline doesn't trigger autoFlush — YAML content never appeared in captured output
- **Fix:** Changed to `println(rawYaml)` + explicit `flush()` in ShowConfigCommand
- **Files modified:** ShowConfigCommand.java
- **Verification:** Debug test showed output with YAML; full suite passes
- **Committed in:** `16ee43c` (Task 2 commit)

**3. [Rule 1 - Bug] Commands use @Spec CommandSpec instead of System.out/err**
- **Found during:** Task 2 (design review before implementation)
- **Issue:** Plan template commands used `System.out.println()` / `System.err.println()` — these cannot be captured by picocli's `CommandLine.setOut/setErr`, breaking all output tests
- **Fix:** Added `@Spec CommandSpec spec` to InitCommand and ShowConfigCommand; all output via `spec.commandLine().getOut()/getErr()`
- **Files modified:** InitCommand.java, ShowConfigCommand.java
- **Verification:** All output tests pass
- **Committed in:** `16ee43c` (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (all Rule 1 - Bug)
**Impact on plan:** All auto-fixes required for test correctness. No scope creep. Plan design used System.out but testable picocli output requires @Spec injection pattern.

## Issues Encountered

None beyond the auto-fixed bugs above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Config subsystem fully operational: init, show, ConfigNotInitializedException guard all working
- All 17 tests passing — build is green
- CONF-01, CONF-02, CONF-03 requirements satisfied
- Phase 01-foundation complete — Phase 02 (publisher management) can begin
- Pattern established: `@Spec CommandSpec` for testable command output must be followed by all future command classes

---
*Phase: 01-foundation*
*Completed: 2026-03-28*
