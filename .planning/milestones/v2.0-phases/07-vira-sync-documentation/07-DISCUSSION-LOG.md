# Phase 7: `vira sync` & documentation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.  
> Decisions are captured in `07-CONTEXT.md`.

**Date:** 2026-04-04  
**Phase:** 7 — `vira sync` & documentation  
**Areas discussed:** CLI flags & naming; exit codes & channels; dry-run & verbose; conflict strategy & README

---

## 1 — CLI flags & naming

| Option | Description | Selected |
|--------|-------------|----------|
| A | `--project-name` + optional `--subscription`, `--dry-run`, `--verbose`, `--json` (single object) | ✓ |
| B | `--project` only (roadmap literal) without aligning to `generate` | |
| C | No `--json` in v2.0 | |

**User's choice:** Consolidated proposal (A) — **Confirmed** via prompt.  
**Notes:** `--project-name` matches `generate`; roadmap `--project` treated as same intent.

---

## 2 — Exit codes & output channels

| Option | Description | Selected |
|--------|-------------|----------|
| A | Exit 0 iff no conflicts and no failures; exit 1 otherwise; human summary stdout; stderr for conflicts/errors | ✓ |
| B | Multiple exit codes for conflict vs I/O | |

**User's choice:** (A) — **Confirmed.**

---

## 3 — Dry-run & verbose

| Option | Description | Selected |
|--------|-------------|----------|
| A | Dry-run = analyze-only, no writes; verbose = per-path lines; combined = planned actions only | ✓ |
| B | Dry-run always exit 0 | |

**User's choice:** (A) — **Confirmed.**

---

## 4 — Conflict strategy & README

| Option | Description | Selected |
|--------|-------------|----------|
| A | Defer `--conflict-strategy`; README documents abort-only + workflow | ✓ |
| B | Implement optional strategy in Phase 7 | |

**User's choice:** (A) — **Confirmed.**

---

## Claude's Discretion

- JSON field naming and exact summary line wording left to implementation.

## Deferred Ideas

- Conflict strategy flags; finer exit codes; watch mode — see `07-CONTEXT.md` `<deferred>`.
