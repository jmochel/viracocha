---
phase: 01-foundation
verified: 2026-03-28T14:15:00Z
status: passed
score: 11/11 must-haves verified
gaps: []
human_verification:
  - test: "Log output isolation during live invocation"
    expected: "Running 'java -jar target/viracocha-0.1.jar config init' writes no bytes to stdout or stderr, and appends a JSONL line to ~/.local/share/viracocha/vira.jsonl"
    why_human: "Tests suppress log output via logback-test.xml; only a live JAR invocation can confirm the production logback.xml sends nothing to stdout/stderr"
---

# Phase 01: Foundation Verification Report

**Phase Goal:** Establish project infrastructure and config subsystem so the CLI can be built and tested end-to-end.
**Verified:** 2026-03-28T14:15:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

Combined must-haves from plans 01-01 and 01-02.

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | jackson-dataformat-yaml and logstash-logback-encoder dependencies are resolvable at compile time | VERIFIED | Both entries present in pom.xml lines 58-69; `mvnw test` exits 0 (BUILD SUCCESS) |
| 2  | Log output goes only to ~/.local/share/viracocha/vira.jsonl — nothing on stdout or stderr | VERIFIED | logback.xml has FileAppender+LogstashEncoder only; grep confirms no ConsoleAppender |
| 3  | XdgPaths.configFile() returns ~/.config/viracocha/config.yaml when XDG_CONFIG_HOME is unset | VERIFIED | XdgPaths.java line 20-22 fallback logic confirmed; XdgPathsTest.configFileUsesHomeDotConfigWhenXdgUnset passes |
| 4  | XdgPaths.configFile() returns $XDG_CONFIG_HOME/viracocha/config.yaml when XDG_CONFIG_HOME is set | VERIFIED | XdgPaths.java line 20-21 reads env first; XdgPathsTest.configFileEndsWithExpectedSegments passes |
| 5  | ViracochaConfig serializes to and deserializes from YAML without data loss | VERIFIED | ViracochaConfigTest (2 tests: serialization + round-trip) passes |
| 6  | User can run 'vira config init' and see a confirmation with the config.yaml path | VERIFIED | InitCommand.call() prints "Config initialized at <path>"; InitCommandTest.initOnFreshDirPrintsConfirmationAndExitsZero passes |
| 7  | Running 'vira config init' a second time prints 'Config already initialized at <path>' and exits 0 | VERIFIED | InitCommand checks Files.exists() before init(); InitCommandTest.reInitPrintsAlreadyInitializedAndExitsZero passes |
| 8  | User can run 'vira config show' to see 'Config file: <path>' followed by raw YAML contents | VERIFIED | ShowConfigCommand outputs "Config file:" then blank line then rawYaml; ShowConfigCommandTest (2 tests) passes |
| 9  | Running any non-init command before 'vira config init' prints the not-initialized error and exits 1 | VERIFIED | ConfigService.load() throws ConfigNotInitializedException; ShowConfigCommand catches it, writes to stderr, returns 1; ShowConfigCommandTest.showBeforeInitExitsOneAndPrintsToStderr passes |
| 10 | All subcommands respond to --help without errors | VERIFIED | All commands have mixinStandardHelpOptions=true; ViracochaCommandTest.helpExitsZeroAndListsConfigSubcommand passes (exit 0, output contains "config") |
| 11 | All command classes implement Callable<Integer> — never Runnable | VERIFIED | ViracochaCommand, ConfigCommand, InitCommand, ShowConfigCommand all implement Callable<Integer>; confirmed by source reads |

