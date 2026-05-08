---
phase: "08"
plan: "01"
subsystem: model-config-foundation
tags: [v3-model, refactor, model, infra, stubs]
dependency_graph:
  requires: []
  provides:
    - org.saltations.model.SourceEntry
    - org.saltations.model.DestinationEntry
    - org.saltations.model.MappingEntry (v3)
    - org.saltations.model.ViracochaConfig (v3)
    - org.saltations.infra.HiddenPathFilter
    - org.saltations.infra.FreemarkerVariableExtractor
  affects:
    - GeneratorService (stubbed for Phase 11)
    - DefaultSyncService (stubbed for Phase 12)
    - ViracochaCommand (v2 subcommands removed)
tech_stack:
  added: []
  patterns:
    - Stub pattern: UnsupportedOperationException with Phase reference in message
    - v3 POJO pattern: @Data @NoArgsConstructor @AllArgsConstructor with Lombok
key_files:
  created:
    - src/main/java/org/saltations/infra/HiddenPathFilter.java
    - src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java
    - src/main/java/org/saltations/model/SourceEntry.java
    - src/main/java/org/saltations/model/DestinationEntry.java
    - src/test/java/org/saltations/model/ViracochaConfigV3Test.java
    - src/test/java/org/saltations/model/ViracochaConfigV3TypedListTest.java
  modified:
    - src/main/java/org/saltations/model/MappingEntry.java (v2 -> v3)
    - src/main/java/org/saltations/model/ViracochaConfig.java (v2 -> v3)
    - src/main/java/org/saltations/generate/GeneratorService.java (stubbed)
    - src/main/java/org/saltations/sync/DefaultSyncService.java (stubbed)
    - src/main/java/org/saltations/ViracochaCommand.java (v2 subcommands removed)
    - src/test/java/org/saltations/config/ConfigServiceTest.java (version 1->3)
    - src/test/java/org/saltations/config/ShowConfigCommandTest.java (version 1->3)
    - src/test/java/org/saltations/ViracochaCommandTest.java (removed subscription check)
    - src/main/resources/META-INF/native-image/org.saltations/viracocha/reflect-config.json
  deleted:
    - src/main/java/org/saltations/model/CatalogEntry.java
    - src/main/java/org/saltations/model/ArchetypeEntry.java
    - src/main/java/org/saltations/model/ProjectEntry.java
    - src/main/java/org/saltations/model/SubscriptionEntry.java
    - src/main/java/org/saltations/model/SubscriptionSyncDirection.java
    - src/main/java/org/saltations/archetype/ (5 command files)
    - src/main/java/org/saltations/catalog/ (5 command files)
    - src/main/java/org/saltations/project/ (6 command files)
    - src/main/java/org/saltations/subscription/ (5 command files)
    - src/test/java/org/saltations/model/{ViracochaConfigTest,ViracochaConfigTypedListTest,ViracochaConfigProjectTypedListTest,SubscriptionEntryYamlTest}.java
    - src/test/java/org/saltations/catalog/ (4 test files)
    - src/test/java/org/saltations/project/ (2 test files)
    - src/test/java/org/saltations/subscription/ (2 test files)
    - src/test/java/org/saltations/sync/ (3 test files)
decisions:
  - "Stubbed GeneratorService and DefaultSyncService with UnsupportedOperationException referencing the Phase where they will be rewritten (11 and 12 respectively)"
  - "Removed v2 command packages (archetype, catalog, project, subscription) and their tests in this plan instead of Plan 02 — required for compile-clean state after v2 model classes were deleted"
metrics:
  duration: ~20 minutes
  completed: "2026-05-08"
  tasks_completed: 2
  files_created: 8
  files_modified: 9
  files_deleted: 36
---

# Phase 08 Plan 01: V3 Model Config Foundation Summary

**One-liner:** Relocated HiddenPathFilter/FreemarkerVariableExtractor to infra/, introduced v3 SourceEntry/DestinationEntry/MappingEntry/ViracochaConfig POJOs, and stubbed GeneratorService/DefaultSyncService with UnsupportedOperationException — all tests green.

## What Was Built

### Task 1: Relocate Utility Classes to infra/

