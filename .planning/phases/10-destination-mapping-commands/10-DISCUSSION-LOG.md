# Phase 10: Destination & Mapping Commands - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-09
**Phase:** 10-destination-mapping-commands
**Areas discussed:** destination show format, path validation, add-mapping flags, DestinationCommand alias

---

## destination show format

| Option | Description | Selected |
|--------|-------------|----------|
| Numbered block per mapping | Each mapping gets a numbered section showing all its fields | ✓ |
| Compact one-liner per mapping | Each mapping on one line: [0] source glob recurse=true sync=false | |

**User's choice:** Numbered block per mapping

| Option | Description | Selected |
|--------|-------------|----------|
| key: value lines indented | Matches source show Parameters: block style; omit if empty | ✓ |
| key=value lines indented | Shell-variable style | |

**User's choice:** key: value lines indented (omit Parameters section if map is empty)

| Option | Description | Selected |
|--------|-------------|----------|
| Omit the Glob line entirely | Only show non-default fields | |
| Show Glob: (all files) | Always show all fields with human-readable null indicator | ✓ |

**User's choice:** Show `Glob: (all files)` when glob is null — always show all mapping fields

| Option | Description | Selected |
|--------|-------------|----------|
| Show 'Mappings: (none)' on one line | Explicit and consistent | ✓ |
| Omit the Mappings section entirely | Cleaner when empty | |

**User's choice:** Show `Mappings: (none)` when no mappings exist

---

## Path Validation

| Option | Description | Selected |
|--------|-------------|----------|
| Only reject '..' traversal — no existence check | Destination paths may not exist yet at registration time | ✓ |
| Validate existence at add time | Consistent with source add; fails fast on typos | |

**User's choice:** Only `..` traversal rejection — no existence check at registration time

---

## add-mapping Interface

| Option | Description | Selected |
|--------|-------------|----------|
| Positional DEST-NAME + --source required | vira destination add-mapping DEST-NAME --source SOURCE-NAME [...] | ✓ |
| All named options: --destination + --source both required | vira destination add-mapping --destination DEST-NAME --source SOURCE-NAME [...] | |

**User's choice:** Positional DEST-NAME + `--source` required

| Option | Description | Selected |
|--------|-------------|----------|
| "Mapping added to destination 'NAME'." | Simple, consistent with source add | ✓ |
| "Mapping [N] added to destination 'NAME'." with index | Shows index immediately for scripting | |

**User's choice:** `"Mapping added to destination 'NAME'."` — no index in success message

---

## DestinationCommand Alias

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — alias 'dest' | Consistent with SourceCommand alias 'src' | ✓ |
| No alias | Keep 'destination' explicit | |

**User's choice:** Alias `dest`

---

## Claude's Discretion

- GlobMatcher placement: infra/ (consistent with other general-purpose utilities)
- GlobMatcher API design: method signature chosen by implementer
- DestinationService method return types: chosen to simplify tests
- MappingEntry.params field: not exposed via --params flag in add-mapping (per-mapping param overrides are out of scope in v3)

## Deferred Ideas

None.
