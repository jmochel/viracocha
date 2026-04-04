# Phase 5: Subscription model & CLI - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.

**Session:** 2026-04-04  
**Mode:** Interactive discuss-phase with **roadmap-default resolution** for gray areas (no conflicting user input in session).

---

## Gray areas identified

1. **Command surface** — Top-level `vira subscription` vs `vira project subscription *`
2. **Subscription identity** — How `show` / `remove` target a row (id vs composite key)
3. **YAML shape** — Nested under `ProjectEntry` vs root-level `subscriptions[]`
4. **Output & validation** — Alignment with Phase 3; duplicate and path rules

---

## Q&A (synthesized decisions)

### Q1: Command surface

**Options:** (A) Top-level `vira subscription` per ROADMAP, (B) Subcommands under `vira project`.

**Resolution:** **(A)** — ROADMAP Phase 5 design notes explicitly specify `vira subscription` with `add`/`list`/`show`/`remove`; keeps parity with other domain groups (`publisher`, `pattern`, `project`).

### Q2: Subscription identity

**Options:** (A) UUID `id` field, (B) Composite key only (project + publisher + paths), (C) User-supplied slug.

**Resolution:** **(A)** — UUID string at `add` time; required for Phase 7 `vira sync --subscription <id>` without ambiguous matching.

### Q3: YAML shape

**Options:** (A) `projects[].subscriptions[]`, (B) Root `subscriptions[]` with `projectName`.

**Resolution:** **(A)** — Matches ROADMAP; keeps workspace path anchored on `ProjectEntry`.

### Q4: Output and validation

**Resolution:** List/show/`--json` follows **03-CONTEXT.md** conventions; duplicates rejected by publisher+source+destination on same project; reject `..` in relative paths; publisher/project must exist before save.

---

## Notes

- **research_before_questions:** disabled in `.planning/config.json` — no web research block in this session.
- **Advisor mode:** USER-PROFILE not used — no parallel advisor research.

---

*End of discussion log*
