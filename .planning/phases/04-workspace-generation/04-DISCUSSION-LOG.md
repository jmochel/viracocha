# Phase 4: Workspace Generation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `04-CONTEXT.md` — this log preserves rationale and alternatives.

**Date:** 2026-04-04
**Phase:** 4 — Workspace Generation
**Mode:** Requirements-driven (no interactive gray-area menu in this session)

---

## Session note

Discuss-phase was executed with **decisions locked to** `.planning/REQUIREMENTS.md` (GEN-01..GEN-08), ROADMAP Phase 4 success criteria, and prior phase CONTEXT files (01–03). Gray areas that would normally be interactive (CLI output shape, merge order, PathExpander-first sequencing) were resolved to **recommended defaults** aligned with those documents and with `PROJECT.md` (skip-existing, project vs mapping params).

If you want different behavior (e.g., exit code 3 for generation failure per v2 UX-03), edit `04-CONTEXT.md` before `/gsd-plan-phase 4`.

---

## Command surface and flags

| Option | Description | Selected |
|--------|-------------|----------|
| As per GEN-01,07,08 | `generate --project-name`, `--dry-run`, `--verbose` | ✓ |
| Alternate spellings | e.g. `--project` only | — |

**Rationale:** REQUIREMENTS spell the flag names; deviating would fail acceptance tests.

---

## Freemarker model merge (GEN-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Project defaults + mapping overlay | `ProjectEntry` gains optional `parameters`; mapping map wins on key clash | ✓ |
| Mapping only | No project-level defaults | — |

**Rationale:** Matches `PROJECT.md` and GEN-02.

---

## Path expansion vs template engine

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated PathExpander + Freemarker for bodies | Explicit utility for path segments; tested first | ✓ |
| Freemarker-only for paths | Rejected — Freemarker does not expand path segments by itself | — |

**Rationale:** Documented blocker in STATE.md Accumulated Context.

---

## Deferred ideas

- None captured beyond v2 backlog already in CONTEXT.
