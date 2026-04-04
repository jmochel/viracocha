# Phase 6: Sync engine - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.  
> Decisions are captured in `06-CONTEXT.md`.

**Date:** 2026-04-04  
**Phase:** 6 — Sync engine  
**Areas discussed:** Conflict detection, Tree semantics, Bidirectional ordering, Result / conflict API  

---

## Conflict detection

| Option | Description | Selected |
|--------|-------------|----------|
| Byte equality for regular files | Both regular files, use `Files.mismatch` or equivalent | ✓ |
| Hash-only compare | Faster for large files; adds complexity | |
| Timestamp-based | Not used for v2.0 Phase 6 default | |

| Option | Description | Selected |
|--------|-------------|----------|
| Type mismatch = conflict | File vs directory at same relative path | ✓ |
| Auto-resolve type mismatch | Rejected — risky | |

| Option | Description | Selected |
|--------|-------------|----------|
| Follow symlinks | Risk of silent wrong copy | |
| Skip symlinks quietly | Hidden ambiguity | |
| Explicit conflict / blocked for symlinks | User-chosen via discussion | ✓ |

**User's choice:** Byte compare for regular files; type mismatch and symlink paths are explicit conflict/blocked outcomes; missing-on-one-side drives copy (not conflict).

**Notes:** Aligns with SYN-03 / SYN-04 safe default.

---

## Tree semantics

| Option | Description | Selected |
|--------|-------------|----------|
| Mirror deletes | Remove extras on dest | |
| Copy-only (no deletes) | Safer for Phase 6 | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Sync empty directories as first-class | Extra walk rules | |
| Parents created as needed for files only | Simpler | ✓ |

**User's choice:** No mirror-delete; deterministic lexicographic copy order; empty dirs not synced as standalone nodes.

---

## Bidirectional ordering

| Option | Description | Selected |
|--------|-------------|----------|
| Single interleaved pass | Harder to test | |
| Analyze then apply | Detect all conflicts before writes | ✓ |
| Apply pub→ws before ws→pub after clean analyze | Deterministic | ✓ |

**User's choice:** Two-phase (analyze → apply); apply order publisher→workspace then workspace→publisher; abort apply if conflicts detected in analyze.

---

## Result / conflict API

| Option | Description | Selected |
|--------|-------------|----------|
| Unstructured strings only | Hard for Phase 7 | |
| Structured result + enum kinds + subscription id | Testable and CLI-ready | ✓ |

**User's choice:** Structured records with relative path, kind enum, optional message; counts per subscription; camelCase-friendly for future JSON.

---

## Claude's Discretion

- Class/package naming; walk implementation details; per-subscription vs global atomicity (recommended: best-effort per subscription with aggregated result).

## Deferred ideas

- Conflict strategy flags — later phase  
- Mirror delete — deferred  
- Symlink replication — deferred  
