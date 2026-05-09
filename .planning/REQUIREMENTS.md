# Requirements: Viracocha

**Defined:** 2026-05-08
**Core Value:** A developer registers sources and destinations once, then populates any workspace with a single command — and regeneration is safe (skips existing files). Mappings with `sync: true` keep destination copies up to date on demand via `vira sync`.

## v3.0 Requirements

Breaking redesign: replace the catalog/archetype/project/subscription model with a unified sources/destinations schema. Local filesystem only; remote HTTP sources deferred to v4.

### Config

- [x] **CFG-01**: v3 config POJOs — `SourceEntry` (name, path, templates boolean, parameters), `DestinationEntry` (name, path, parameters, mappings), `MappingEntry` v3 (sourceRef, glob, recurse, sync, params) — with YAML round-trip and no data loss
- [x] **CFG-02**: `ConfigService.load()` detects a v2 config (missing version field or version < 3) and fails with a clear error message instructing the user to recreate their config
- [x] **CFG-03**: All v2 CLI command packages (catalog, archetype, project, subscription) are removed from the codebase; running old command names produces "unknown command" error

### Source

- [ ] **SRC-01**: User can add a named local directory source with `--name`, `--path`, and optional `--templates` flag (`vira source add`)
- [ ] **SRC-02**: User can list all registered sources in plain or JSON output (`vira source list [--json]`)
- [ ] **SRC-03**: User can view a source's full details — name, path, templates flag, and extracted parameter names (`vira source show NAME`)
- [ ] **SRC-04**: User can remove a source by name (`vira source remove NAME`)
- [ ] **SRC-05**: Source add rejects duplicate source names with a clear error
- [ ] **SRC-06**: Source add rejects paths containing `..` directory traversal sequences
- [ ] **SRC-07**: Source add with `--templates` extracts Freemarker variable names from all template files in the source directory and persists them in config

### Destination

- [ ] **DEST-01**: User can add a named destination workspace path with `--name` and `--path` (`vira destination add`)
- [ ] **DEST-02**: User can list all registered destinations in plain or JSON output (`vira destination list [--json]`)
- [ ] **DEST-03**: User can view a destination's full details — name, path, parameters, and all mappings (`vira destination show NAME`)
- [ ] **DEST-04**: User can remove a destination by name (`vira destination remove NAME`)
- [ ] **DEST-05**: Destination add rejects duplicate destination names with a clear error
- [ ] **DEST-06**: Destination add rejects paths containing `..` directory traversal sequences

### Mapping

- [ ] **MAP-01**: User can add a mapping to a destination specifying a source name, optional glob pattern, recurse flag, and sync flag (`vira destination add-mapping`)
- [ ] **MAP-02**: User can list all mappings for a destination (`vira destination list-mappings NAME`)
- [ ] **MAP-03**: User can remove a mapping from a destination by index (`vira destination remove-mapping NAME INDEX`)
- [ ] **MAP-04**: Mapping add validates that the referenced source name exists in the config and rejects unknown source references
- [ ] **MAP-05**: `GlobMatcher` utility wraps JDK `FileSystem.getPathMatcher` with `glob:` / `regex:` prefix support; unit tests verify `+` is treated as a literal character (not a quantifier) in glob patterns

### Generate

- [ ] **GEN-01**: `vira generate` traverses destinations → mappings → sources, applies glob/recurse filtering, and writes files to the destination path
- [ ] **GEN-02**: `vira generate` skips destination files that already exist (skip-existing semantics preserved from v2)
- [ ] **GEN-03**: `vira generate` expands Freemarker templates in both path segments and file content for sources with `templates: true`
- [ ] **GEN-04**: `vira generate` uses binary byte copy (not string read) for sources with `templates: false`, preserving non-text files
- [ ] **GEN-05**: `vira generate` accepts `--destination-name` to target a single destination
- [ ] **GEN-06**: `vira generate` supports `--dry-run` (reports actions without writing files)
- [ ] **GEN-07**: `vira generate` supports `--verbose` (prints per-file action lines)