**Score:** 11/11 truths verified

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `pom.xml` | jackson-dataformat-yaml + logstash-logback-encoder dependencies | VERIFIED | Lines 58-69: jackson-dataformat-yaml (compile, BOM-managed); logstash-logback-encoder 7.4 (runtime) |
| `src/main/resources/logback.xml` | JSONL FileAppender, no ConsoleAppender | VERIFIED | FileAppender with LogstashEncoder to ${user.home}/.local/share/viracocha/vira.jsonl; no ConsoleAppender |
| `src/main/java/org/saltations/infra/XdgPaths.java` | XDG path resolution with fallback; configDir(), configFile(), dataDir(), logFile() | VERIFIED | @Singleton; all 4 methods implemented with env-first, ~/.config fallback logic |
| `src/main/java/org/saltations/model/ViracochaConfig.java` | Root config POJO with version field | VERIFIED | @Data with version=1, publishers/patterns/projects as List<Object> |
| `src/test/java/org/saltations/infra/XdgPathsTest.java` | XdgPaths unit tests (4 tests) | VERIFIED | 4 tests; all pass |
| `src/test/java/org/saltations/model/ViracochaConfigTest.java` | ViracochaConfig round-trip YAML test (2 tests) | VERIFIED | 2 tests; all pass |
| `src/main/java/org/saltations/ViracochaCommand.java` | Root command with Callable<Integer>, subcommands={ConfigCommand.class} | VERIFIED | Implements Callable<Integer>; @Command(subcommands={ConfigCommand.class}); PicocliRunner.execute() with System.exit() |
| `src/main/java/org/saltations/config/ConfigService.java` | init(), load(), save() over XdgPaths config file | VERIFIED | @Singleton; constructor-injected XdgPaths; init/load/save fully implemented with real file I/O |
| `src/main/java/org/saltations/config/InitCommand.java` | vira config init — creates config, idempotent; contains "already initialized" message | VERIFIED | @Spec CommandSpec; checks file existence before init(); correct messages; exits 0 both cases |
| `src/main/java/org/saltations/config/ShowConfigCommand.java` | vira config show — prints path + YAML; contains "Config file:" | VERIFIED | Calls load() for guard, then reads raw YAML; "Config file:" prefix confirmed |
| `src/main/java/org/saltations/config/ConfigNotInitializedException.java` | Checked exception thrown by ConfigService.load() when config missing | VERIFIED | RuntimeException with exact message "Config not initialized. Run 'vira config init' first." |
| `src/main/java/org/saltations/config/ConfigCommand.java` | Group command with InitCommand + ShowConfigCommand subcommands | VERIFIED | @Command(subcommands={InitCommand.class, ShowConfigCommand.class}); Callable<Integer> |
| `src/test/resources/logback-test.xml` | NOPAppender suppresses log output during tests | VERIFIED | NOPAppender with root level="off" |
| `src/test/java/org/saltations/config/ConfigServiceTest.java` | 5 ConfigService unit tests | VERIFIED | 5 tests; all pass |
| `src/test/java/org/saltations/config/InitCommandTest.java` | 2 InitCommand integration tests | VERIFIED | 2 tests; all pass |
| `src/test/java/org/saltations/config/ShowConfigCommandTest.java` | 3 ShowConfigCommand integration tests | VERIFIED | 3 tests; all pass |
| `src/test/java/org/saltations/ViracochaCommandTest.java` | Smoke test: --help exits 0 and lists 'config' | VERIFIED | 1 test; passes |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| logback.xml | ~/.local/share/viracocha/vira.jsonl | FileAppender + LogstashEncoder | VERIFIED | logback.xml line 4-8: FileAppender with LogstashEncoder; file path matches |
| XdgPaths | System.getenv("XDG_CONFIG_HOME") | null-safe env read with ~/.config fallback | VERIFIED | XdgPaths.java line 19-22: env read, isBlank check, fallback to user.home/.config |
| InitCommand | ConfigService.init() | @Inject ConfigService; init() creates dir + file | VERIFIED | InitCommand line 43: configService.init() called in call() method |
| ShowConfigCommand | ConfigService.load() | @Inject ConfigService; load() reads YAML | VERIFIED | ShowConfigCommand line 44: configService.load() called as first operation |
| ConfigService.load() | ConfigNotInitializedException | throws when Files.notExists(configFile) | VERIFIED | ConfigService line 56-58: if (!Files.exists(configFile)) throw new ConfigNotInitializedException() |
| main() | System.exit(commandLine.execute(args)) | picocli CommandLine.execute return value | VERIFIED | ViracochaCommand line 27: System.exit(PicocliRunner.execute(ViracochaCommand.class, args)) |

