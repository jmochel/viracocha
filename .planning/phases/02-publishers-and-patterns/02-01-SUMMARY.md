---
phase: 02-publishers-and-patterns
plan: 01
subsystem: model
tags: [freemarker, lombok, jackson-dataformat-yaml, picocli, model]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: ViracochaConfig with List<Object> fields, ConfigCommand pattern, pom.xml with jackson-dataformat-yaml
provides:
  - freemarker 2.3.34 compile dependency on classpath
  - PublisherEntry POJO (name + path)
  - PatternEntry POJO (name + path + List<String> parameters)
  - ViracochaConfig with typed List<PublisherEntry> and List<PatternEntry> fields
  - PublisherCommand group command with 4 stub leaf commands
  - PatternCommand group command with 4 stub leaf commands
  - ViracochaCommand wired with full publisher/pattern subcommand hierarchy
affects: [02-02, 02-03, phase-03, phase-04]

# Tech tracking
tech-stack:
  added: [freemarker 2.3.34]
  patterns:
    - Group command with static subcommand list (PublisherCommand, PatternCommand replicating ConfigCommand pattern)
    - Typed YAML POJO with Lombok @Data/@NoArgsConstructor/@AllArgsConstructor

key-files:
  created:
    - src/main/java/org/saltations/model/PublisherEntry.java
    - src/main/java/org/saltations/model/PatternEntry.java
    - src/main/java/org/saltations/publisher/PublisherCommand.java
    - src/main/java/org/saltations/publisher/RegisterPublisherCommand.java
    - src/main/java/org/saltations/publisher/ListPublishersCommand.java
    - src/main/java/org/saltations/publisher/ShowPublisherCommand.java
    - src/main/java/org/saltations/publisher/UnregisterPublisherCommand.java
    - src/main/java/org/saltations/pattern/PatternCommand.java
    - src/main/java/org/saltations/pattern/RegisterPatternCommand.java
    - src/main/java/org/saltations/pattern/ListPatternsCommand.java
    - src/main/java/org/saltations/pattern/ShowPatternCommand.java
    - src/main/java/org/saltations/pattern/UnregisterPatternCommand.java
    - src/test/java/org/saltations/model/ViracochaConfigTypedListTest.java
  modified:
    - pom.xml
    - src/main/java/org/saltations/model/ViracochaConfig.java
    - src/main/java/org/saltations/ViracochaCommand.java

key-decisions:
  - "Stub leaf commands declared in same plan as group commands to keep compilation atomic — Plans 02/03 fill in actual logic"
  - "PatternEntry carries a List<String> parameters field initialized to new ArrayList<>() for JSON serialization safety"
  - "freemarker pinned explicitly at 2.3.34 (not BOM-managed) following logstash-logback-encoder pinning pattern"

patterns-established:
  - "Group command with static subcommand list: @Command(subcommands = {...}) @Singleton implements Callable<Integer>"
  - "Typed YAML POJO: @Data @NoArgsConstructor @AllArgsConstructor Lombok triple for jackson-dataformat-yaml round-trip"

requirements-completed: [PUB-01, PUB-03, PAT-01, PAT-03]

# Metrics
duration: 2min
completed: 2026-03-28
---

# Phase 02 Plan 01: Shared Foundation Summary

**Freemarker 2.3.34 added to classpath, typed PublisherEntry/PatternEntry POJOs created, ViracochaConfig upgraded to typed lists, and publisher/pattern group command stubs wired into the full subcommand hierarchy**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-28T15:05:07Z
- **Completed:** 2026-03-28T15:07:39Z
- **Tasks:** 2
- **Files modified:** 16 (13 created, 3 modified)

## Accomplishments

- freemarker 2.3.34 added as a compile dependency with version pinned in `<properties>`
- Typed PublisherEntry and PatternEntry POJOs replace `List<Object>` in ViracochaConfig
- Full publisher/pattern subcommand hierarchy (2 group commands + 8 leaf stubs) wired into ViracochaCommand
- 3 new typed-list round-trip tests added; all 20 tests green

