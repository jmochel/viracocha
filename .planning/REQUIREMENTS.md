# Requirements: Viracocha

**Defined:** 2026-04-04  
**Milestone:** v2.0 Subscriptions & sync  
**Core Value:** (from PROJECT.md) Register patterns and catalogs once; generate safely; **keep published artifacts in sync with the workspace** via subscriptions.

---

## v2.0 Requirements

Requirements for milestone v2.0. Each maps to roadmap phases (5–7).

### Config schema

- [ ] **CFG-01**: Central YAML includes a `subscriptions` collection (or nested under projects — see roadmap) with enough fields to persist catalog reference, paths, direction, and stable identity for CLI targeting
- [ ] **CFG-02**: Config load/save round-trips subscription entries without data loss; invalid YAML fails with a clear error

### Subscriptions (CLI & model)

- [ ] **SUB-01**: User can add a subscription on a **project** that references a registered **catalog** by name and ties a catalog-side source path to a workspace-relative path (subscription subtree)
- [ ] **SUB-02**: Each subscription records **sync direction**: catalog→workspace only, workspace→catalog only, or **bidirectional**
- [ ] **SUB-03**: User can **list** subscriptions (scope: project or global list — as implemented in roadmap) in plain text and JSON consistent with existing `vira` commands
- [ ] **SUB-04**: User can **show** details for one subscription
- [ ] **SUB-05**: User can **remove** a subscription
- [ ] **SUB-06**: Registration validates that the project and catalog exist, paths exist where required, and paths reject unsafe traversal (e.g. `..` escapes)
- [ ] **SUB-07**: Duplicate or overlapping subscriptions are rejected or warned per explicit rules documented in CLI help

### Sync engine

- [ ] **SYN-01**: A sync service can **copy files** from catalog tree into the workspace for subscriptions with catalog→workspace or bidirectional direction
- [ ] **SYN-02**: The same service can **copy files** from workspace into the catalog tree when direction includes workspace→catalog
- [ ] **SYN-03**: **Bidirectional** runs combine both directions using a **documented conflict policy** when both sides have changes affecting the same relative path
- [ ] **SYN-04**: Default conflict behavior is **safe** (e.g. fail the sync with a clear report); optional flags may allow **explicit** overwrite / last-write-wins where implemented
- [ ] **SYN-05**: Sync skips or includes **hidden** segments consistently with `generate` (document behavior)

### `vira sync` command

- [ ] **SYN-06**: User can run `vira sync --project <name>` to execute sync for that project’s subscriptions (full or filtered per roadmap)
- [ ] **SYN-07**: User can pass **`--dry-run`** to print planned actions without mutating files
- [ ] **SYN-08**: User can pass **`--verbose`** for per-file outcomes (created / updated / skipped / conflict)
- [ ] **SYN-09**: Sync prints a **summary** line on completion (counts: copied, skipped, failed, conflicts)

### Cross-cutting

- [ ] **X-01**: Integration tests cover at least one catalog→workspace flow, one workspace→catalog flow, and one bidirectional conflict case
- [ ] **X-02**: README documents subscriptions, directions, conflict policy, and examples

---

## Deferred (post–v2.0)

| ID | Item |
|----|------|
| WATCH-01 | Watch mode / background sync daemon |
| PUB-02a | `vira catalog list` shows subscriber count per catalog (optional enhancement) |
| PAT-02a | `vira pattern unregister` warns when pattern still referenced |

---

## Out of scope (v2.0)

| Feature | Reason |
|---------|--------|
| Remote catalogs (HTTP/Git) | Network/auth complexity; local paths only |
| Git integration | Explicit v1/v2 filesystem scope |
| Interactive merge UI | Scriptable CLI first |

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CFG-01 | Phase 5 | Pending |
| CFG-02 | Phase 5 | Pending |
| SUB-01 | Phase 5 | Pending |
| SUB-02 | Phase 5 | Pending |
| SUB-03 | Phase 5 | Pending |
| SUB-04 | Phase 5 | Pending |
| SUB-05 | Phase 5 | Pending |
| SUB-06 | Phase 5 | Pending |
| SUB-07 | Phase 5 | Pending |
| SYN-01 | Phase 6 | Pending |
| SYN-02 | Phase 6 | Pending |
| SYN-03 | Phase 6 | Pending |
| SYN-04 | Phase 6 | Pending |
| SYN-05 | Phase 6 | Pending |
| SYN-06 | Phase 7 | Pending |
| SYN-07 | Phase 7 | Pending |
| SYN-08 | Phase 7 | Pending |
| SYN-09 | Phase 7 | Pending |
| X-01 | Phase 7 | Pending |
| X-02 | Phase 7 | Pending |

**Coverage:**

- v2.0 requirements: 18 total  
- Mapped to phases: 18  
- Unmapped: 0 ✓  

---
*Requirements defined: 2026-04-04 — milestone v2.0*
