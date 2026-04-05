# Phase 3: Projects and Mappings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `03-CONTEXT.md` — this log preserves the alternatives considered.

**Date:** 2026-04-04
**Phase:** 3 — Projects and Mappings
**Mode:** `/gsd-next` → `/gsd-discuss-phase 3 --auto` (auto-selected recommended defaults)

**Areas discussed:** command surface, output format, validation, data model, tests

---

## Command surface and flags

| Option | Description | Selected |
|--------|-------------|----------|
| Strict REQUIREMENTS.md flags | Match PROJ-* verbatim | ✓ |
| Invent aliases | e.g. short `-p` only | |

**User's choice:** [auto] Strict REQUIREMENTS.md flags — minimizes drift from acceptance tests.

**Notes:** `add-mapping` uses repeatable `--param key=value` as specified.

---

## Output format

| Option | Description | Selected |
|--------|-------------|----------|
| Match Phase 2 list/show/`--json` | Consistent UX across entity types | ✓ |
| YAML-only output | Different from publishers/patterns | |

**User's choice:** [auto] Align with Phase 2 plain text + `--json`.

---

## Validation behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Fail fast, no partial writes | Same as Phase 2 register flows | ✓ |
| Best-effort partial save | — | |

**User's choice:** [auto] Duplicate project and missing pattern both exit 1 without saving.

---

## Mapping storage

| Option | Description | Selected |
|--------|-------------|----------|
| String map per mapping | Simple YAML serialization | ✓ |
| Typed nested objects per param | Overkill for v1 | |

**User's choice:** [auto] String-to-string map for per-mapping parameter values.

---

## Claude's Discretion

- JSON field naming and column formatting
- Package layout for project commands and model classes
- Path normalization details for workspace paths

## Deferred Ideas

None surfaced during auto pass.
