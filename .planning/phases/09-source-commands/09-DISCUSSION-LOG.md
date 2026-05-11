# Phase 9: Source Commands - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-09
**Phase:** 09-source-commands
**Areas discussed:** Path validation strictness, source list output columns, source show parameter display, service layer for source CRUD

---

## Path Validation Strictness

| Option | Description | Selected |
|--------|-------------|----------|
| Validate at add time | Reject non-existent paths immediately with a clear error. Catches typos at registration, not at generate/sync time. | ✓ |
| Register any path, validate lazily | Allow registering non-existent paths; validate at generate/sync time. | |
| Warn but allow | Print a warning if path doesn't exist, but still register. | |

**User's choice:** Validate at add time

---

| Option | Description | Selected |
|--------|-------------|----------|
| Must be existing directory; store absolute path | Clear semantics — sources are directories. Storing absolute path avoids CWD ambiguity. | ✓ |
| Must exist but can be a file; store as-given | More permissive, no strict directory check. | |
| Must be existing directory; store as-given (relative OK) | Allows relative paths — resolution depends on CWD. | |

**User's choice:** Must be an existing directory; store absolute path

---

| Option | Description | Selected |
|--------|-------------|----------|
| Descriptive with guidance | E.g. "Path does not exist: /some/dir" or "Path is not a directory: /some/file". | ✓ |
| Terse | E.g. "Invalid path: /some/dir". | |
| Match existing error style | Mirror wording from SRC-05/06 error patterns. | |

**User's choice:** Descriptive with guidance

---

| Option | Description | Selected |
|--------|-------------|----------|
| That's sufficient | exists + is-directory + no-traversal + absolute path covers all v3 cases. | ✓ |
| Also check readability | Verify Files.isReadable(path) at add time. | |
| Also check not already registered | Reject if same absolute path already in config under a different name. | |

**User's choice:** That's sufficient

---

## Source List Output Columns

| Option | Description | Selected |
|--------|-------------|----------|
| Name + path only | Minimal, consistent with Phase 2 precedent. | ✓ |
| Name + path + templates indicator | E.g. `my-source  /some/dir  [templates]`. | |
| Name + path + templates + param count | Most information-dense. | |

**User's choice:** Name + path only

---

## Source Show Parameter Display

| Option | Description | Selected |
|--------|-------------|----------|
| One per line under Parameters: label | E.g. `Parameters:\n  name\n  email`. Readable for any number. | ✓ |
| Comma-separated on one line | E.g. `Parameters: name, email, role`. | |
| Count only, then one per line | E.g. `Parameters (3):\n  name`. | |

**User's choice:** One per line under a Parameters: label

---

| Option | Description | Selected |
|--------|-------------|----------|
| Omit parameters section entirely | Don't show `Parameters:` at all for non-template sources. | ✓ |
| Show `Parameters: (none)` | Always show the field, explicit when empty. | |
| Show `Templates: false` and omit Parameters | Always show templates flag, omit params when empty. | |

**User's choice:** Omit parameters section entirely

---

| Option | Description | Selected |
|--------|-------------|----------|
| Name / Path / Templates / Parameters (when applicable) | Shows all four SourceEntry fields; Templates as true/false. | ✓ |
| Name / Path / Templates (when true only) / Parameters (when applicable) | Omit `Templates: false` line entirely. | |
| Name / Path only, with parameters block appended | Minimal header then parameter block if templates. | |

**User's choice:** Name / Path / Templates / Parameters (when applicable)

---

## Service Layer for Source CRUD

| Option | Description | Selected |
|--------|-------------|----------|
| SourceService singleton | Thin commands, centralized validation, scales to DestinationService. | ✓ |
| Commands call ConfigService directly | No new layer; matches current InitCommand/ShowConfigCommand style. | |
| You decide | Claude picks based on test simplicity. | |

**User's choice:** SourceService singleton

---

| Option | Description | Selected |
|--------|-------------|----------|
| addSource / listSources / getSource / removeSource | Four methods mapping 1:1 to CLI commands. | ✓ |
| Operations on ViracochaConfig + validation helpers | Service wraps ConfigService but exposes config object. | |
| You decide | Claude picks method signatures based on test simplicity. | |

**User's choice:** addSource / listSources / getSource / removeSource

---

## Claude's Discretion

- Exact picocli annotations for positional NAME vs --name flag
- Whether removeSource returns boolean or throws checked exception
- Column alignment implementation
- Whether addSource returns SourceEntry or void

## Deferred Ideas

None.
