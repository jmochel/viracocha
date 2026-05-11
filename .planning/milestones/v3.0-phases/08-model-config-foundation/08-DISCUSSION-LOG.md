# Phase 8: Model & Config Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-08
**Phase:** 08-model-config-foundation
**Areas discussed:** Utility relocation, SourceEntry.parameters field, MappingEntry defaults

---

## Utility Relocation

| Option | Description | Selected |
|--------|-------------|----------|
| Move both to infra/ now | ArchetypePathUtils → infra/HiddenPathFilter, FreemarkerVariableExtractor → infra/. Update service imports. | ✓ |
| Move ArchetypePathUtils only, delete FreemarkerVariableExtractor | Fix compile dependency; let Phase 9 recreate extractor | |
| Stub out GeneratorService and DefaultSyncService | Remove bodies so archetype/ can be cleanly deleted | |

**User's choice:** Move both to `infra/` now
**Follow-up:** Rename `ArchetypePathUtils` → `HiddenPathFilter` — confirmed yes (better reflects purpose)

---

## SourceEntry.parameters Field Type

| Option | Description | Selected |
|--------|-------------|----------|
| List\<String\> — variable names only | Matches current ArchetypeEntry pattern; extractor returns names | ✓ |
| Map\<String, String\> — names + default values | Richer but mixes 'what variables exist' with 'what values to use' | |

**User's choice:** `List<String>` — variable names only

---

## MappingEntry Defaults

| Option | Description | Selected |
|--------|-------------|----------|
| recurse: true (walk all subdirs) | Matches current generate behavior | |
| recurse: false (flat copy, opt-in) | Explicit is better | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| glob: null (no filter, select all) | Simplest case, most common | ✓ |
| glob: "**/*" (explicit match-all) | Self-documenting in YAML | |

**User's choices:** `recurse: false` (opt-in to recursion), `glob: null` (no filter = all files)
**Notes:** `sync: false` was established as default during milestone planning — not re-discussed.

---

## Claude's Discretion

- Version guard implementation detail (JsonNode peek vs. minimal POJO)
- Whether ConfigVersionException is a new class or extends ConfigNotInitializedException
- Exact placement of utilities within infra/

## Deferred Ideas

None.
