---
phase: 01-foundation
plan: "01"
subsystem: infra
tags: [jackson-dataformat-yaml, logstash-logback-encoder, logback, xdg, lombok, micronaut]

# Dependency graph
requires: []
provides:
  - jackson-dataformat-yaml dependency on compile classpath
  - logstash-logback-encoder 7.4 on runtime classpath
  - JSONL FileAppender logback config (no stdout/stderr logging)
  - XdgPaths @Singleton resolving config/data/log paths per XDG spec
  - ViracochaConfig @Data POJO for YAML round-trip serialization
affects:
  - 01-02 (ConfigService depends on XdgPaths and ViracochaConfig)
  - All future phases (log output format and config path resolution)

# Tech tracking
tech-stack:
  added:
    - jackson-dataformat-yaml (BOM-managed, compile scope) — YAML ObjectMapper for config files
    - logstash-logback-encoder 7.4 (runtime scope) — JSONL structured log encoding
  patterns:
    - XDG Base Directory Specification for config/data path resolution
    - TDD (RED → GREEN) for infrastructure code
    - logback-test.xml with NOPAppender to suppress log noise during tests
    - @Singleton utility class for DI-injectable path resolution

key-files:
  created:
    - src/main/java/org/saltations/infra/XdgPaths.java
    - src/main/java/org/saltations/model/ViracochaConfig.java
    - src/test/java/org/saltations/infra/XdgPathsTest.java
    - src/test/java/org/saltations/model/ViracochaConfigTest.java
    - src/test/resources/logback-test.xml
  modified:
    - pom.xml
    - src/main/resources/logback.xml

key-decisions:
  - "logstash-logback-encoder must be pinned at 7.4 (not BOM-managed) — Micronaut BOM does not include it"
  - "ViracochaConfig uses List<Object> for publishers/patterns/projects in Phase 1 — typed entries added in Phase 2"
  - "logback.xml uses ${user.home} property (not XDG path) for log file — XdgPaths.logFile() used at runtime by ConfigService to ensure dir exists"

patterns-established:
  - "XDG path resolution: check env var first, fall back to ~/.config or ~/.local/share"
  - "Test isolation: logback-test.xml with NOPAppender prevents log file creation during tests"
  - "Config POJO pattern: @Data with default field values for safe deserialization"

requirements-completed: [CONF-01, LOG-01, LOG-02]

# Metrics
duration: 2min
completed: "2026-03-28"
---

# Phase 01 Plan 01: Project Infrastructure Summary

**jackson-dataformat-yaml + logstash-logback-encoder wired to pom.xml, JSONL-only logback, XdgPaths XDG utility, and ViracochaConfig round-trip YAML POJO — 6 tests pass**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-28T14:00:11Z
- **Completed:** 2026-03-28T14:02:09Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added two missing Maven dependencies (jackson-dataformat-yaml BOM-managed, logstash-logback-encoder 7.4) and confirmed compile succeeds
- Replaced ConsoleAppender logback configuration with JSONL FileAppender to `~/.local/share/viracocha/vira.jsonl` — no stdout/stderr log output
- Implemented XdgPaths @Singleton resolving XDG-compliant config/data/log paths with fallback to ~/.config and ~/.local/share
- Implemented ViracochaConfig @Data POJO serializing to and round-tripping from YAML correctly
- Created logback-test.xml with NOPAppender to suppress log output and prevent log file creation during tests
- All 6 unit tests pass (4 XdgPathsTest + 2 ViracochaConfigTest)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add missing pom.xml dependencies and replace logback.xml** - `3fc1edb` (feat)
2. **Task 2: Implement XdgPaths utility and ViracochaConfig model with tests** - `b3188f9` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified

- `pom.xml` — Added jackson-dataformat-yaml (compile) and logstash-logback-encoder 7.4 (runtime) after logback-classic
- `src/main/resources/logback.xml` — Replaced ConsoleAppender with JSONL FileAppender using LogstashEncoder
- `src/main/java/org/saltations/infra/XdgPaths.java` — @Singleton XDG path resolver with 4 public methods
- `src/main/java/org/saltations/model/ViracochaConfig.java` — @Data POJO with version, publishers, patterns, projects
- `src/test/java/org/saltations/infra/XdgPathsTest.java` — 4 unit tests for path segment and fallback behavior
- `src/test/java/org/saltations/model/ViracochaConfigTest.java` — 2 unit tests for YAML serialization and round-trip
- `src/test/resources/logback-test.xml` — NOPAppender root suppresses all log output in test JVM

## Decisions Made

- logstash-logback-encoder pinned at version 7.4 — the Micronaut BOM does not include it, so an explicit version is required
- ViracochaConfig uses `List<Object>` for the three list fields in Phase 1 — typed domain entries (PublisherEntry, PatternEntry, ProjectEntry) will be added in Phase 2 when those models are defined
- logback.xml uses `${user.home}` system property (not XdgPaths) for the file path because logback resolves paths at startup before Micronaut DI is initialized; XdgPaths.logFile() is used by ConfigService.init() to ensure the directory exists

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 02 (ConfigService) can now import XdgPaths and ViracochaConfig without compilation errors
- XdgPaths resolves config file path; ConfigService can use it to load/save YAML config
- ViracochaConfig serializes/deserializes correctly; ConfigService can use ObjectMapper(YAMLFactory) pattern established in ViracochaConfigTest
- Log directory (`~/.local/share/viracocha/`) must exist before first log write — ConfigService.init() should call Files.createDirectories(xdgPaths.dataDir())

---
*Phase: 01-foundation*
*Completed: 2026-03-28*

## Self-Check: PASSED

- FOUND: pom.xml
- FOUND: logback.xml
- FOUND: XdgPaths.java
- FOUND: ViracochaConfig.java
- FOUND: XdgPathsTest.java
- FOUND: ViracochaConfigTest.java
- FOUND: logback-test.xml
- FOUND: 01-01-SUMMARY.md
- FOUND commit: 3fc1edb (Task 1)
- FOUND commit: b3188f9 (Task 2)
- 6/6 tests pass
