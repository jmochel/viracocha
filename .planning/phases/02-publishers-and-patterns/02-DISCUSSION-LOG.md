# Phase 2: Publishers and Patterns - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in 02-CONTEXT.md — this log preserves the Q&A.

**Date:** 2026-03-28
**Phase:** 02-publishers-and-patterns
**Mode:** discuss
**Areas discussed:** Output format, Freemarker extraction scope, Duplicate/not-found handling

---

## Area 1: Output Format

| Question | Options Presented | Selected |
|----------|------------------|----------|
| How should list commands display entries? | Plain text aligned columns / Tabular with headers / You decide | Plain text aligned columns |
| How is machine-readable output triggered? | `--json` flag / `--output jsonl` / You decide | `--json` flag |
| How should show commands display a single entry? | Multi-line key-value block / Same as list row / You decide | Multi-line key-value block |

---

## Area 2: Freemarker Extraction Scope

| Question | Options Presented | Selected |
|----------|------------------|----------|
| What syntax to extract? | `${varName}` top-level only / `${...}` and `<#...>` directives / You decide | `${varName}` top-level only |
| Behavior on malformed template? | Skip silently / Warn but continue / Fail fast | Fail fast — reject registration |

---

## Area 3: Duplicate/Not-Found Handling

| Question | Options Presented | Selected |
|----------|------------------|----------|
| Registering an existing name? | Error + exit 1 / Overwrite silently / You decide | Error + exit 1 |
| Unregistering a non-existent name? | Error + exit 1 / Silent no-op / You decide | Error + exit 1 |

---

## Additional Decision (User-requested)

User added: every implemented task must include at least one JUnit 5 test for verification, before the task is considered complete (carry-forward from Phase 1, made explicit for Phase 2).

---

## Corrections Made

None — all options confirmed as selected.
