# Phase 12: Sync Rewrite - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-10
**Phase:** 12-sync-rewrite
**Areas discussed:** Conflict semantics, Template source handling, Result model redesign, SyncCommand option cleanup

---

## Conflict semantics

| Option | Description | Selected |
|--------|-------------|----------|
| Timestamp-based: newer side wins | source.mtime > dest.mtime → copy; dest.mtime > source.mtime → conflict, exit 1 | ✓ |
| Any mismatch = conflict, abort | If dest exists and content != source → abort with exit 1 | |
| Source always wins, overwrite | Copy source to dest unconditionally when content differs | |

**User's choice:** Timestamp-based
**Notes:** User asked about FAT32 and OS mtime reliability. Clarified that local filesystem scope (ext4, APFS, NTFS) makes mtime reliable; FAT32's 2-second resolution is the only edge case and is unlikely for dev dirs. Java's `Files.copy()` not preserving mtime by default was identified as beneficial — dest.mtime reflects time of copy, so subsequent source updates are detectable. User confirmed timestamp-based after that clarification.

Secondary `Files.mismatch()` check added to avoid unnecessary writes when content is already identical despite mtime differences.

---

## Template source handling

| Option | Description | Selected |
|--------|-------------|----------|
| Re-expand templates (same as generate) | Walk template source, expand Freemarker, compare/copy to dest | |
| Skip template sources entirely | Sync only processes non-template sources | ✓ |
| Byte-copy template source files as-is | Copy raw .ftl files unexpanded to dest | |

**User's choice:** Skip template sources entirely on sync
**Notes:** User initially responded "Same as 1 unless destination is newer than sources, then exit" — interpreted as option 1 + timestamp conflict detection. Follow-up confirmed they actually wanted option 2 (skip entirely). Template expansion is a generate-only operation.

---

## Result model redesign

| Option | Description | Selected |
|--------|-------------|----------|
| New flat record like GenerationResult | SyncResult record with copied/skipped/failed/conflicts/verboseLines/conflictRecords | ✓ |
| Rework existing classes in-place | Rename SyncSubscriptionResult → SyncMappingResult, replace subscriptionId with mappingIndex | |
| Flat aggregate only (no per-mapping breakdown) | Single counts object, no nesting | |

**User's choice:** New flat record like GenerationResult
**Notes:** Deletes v2 artifacts (SyncEngineResult, SyncSubscriptionResult). SyncConflictRecord adapted (remove subscriptionId field). SyncConflictKind reused as-is.

---

## SyncCommand option cleanup

### --mapping-id fate

| Option | Description | Selected |
|--------|-------------|----------|
| Remove it entirely | No SYN requirement; clean break with v2 | ✓ |
| Keep but ignore (silent no-op) | Preserves parsing compat but does nothing | |

**User's choice:** Remove entirely

### --destination-name optionality

| Option | Description | Selected |
|--------|-------------|----------|
| Optional — sync all if omitted | Matches SC3 filter wording | |
| Required — must specify destination | Consistent with generate behavior | ✓ |

**User's choice:** Required — must specify a destination
**Notes:** Consistency with generate chosen over SC3 wording flexibility.

---

## Claude's Discretion

- Sorted file traversal order within mappings (recommended for test determinism)
- Exception wrapping strategy for IOException in traversal loop
- Whether SyncResult.empty() returns mutable or immutable instance
- relativePath representation (POSIX string vs. Path object internally)

## Deferred Ideas

- Per-mapping filter (--mapping-id): no SYN requirement, removed for v3 clean break
- --destination-name optional (sync all): user chose required; revisit if all-destination sync needed
- Template source sync: deferred; template expansion is generate-only for Phase 12
