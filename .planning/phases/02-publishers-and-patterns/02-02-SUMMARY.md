---
phase: 02-publishers-and-patterns
plan: 02
subsystem: cli
tags: [picocli, config, publisher]

# Dependency graph
requires:
  - phase: 02-publishers-and-patterns plan 01
    provides: Typed CatalogEntry, stub publisher leaves, ConfigService
provides:
  - RegisterCatalogCommand, ListCatalogsCommand, ShowCatalogCommand, UnregisterCatalogCommand (full implementations)
  - Four integration test classes for catalog commands
affects: [02-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - load-mutate-save via ConfigService for publisher CRUD
    - Plain-text columns and JSONL (--json) mirroring D-01/D-03

key-files:
  created:
    - src/test/java/org/saltations/catalog/RegisterCatalogCommandTest.java
    - src/test/java/org/saltations/catalog/ListCatalogsCommandTest.java
    - src/test/java/org/saltations/catalog/ShowCatalogCommandTest.java
    - src/test/java/org/saltations/catalog/UnregisterCatalogCommandTest.java
  modified:
    - src/main/java/org/saltations/catalog/RegisterCatalogCommand.java
    - src/main/java/org/saltations/catalog/ListCatalogsCommand.java
    - src/main/java/org/saltations/catalog/ShowCatalogCommand.java
    - src/main/java/org/saltations/catalog/UnregisterCatalogCommand.java

key-decisions:
  - "Publisher commands follow InitCommand-style error handling and @Spec for testable stdout/stderr"

patterns-established:
  - "Publisher list uses printf column alignment; list --json emits one ObjectMapper line per entry"

requirements-completed: [PUB-01, PUB-02, PUB-03, PUB-04, PUB-05]

# Metrics
duration: —
completed: 2026-04-04
---

# Phase 02 Plan 02: Publisher Commands Summary

**All four catalog leaf commands implemented with tests: register (path + duplicate validation), list (plain + JSON), show (plain + JSON), unregister — PUB-01 through PUB-05.**

## Performance

- **Duration:** (tracked with Plan 03 in this session)
- **Tasks:** 2 (per plan)
- **Files:** 8 (4 main + 4 test)

## Accomplishments

- Publisher CRUD matches Phase 1 command patterns (`Callable<Integer>`, `ConfigNotInitializedException`, exit 1 on errors).
- Integration tests use temp `XdgPaths` and `CommandLine` with captured streams.

## Deviations from Plan

None — behavior matches 02-02-PLAN.md.

## Files Created/Modified

See `key-files` frontmatter.
