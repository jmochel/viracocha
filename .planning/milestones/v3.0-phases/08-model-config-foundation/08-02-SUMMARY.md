---
phase: "08"
plan: "02"
subsystem: model-config-foundation
tags: [v3-config, version-guard, v2-cleanup, config-service]
dependency_graph:
  requires:
    - 08-01
  provides:
    - ConfigVersionException (CFG-02)
    - ConfigService.load() version guard
    - v2 command packages fully removed (CFG-03)
  affects:
    - ShowConfigCommand (surfaces ConfigVersionException via IOException handler)
    - GenerateCommand (updated to v3 terminology)
    - SyncCommand (updated to v3 terminology)
tech_stack:
  added: []
  patterns:
    - Version guard pattern: readTree() pre-check before full deserialization
    - ConfigVersionException extends IOException so existing catch blocks surface the error
key_files:
  created:
    - src/main/java/org/saltations/config/ConfigVersionException.java
  modified:
    - src/main/java/org/saltations/config/ConfigService.java (load() with version guard)
    - src/main/java/org/saltations/generate/GenerateCommand.java (v3 terminology)
    - src/main/java/org/saltations/generate/PathExpander.java (fixed stale @link)
    - src/main/java/org/saltations/sync/SyncCommand.java (v3 terminology)
    - src/test/java/org/saltations/config/ConfigServiceTest.java (4 new tests)
    - src/test/java/org/saltations/config/ShowConfigCommandTest.java (1 new test)
    - src/test/java/org/saltations/ViracochaCommandTest.java (assertFalse for v2 commands)
decisions:
  - "Updated GenerateCommand and SyncCommand descriptions/options to v3 terminology (destination vs project) to allow ViracochaCommandTest assertFalse checks on v2 command names"
  - "ConfigVersionException extends IOException so ShowConfigCommand's existing IOException catch surfaces the message without additional command-specific code"
metrics:
  duration: ~20 minutes
  completed: "2026-05-09"
  tasks_completed: 2
  files_created: 1
  files_modified: 7
  files_deleted: 0
---

# Phase 08 Plan 02: ConfigVersionException and v2 Package Removal Summary

**One-liner:** Added ConfigVersionException with D-09 message format and version pre-read guard in ConfigService.load(), updated GenerateCommand/SyncCommand to v3 terminology, and confirmed full v2 package removal from Plan 01 — all 37 tests green.

## What Was Built

### Task 1: ConfigVersionException and Version Guard in ConfigService.load()

Created `ConfigVersionException extends IOException` with the exact D-09 message format:
`Config file is v{N} — v3 format required. Delete ~/.config/viracocha/config.yaml and run 'vira config init' to start fresh.`

Updated `ConfigService.load()` to:
1. Call `yaml.readTree(configFile.toFile())` for a fast version pre-read
2. Extract the `version` field (treating missing/null as 0)
3. Throw `ConfigVersionException(version)` when version < 3
4. Fall through to full `yaml.readValue()` for version >= 3

Added 4 tests to `ConfigServiceTest`:
- `loadThrowsConfigVersionExceptionForV1Config` — version 1 triggers exception
- `loadThrowsConfigVersionExceptionForMissingVersionField` — missing field treated as v0
- `loadSucceedsForV3Config` — version 3 loads normally
- `initWritesVersion3` — init() writes "version: 3" to the file

Added `showPrintsVersionErrorForV2Config` to `ShowConfigCommandTest` — verifies that when a v1 config exists, the show command exits 1 and stderr contains "Error reading config: ... v3 format required".

### Task 2: v2 Cleanup Verification and ViracochaCommandTest Update

Confirmed all v2 package deletion completed in Plan 01:
- archetype/, catalog/, project/, subscription/ packages gone from main
- All v2 test directories deleted
- ViracochaCommand.java already updated to ConfigCommand/GenerateCommand/SyncCommand only
- reflect-config.json already updated to v3 model entries

Updated `ViracochaCommandTest` to add `assertFalse` assertions ensuring v2 command names are absent from `--help` output.

Found that GenerateCommand and SyncCommand still used v2 terminology ("project mappings", "per subscriptions", `--project-name`, `--subscription` options) causing the `assertFalse(help.contains("project/subscription"))` assertions to fail. Updated both commands to v3 terminology:
- GenerateCommand: description "Generate workspace files from sources to a destination." and `--dest` option
- SyncCommand: description "Sync files from source directories to destination workspaces per mapping rules." and `--dest`/`--mapping-id` options

Also fixed a stale Javadoc `@link` in PathExpander pointing to the deleted `org.saltations.archetype.FreemarkerVariableExtractor` — updated to `org.saltations.infra.FreemarkerVariableExtractor`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated GenerateCommand and SyncCommand to v3 terminology**

- **Found during:** Task 2 — ViracochaCommandTest `assertFalse(help.contains("project/subscription"))` failed because these words appeared in command descriptions and option names
- **Issue:** GenerateCommand description "from project mappings" and `--project-name` option; SyncCommand description "per subscriptions" and `--subscription` option caused the assertFalse checks to fail
- **Fix:** Updated both commands to use v3 domain terminology (destination, mapping-id instead of project, subscription)
- **Files modified:** src/main/java/org/saltations/generate/GenerateCommand.java, src/main/java/org/saltations/sync/SyncCommand.java
- **Commit:** 767ca2c

**2. [Rule 1 - Bug] Fixed stale @link in PathExpander Javadoc**

- **Found during:** Task 2 verification (grep for org.saltations.archetype refs)
- **Issue:** PathExpander.java Javadoc had `{@link org.saltations.archetype.FreemarkerVariableExtractor}` pointing to a deleted class
- **Fix:** Updated to `{@link org.saltations.infra.FreemarkerVariableExtractor}`
- **Files modified:** src/main/java/org/saltations/generate/PathExpander.java
- **Commit:** 767ca2c

## Known Stubs

- `GeneratorService.generate()` — throws `UnsupportedOperationException`. Deferred to Phase 11.
- `DefaultSyncService.syncProject()` — throws `UnsupportedOperationException`. Deferred to Phase 12.

## Self-Check

Files created/verified:
- src/main/java/org/saltations/config/ConfigVersionException.java — EXISTS
- src/main/java/org/saltations/config/ConfigService.java — MODIFIED (has readTree + ConfigVersionException)

Commits:
- 08db755 feat(08-02): task 1 - ConfigVersionException and version guard in ConfigService.load()
- 767ca2c feat(08-02): task 2 - verify v2 cleanup complete, green tests

## Self-Check: PASSED