Created `org.saltations.infra.HiddenPathFilter` (renamed from `ArchetypePathUtils`) and `org.saltations.infra.FreemarkerVariableExtractor` (moved, updated import). Updated `GeneratorService` and `DefaultSyncService` to import `HiddenPathFilter` instead of `ArchetypePathUtils`. Compilation verified green.

### Task 2: V3 Model POJOs, Stub Services, Green Tests

**New POJOs:**
- `SourceEntry`: name, path, templates (bool), parameters (List<String>)
- `DestinationEntry`: name, path, parameters (Map), mappings (List<MappingEntry>)
- `MappingEntry` (v3): sourceRef, glob, recurse (bool), sync (bool), params (Map)
- `ViracochaConfig` (v3): version=3, sources (List<SourceEntry>), destinations (List<DestinationEntry>)

**Stubbed services:** GeneratorService and DefaultSyncService now throw `UnsupportedOperationException` with a Phase reference, retaining their constructor and method signatures.

**Tests:** Created `ViracochaConfigV3Test` (3 tests, YAML round-trip) and `ViracochaConfigV3TypedListTest` (5 tests, typed field round-trips). All 32 tests pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Deleted v2 command packages ahead of Plan 02 schedule**

- **Found during:** Task 2 — after deleting v2 model classes, 16 production command files in archetype/, catalog/, project/, subscription/ packages failed to compile
- **Issue:** Plan 02 was scheduled to delete these command packages, but with v2 model classes deleted, compilation failed immediately
- **Fix:** Deleted all v2 command packages (archetype/, catalog/, project/, subscription/) and their associated test files. Updated ViracochaCommand.java to register only config, generate, sync subcommands.
- **Files modified:** ViracochaCommand.java; deleted 16 production command files, 11 test files
- **Commit:** cda3b88

**2. [Rule 1 - Bug] Updated ShowConfigCommandTest to expect version: 3**

- **Found during:** Task 2 test run
- **Issue:** `ShowConfigCommandTest.showAfterInitContainsYamlVersion` asserted `version: 1` but ViracochaConfig now defaults to version=3
- **Fix:** Updated assertion to `version: 3`
- **Files modified:** src/test/java/org/saltations/config/ShowConfigCommandTest.java
- **Commit:** cda3b88

**3. [Rule 2 - Missing Critical] Updated reflect-config.json for GraalVM native image**

- **Found during:** Task 2 cleanup
- **Issue:** reflect-config.json still listed CatalogEntry, ArchetypeEntry, ProjectEntry (deleted classes) and was missing SourceEntry, DestinationEntry
- **Fix:** Replaced v2 model entries with v3 model entries
- **Files modified:** src/main/resources/META-INF/native-image/org.saltations/viracocha/reflect-config.json
- **Commit:** cda3b88

## Known Stubs

- `GeneratorService.generate()` — throws `UnsupportedOperationException`. Intentional; full v3 implementation deferred to Phase 11.
- `DefaultSyncService.syncProject()` — throws `UnsupportedOperationException`. Intentional; full v3 implementation deferred to Phase 12.
- `GeneratorServiceTest` — placeholder test `assertTrue(true)`. Tests rewritten in Phase 11.
- `GenerateCommandTest` — placeholder test `assertTrue(true)`. Tests rewritten in Phase 11.

## Self-Check

Files created/verified:
- src/main/java/org/saltations/infra/HiddenPathFilter.java — EXISTS
- src/main/java/org/saltations/infra/FreemarkerVariableExtractor.java — EXISTS
- src/main/java/org/saltations/model/SourceEntry.java — EXISTS
- src/main/java/org/saltations/model/DestinationEntry.java — EXISTS
- src/main/java/org/saltations/model/MappingEntry.java — EXISTS (v3)
- src/main/java/org/saltations/model/ViracochaConfig.java — EXISTS (v3)
- src/test/java/org/saltations/model/ViracochaConfigV3Test.java — EXISTS
- src/test/java/org/saltations/model/ViracochaConfigV3TypedListTest.java — EXISTS

Commits:
- 5537b68 feat(08-01): task 1 - relocate utility classes to infra/
- cda3b88 feat(08-01): task 2 - v3 model POJOs, stub services, green tests

## Self-Check: PASSED
