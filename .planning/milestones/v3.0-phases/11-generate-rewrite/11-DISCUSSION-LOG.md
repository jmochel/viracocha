# Phase 11: Generate Rewrite - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-10
**Phase:** 11-generate-rewrite
**Areas discussed:** Glob pattern matching scope, Missing destination directory

---

## Glob pattern matching scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full relative path from source root | Pass relative path (e.g. `docs/api/readme.md`) to GlobMatcher — enables `**` patterns | ✓ |
| Filename only | Pass just the filename — simpler, but `**` prefixes have no effect | |
| You decide | Whatever is cleanest given the JDK PathMatcher API | |

**User's choice:** Full relative path from source root

---

### Follow-up: glob + recurse=false

| Option | Description | Selected |
|--------|-------------|----------|
| Top-level files only, matched against filename | Shallow walk; glob applied to filename (identical to relative path at depth 0) | ✓ |
| Top-level files only, matched against full relative path | Same outcome, more consistent API | |

**User's choice:** Top-level files matched against filename

**Notes:** For recurse=false, relative path of any top-level file IS its filename — both approaches produce identical results. Implementation uses relative path consistently.

---

### Follow-up: hidden file filtering

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, keep hidden file filtering | Apply `HiddenPathFilter.hasHiddenPathSegment()` during source walks — consistent with v1/v2 | ✓ |
| No, copy everything including hidden | Simpler — let glob do all filtering | |
| You decide | | |

**User's choice:** Yes, keep hidden file filtering (consistent with prior behavior)

---

## Missing destination directory

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-create destination directory | `Files.createDirectories()` before writing — required for new workspaces | |
| Prompt user for top-level directory creation | Y/N prompt; only top-level triggers confirmation; subdirs auto-created | ✓ |
| Fail with a clear error message | Print error, exit 1 | |

**User's choice:** Prompt user — "Should pause and have the user confirm that it should create a new top-level destination folder (all folders below the root destination directory are automatically created)"

---

### Follow-up: confirmation prompt format

| Option | Description | Selected |
|--------|-------------|----------|
| Y/N prompt to stdout | `"Destination /path does not exist. Create it? [y/N]"` | ✓ |
| --no-create-dest flag | Prompt is shown by default; flag suppresses prompt and fails instead | |

**User's choice:** Y/N prompt to stdout

---

### Follow-up: dry-run behavior for missing destination

| Option | Description | Selected |
|--------|-------------|----------|
| Skip prompt, report 'Would create: <path>', continue | Dry-run previews actions without interactive prompts | ✓ |
| Still prompt even in dry-run | Consistent behavior | |

**User's choice:** Skip the confirmation prompt; report `"Would create: <path>"` and continue

---

### Follow-up: exit code when user declines

| Option | Description | Selected |
|--------|-------------|----------|
| Exit 0 — user chose not to proceed, not an error | Clean cancellation | ✓ |
| Exit 1 — destination not found is a failure | Allows scripting to distinguish cancellation from success | |

**User's choice:** Exit 0

---

### Follow-up: sub-directory creation

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-create sub-directories silently | Only top-level triggers prompt; everything below auto-created | ✓ |
| Prompt for each new sub-directory | Too noisy | |

**User's choice:** Auto-create sub-directories silently (Recommended)

---

## Claude's Discretion

- Freemarker Configuration lifecycle (new per run or shared singleton)
- Exact file sort order during traversal
- Whether dry-run summary uses `"Generated:"` or `"Would generate:"` format
- Exception wrapping strategy inside the traversal loop

## Deferred Ideas

- `--dest` as optional (generate all destinations when omitted) — reviewed, not selected; current required behavior preserved
- Per-mapping parameter overrides — out of scope per REQUIREMENTS.md Out of Scope table
