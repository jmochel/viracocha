# Phase 6: Sync engine — Technical research

**Date:** 2026-04-04  
**Question:** What do we need to know to plan and implement the sync engine well?

## RESEARCH COMPLETE

---

## 1. Java NIO building blocks

- **`Files.walk(Path)`** with `maxDepth` optional — returns stream; use try-with-resources; filter `Files.isRegularFile` for file sync (directories handled implicitly via parent creation on copy).
- **`Files.mismatch(Path, Path)`** (Java 12+): returns `-1` if identical, else first differing byte index — ideal for D-01 byte equality. JDK 21 available per project.
- **`Files.copy(Path, Path, CopyOption...)`** with `StandardCopyOption.REPLACE_EXISTING` only when safe — Phase 6 apply phase copies **to missing targets** or **updates** when analyze said no conflict; use `REPLACE_EXISTING` when overwriting a file that existed with same path after clean analyze (actually if analyze passed, dest either missing or identical — so copy with CREATE + REPLACE for update case).
- **`Files.isSymbolicLink`**, **`Files.isRegularFile`**, **`Files.isDirectory`** — check symlinks **before** regular file checks (symlink may report as regular file if following — use `isSymbolicLink` first per D-03).

## 2. Hidden path parity (SYN-05)

- Reuse **`PatternPathUtils.hasHiddenPathSegment(Path root, Path path)`** with `root` = subscription subtree root on each side (`publisherRoot` / `workspaceRoot`), same as `GeneratorService` filters pattern walks.

## 3. Relative path identity

- Normalize relative paths with **`/`** separators for stable keys (`.toString().replace('\\', '/')` on relativized paths).
- Lexicographic sort: **`String::compareTo`** on normalized relative strings (D-07).

## 4. Two-phase bidirectional (D-08)

1. **Analyze:** Union of relative paths from both trees (regular files only, hidden filtered). For each path present on both sides: classify conflict kinds. Collect full conflict list without writes.
2. **Apply:** If conflicts non-empty for subscription → skip mutations. Else: copies **publisher → workspace** first (all needed), then **workspace → publisher** (all needed), each in lexicographic order.

## 5. Micronaut integration

- **`@Singleton`** service, **`ConfigService` injected**, load config in `syncProject(String projectName)` (or pass `ViracochaConfig` for tests). Match `GeneratorService` style.

## 6. Risks / pitfalls

- **Symlink:** Must not use `Files.walk` following links blindly — `Files.walk` does not follow by default for listing, but copying must not follow — D-03 says treat symlink as blocked/conflict.
- **Path escape:** Resolve `publisher.path` and `project.path` to absolute; verify copied targets stay under intended roots (mirror `GeneratorService` workspace containment check).

---

## Validation Architecture

Phase 6 verification is **JUnit 5** + **`@TempDir`** filesystem scenarios (no separate framework).

| Dimension | Approach |
|-----------|----------|
| **Automated** | `mvn test`; focused test class `SyncServiceTest` (or per-plan classes) |
| **SYN-01–SYN-04** | Temp dirs with publisher + workspace trees; assert result counts and conflict lists |
| **SYN-05** | Tree with `.hidden` or `.git` segment — assert file not copied / skipped per hidden rule |
| **Sampling** | Run full `mvn test` after each plan wave |

**Quick command:** `mvn -q test`  
**Full suite:** `mvn test`

**Wave 0:** Not required — existing Maven + JUnit infrastructure covers the phase.