### Sync

- [ ] **SYN-01**: `vira sync` copies changed source files to the destination for all mappings with `sync: true`
- [ ] **SYN-02**: `vira sync` detects conflicts (destination file content differs from source) and aborts with exit 1
- [ ] **SYN-03**: `vira sync` accepts `--destination-name` to target a single destination
- [ ] **SYN-04**: `vira sync` supports `--dry-run`
- [ ] **SYN-05**: `vira sync` supports `--verbose`
- [ ] **SYN-06**: `vira sync` supports `--json` for machine-readable output
- [ ] **SYN-07**: `vira sync` prints a summary line reporting copied/skipped/failed/conflict counts

## Future Requirements (v4+)

### Remote Sources

- **REM-01**: User can add a remote http(s) URL as a source
- **REM-02**: `vira generate` / `vira sync` fetches remote source content at runtime (fetch-and-copy, no local cache)
- **REM-03**: Remote source mappings are limited to literal path globs (no wildcards); `recurse: true` rejected for remote sources

### Config Migration

- **MIG-01**: `vira config migrate` command converts a v2 config file to v3 format (with backup)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Config auto-migration from v2 | Clean break — v3 is a fresh start; fail-with-instructions is sufficient |
| Bidirectional sync | Semantically undefined with "source is canonical" model; removed with subscriptions |
| Watch mode / background sync | One-shot `vira sync` covers the use case; daemon adds OS complexity |
| Remote write operations | Sources are always read-only; write operations stay on local filesystem |
| Remote source authentication | Scope creep; use SHA-pinned public URLs instead |
| Per-mapping parameter overrides | Added v2 complexity that rarely earned its keep; destination-level params are sufficient |
| Multiple config profiles | Single XDG config path is sufficient |
| GraalVM native image | Profile exists in pom.xml; not a v3 target |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CFG-01 | Phase 8 | Complete |
| CFG-02 | Phase 8 | Complete |
| CFG-03 | Phase 8 | Complete |
| SRC-01 | Phase 9 | Pending |
| SRC-02 | Phase 9 | Pending |
| SRC-03 | Phase 9 | Pending |
| SRC-04 | Phase 9 | Pending |
| SRC-05 | Phase 9 | Pending |
| SRC-06 | Phase 9 | Pending |
| SRC-07 | Phase 9 | Pending |
| DEST-01 | Phase 10 | Pending |
| DEST-02 | Phase 10 | Pending |
| DEST-03 | Phase 10 | Pending |
| DEST-04 | Phase 10 | Pending |
| DEST-05 | Phase 10 | Pending |
| DEST-06 | Phase 10 | Pending |
| MAP-01 | Phase 10 | Pending |
| MAP-02 | Phase 10 | Pending |
| MAP-03 | Phase 10 | Pending |
| MAP-04 | Phase 10 | Pending |
| MAP-05 | Phase 10 | Pending |
| GEN-01 | Phase 11 | Pending |
| GEN-02 | Phase 11 | Pending |
| GEN-03 | Phase 11 | Pending |
| GEN-04 | Phase 11 | Pending |
| GEN-05 | Phase 11 | Pending |
| GEN-06 | Phase 11 | Pending |
| GEN-07 | Phase 11 | Pending |
| SYN-01 | Phase 12 | Pending |
| SYN-02 | Phase 12 | Pending |
| SYN-03 | Phase 12 | Pending |
| SYN-04 | Phase 12 | Pending |
| SYN-05 | Phase 12 | Pending |
| SYN-06 | Phase 12 | Pending |
| SYN-07 | Phase 12 | Pending |

**Coverage:**
- v3.0 requirements: 35 total
- Mapped to phases: 35
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-08*
*Last updated: 2026-05-08 — traceability filled after roadmap creation*