## Task Commits

Each task was committed atomically:

1. **Task 1: Add freemarker dependency and create typed model POJOs** - `aef8ff2` (feat)
2. **Task 2: Stub PublisherCommand and PatternCommand group commands** - `d28b632` (feat)

**Plan metadata:** (docs commit follows — see final commit below)

## Files Created/Modified

- `pom.xml` - Added freemarker.version property and freemarker 2.3.34 compile dependency
- `src/main/java/org/saltations/model/PublisherEntry.java` - POJO with name + path fields
- `src/main/java/org/saltations/model/PatternEntry.java` - POJO with name + path + List<String> parameters
- `src/main/java/org/saltations/model/ViracochaConfig.java` - Upgraded publishers/patterns to typed lists
- `src/main/java/org/saltations/publisher/PublisherCommand.java` - Group command: vira publisher
- `src/main/java/org/saltations/publisher/RegisterPublisherCommand.java` - Stub leaf command
- `src/main/java/org/saltations/publisher/ListPublishersCommand.java` - Stub leaf command
- `src/main/java/org/saltations/publisher/ShowPublisherCommand.java` - Stub leaf command
- `src/main/java/org/saltations/publisher/UnregisterPublisherCommand.java` - Stub leaf command
- `src/main/java/org/saltations/pattern/PatternCommand.java` - Group command: vira pattern
- `src/main/java/org/saltations/pattern/RegisterPatternCommand.java` - Stub leaf command
- `src/main/java/org/saltations/pattern/ListPatternsCommand.java` - Stub leaf command
- `src/main/java/org/saltations/pattern/ShowPatternCommand.java` - Stub leaf command
- `src/main/java/org/saltations/pattern/UnregisterPatternCommand.java` - Stub leaf command
- `src/main/java/org/saltations/ViracochaCommand.java` - Added PublisherCommand.class and PatternCommand.class to subcommands
- `src/test/java/org/saltations/model/ViracochaConfigTypedListTest.java` - 3 typed-list round-trip tests

## Decisions Made

- Stub leaf commands declared in the same plan as group commands to keep compilation atomic — Plans 02/03 fill in actual logic
- PatternEntry carries `List<String> parameters` initialized to `new ArrayList<>()` for JSON serialization safety
- freemarker pinned explicitly at 2.3.34 in `<properties>` (not BOM-managed), following logstash-logback-encoder pinning pattern

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

The following stub command files contain empty `call()` implementations returning 0. These are intentional foundations for Plans 02 and 03:

- `src/main/java/org/saltations/publisher/RegisterPublisherCommand.java` - line 9: stub `call()` — logic in Plan 02
- `src/main/java/org/saltations/publisher/ListPublishersCommand.java` - line 9: stub `call()` — logic in Plan 02
- `src/main/java/org/saltations/publisher/ShowPublisherCommand.java` - line 9: stub `call()` — logic in Plan 02
- `src/main/java/org/saltations/publisher/UnregisterPublisherCommand.java` - line 9: stub `call()` — logic in Plan 02
- `src/main/java/org/saltations/pattern/RegisterPatternCommand.java` - line 9: stub `call()` — logic in Plan 03
- `src/main/java/org/saltations/pattern/ListPatternsCommand.java` - line 9: stub `call()` — logic in Plan 03
- `src/main/java/org/saltations/pattern/ShowPatternCommand.java` - line 9: stub `call()` — logic in Plan 03
- `src/main/java/org/saltations/pattern/UnregisterPatternCommand.java` - line 9: stub `call()` — logic in Plan 03

All stubs are intentional scaffolding. Plans 02 and 03 are the designated resolvers.

## Issues Encountered

None.

## Next Phase Readiness

- Plans 02 and 03 can now compile and implement publisher/pattern command logic against the typed model
- freemarker is on the classpath, ready for use in pattern registration (Plan 03)
- All existing 20 tests green; no regressions

---
*Phase: 02-publishers-and-patterns*
*Completed: 2026-03-28*
