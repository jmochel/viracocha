---
phase: 02-publishers-and-patterns
plan: 03
subsystem: pattern
tags: [freemarker, picocli, regex]

# Dependency graph
requires:
  - phase: 02-publishers-and-patterns plan 01
    provides: PatternEntry, stub pattern leaves
  - phase: 02-publishers-and-patterns plan 02
    provides: Publisher command patterns for replication
provides:
  - FreemarkerVariableExtractor (D-04/D-05, hidden-path skip)
  - RegisterPatternCommand, ListPatternsCommand, ShowPatternCommand, UnregisterPatternCommand
  - Five test classes (extractor + four commands)
affects: [phase-03, phase-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Regex extraction for top-level Freemarker names; IOException on malformed `${`
    - Pattern register stores sorted extracted names in PatternEntry.parameters

key-files:
  created:
    - src/test/java/org/saltations/pattern/RegisterPatternCommandTest.java
    - src/test/java/org/saltations/pattern/ListPatternsCommandTest.java
    - src/test/java/org/saltations/pattern/ShowPatternCommandTest.java
    - src/test/java/org/saltations/pattern/UnregisterPatternCommandTest.java
  modified:
    - src/main/java/org/saltations/pattern/FreemarkerVariableExtractor.java
    - src/main/java/org/saltations/pattern/RegisterPatternCommand.java
    - src/main/java/org/saltations/pattern/ListPatternsCommand.java
    - src/main/java/org/saltations/pattern/ShowPatternCommand.java
    - src/main/java/org/saltations/pattern/UnregisterPatternCommand.java
    - src/test/java/org/saltations/pattern/FreemarkerVariableExtractorTest.java

key-decisions:
  - "Hidden files: skip any path under root whose relative path contains a segment starting with '.' (fixes `.git/config` leaking `${secret}`)"
  - "Show pattern plain text uses Parameters: (none) when no variables extracted"

patterns-established:
  - "List patterns: name, path, param count columns; JSON lines include parameters array"

requirements-completed: [PAT-01, PAT-02, PAT-03, PAT-04, PAT-05, PAT-06]

# Metrics
duration: —
completed: 2026-04-04
---

# Phase 02 Plan 03: Patterns + Freemarker Extractor Summary

**FreemarkerVariableExtractor walks pattern trees (content + path segments), enforces D-05 malformed `${`, skips hidden subtrees; pattern commands register/list/show/unregister with extracted parameters persisted in YAML.**

## Performance

- **Tasks:** 2
- **Tests:** FreemarkerVariableExtractorTest (9) + pattern command tests (16)

## Accomplishments

- `hasHiddenPathSegment` ensures `.git/**` files do not contribute variables.
- Register loads config, validates path, runs extractor, saves `PatternEntry(name, path, parameters)`.
- List/show/unregister parallel publisher UX with parameter count and `Parameters:` / JSON.

## Deviations from Plan

- Fixed stray typo in extractor (`Collections.sort` line) and aligned implementation with hidden-directory behavior required by tests (plan snippet still showed leaf-only filter).

## Verification

- `./mvnw clean test` — all modules green.