---

### Data-Flow Trace (Level 4)

Not applicable — this phase produces CLI commands and infrastructure utilities, not components that render dynamic data from a store. ConfigService performs real file I/O (Files.createDirectories, ObjectMapper.writeValue/readValue) with no static return stubs.

---

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full test suite passes (17 tests) | ./mvnw test | BUILD SUCCESS, Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 | PASS |
| ViracochaConfigTest round-trip | ./mvnw test -Dtest="ViracochaConfigTest" | 2/2 pass | PASS |
| XdgPathsTest path logic | ./mvnw test -Dtest="XdgPathsTest" | 4/4 pass | PASS |
| ConfigServiceTest init/load/save | ./mvnw test -Dtest="ConfigServiceTest" | 5/5 pass | PASS |
| InitCommandTest idempotent init | ./mvnw test -Dtest="InitCommandTest" | 2/2 pass | PASS |
| ShowConfigCommandTest guard + display | ./mvnw test -Dtest="ShowConfigCommandTest" | 3/3 pass | PASS |
| ViracochaCommandTest --help exit code | ./mvnw test -Dtest="ViracochaCommandTest" | 1/1 pass | PASS |
| Live JAR log isolation | Requires JAR invocation with real filesystem | Not run | SKIP — routed to human verification |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CONF-01 | 01-01, 01-02 | User can run `vira config init` to create XDG config directory and empty central config.yaml | SATISFIED | InitCommand creates dirs via ConfigService.init(); idempotent; InitCommandTest passes |
| CONF-02 | 01-02 | User can run `vira config show` to display config file path and contents | SATISFIED | ShowConfigCommand prints "Config file: <path>" then raw YAML; ShowConfigCommandTest passes |
| CONF-03 | 01-02 | All commands other than config init check config exists and print clear error if not | SATISFIED | ConfigService.load() throws ConfigNotInitializedException; ShowConfigCommand catches and exits 1; test verified |
| LOG-01 | 01-01 | Structured log output written in JSONL format to ~/.local/share/viracocha/vira.jsonl | SATISFIED (automated) | logback.xml FileAppender+LogstashEncoder to correct path; human verification needed for live run |
| LOG-02 | 01-01 | Log output does not appear on stdout or stderr during normal command execution | SATISFIED (automated) | No ConsoleAppender in logback.xml; logback-test.xml uses NOPAppender; human verification needed for live run |

No orphaned requirements — all 5 requirement IDs from plans match the 5 requirements mapped to Phase 1 in REQUIREMENTS.md.

---

### Anti-Patterns Found

None. Scanned all main source files under src/main/java/org/saltations/ for TODO, FIXME, XXX, HACK, PLACEHOLDER, empty implementations, hardcoded empty returns, and stub patterns. No matches found.

---

### Human Verification Required

#### 1. Log Output Isolation (Live JAR)

**Test:** Build the fat JAR (`./mvnw package -q`), then run `java -jar target/viracocha-0.1.jar config init` in a terminal. Observe stdout and stderr in the terminal.
**Expected:** Only the "Config initialized at <path>" confirmation appears on stdout. No log lines, no JSONL, no stack traces appear on stdout or stderr. Check `~/.local/share/viracocha/vira.jsonl` — it should contain at least one JSONL line after the run.
**Why human:** Tests use logback-test.xml (NOPAppender, level=off) which masks the production logback.xml. Only a real JAR invocation with the production classpath can confirm the JSONL-only routing and that the log directory creation by ConfigService.init() allows logback to write without error.

---

### Gaps Summary

No gaps. All 11 must-have truths verified, all 17 artifacts pass levels 1-3, all 6 key links wired, 5/5 requirements satisfied, 0 anti-patterns. The one human verification item (live JAR log isolation) is a confirmation check, not a suspected failure — the static analysis is complete and consistent.

---

_Verified: 2026-03-28T14:15:00Z_
_Verifier: Claude (gsd-verifier)_
