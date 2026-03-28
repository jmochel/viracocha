# Requirements: Viracocha

**Defined:** 2026-03-27
**Core Value:** A developer can register patterns and publishers once, then generate a correctly-structured workspace with a single command — and regenerating is safe (skips existing files).

---

## v1 Requirements

### Config

- [x] **CONF-01**: User can run `vira config init` to create the XDG config directory and an empty central `config.yaml`
- [ ] **CONF-02**: User can run `vira config show` to display the current config file path and its contents
- [ ] **CONF-03**: All commands other than `config init` check that config exists and print a clear error if not ("Config not initialized. Run 'vira config init' first.")

### Publisher

- [ ] **PUB-01**: User can run `vira publisher register --name <name> --path <path>` to register a named publisher in central config
- [ ] **PUB-02**: `vira publisher register` validates that the specified path exists on disk before registering
- [ ] **PUB-03**: User can run `vira publisher list` to display all registered publishers (name, path)
- [ ] **PUB-04**: User can run `vira publisher show --name <name>` to display details of a specific publisher
- [ ] **PUB-05**: User can run `vira publisher unregister --name <name>` to remove a publisher from central config

### Pattern

- [ ] **PAT-01**: User can run `vira pattern register --name <name> --path <path>` to register a named pattern in central config
- [ ] **PAT-02**: `vira pattern register` validates that the specified path exists on disk before registering
- [ ] **PAT-03**: `vira pattern register` extracts Freemarker variable names from the pattern source (scanning file content and file/folder name path segments) and stores them in the config entry
- [ ] **PAT-04**: User can run `vira pattern list` to display all registered patterns (name, path, parameter count)
- [ ] **PAT-05**: User can run `vira pattern show --name <name>` to display pattern details including the list of extracted parameter names
- [ ] **PAT-06**: User can run `vira pattern unregister --name <name>` to remove a pattern from central config

### Project

- [ ] **PROJ-01**: User can run `vira project create --name <name> --path <path>` to create a named project entry in central config
- [ ] **PROJ-02**: User can run `vira project list` to display all registered projects (name, workspace path)
- [ ] **PROJ-03**: User can run `vira project add-mapping --project <name> --pattern <pattern-name> --destination <rel-path> [--param key=value ...]` to add a mapping to a project
- [ ] **PROJ-04**: `vira project add-mapping` validates that the referenced pattern exists in central config before adding the mapping
- [ ] **PROJ-05**: User can run `vira project show --name <name>` to display project details including workspace path and all mappings with their parameter values
- [ ] **PROJ-06**: User can run `vira project unregister --name <name>` to remove a project from central config

### Generation

- [ ] **GEN-01**: User can run `vira generate --project-name <name>` to expand all project mappings using Freemarker, writing files to the workspace path
- [ ] **GEN-02**: `vira generate` merges project-level params with mapping-level values (mapping values take precedence) to form the Freemarker data model
- [ ] **GEN-03**: `vira generate` expands Freemarker variables in file/folder name path segments (not only file content)
- [ ] **GEN-04**: `vira generate` creates intermediate directories in the workspace as needed
- [ ] **GEN-05**: `vira generate` skips files that already exist in the destination (skip-existing semantics; never overwrites)
- [ ] **GEN-06**: `vira generate` prints a summary at completion: "Generated: N files, Skipped: M files, Failed: K files"
- [ ] **GEN-07**: User can pass `--dry-run` to `vira generate` to preview what would be written without touching the filesystem
- [ ] **GEN-08**: User can pass `--verbose` to `vira generate` to see each individual file action logged (Created / Skipped / Failed + path)

### Logging

- [x] **LOG-01**: Structured log output is written in JSONL format to `~/.local/share/viracocha/vira.jsonl` (XDG data home)
- [x] **LOG-02**: Log output does not appear on stdout or stderr during normal command execution (stdout reserved for user-facing output; JSONL log goes to file only)

---

## v2 Requirements

### Publisher/Pattern

- **PUB-v2-01**: `vira publisher list` shows subscriber count per publisher
- **PAT-v2-01**: `vira pattern unregister` warns if pattern is referenced by existing project mappings

### Subscriptions

- **SUB-01**: User can add a subscription to a project linking a publisher source path to a workspace destination path
- **SUB-02**: `vira sync --project <name>` performs a one-time copy/sync of subscribed publisher files to workspace
- **SUB-03**: Subscriptions support one-way and bidirectional directionality
- **SUB-04**: `vira sync` respects subscription directionality

### UX

- **UX-01**: Root `--log-level` option controls runtime log verbosity
- **UX-02**: `--force` flag on `vira generate` overwrites existing files (requires explicit opt-in)
- **UX-03**: Meaningful exit codes: 0=success, 1=usage error, 2=config error, 3=generation error

---

## Out of Scope

| Feature | Reason |
|---------|--------|
| Remote publishers (HTTP/Git URLs) | Local paths only in v1; adds significant network/auth complexity |
| Watch mode (background sync daemon) | v2 subscription feature |
| Interactive prompts during generate | Breaks scripting; all values supplied at `add-mapping` time |
| `--force` overwrite on generate | Dangerous without explicit confirmation UX; v2 |
| Shell completion scripts | Nice-to-have; not core to tool value |
| Multiple config profiles | Over-engineering for v1 personal use case |
| GraalVM native image | JVM is acceptable for v1; native adds significant build complexity |
| Config migration tooling | Not needed until schema evolves across versions |

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CONF-01 | Phase 1 | Complete |
| CONF-02 | Phase 1 | Pending |
| CONF-03 | Phase 1 | Pending |
| LOG-01 | Phase 1 | Complete |
| LOG-02 | Phase 1 | Complete |
| PUB-01 | Phase 2 | Pending |
| PUB-02 | Phase 2 | Pending |
| PUB-03 | Phase 2 | Pending |
| PUB-04 | Phase 2 | Pending |
| PUB-05 | Phase 2 | Pending |
| PAT-01 | Phase 2 | Pending |
| PAT-02 | Phase 2 | Pending |
| PAT-03 | Phase 2 | Pending |
| PAT-04 | Phase 2 | Pending |
| PAT-05 | Phase 2 | Pending |
| PAT-06 | Phase 2 | Pending |
| PROJ-01 | Phase 3 | Pending |
| PROJ-02 | Phase 3 | Pending |
| PROJ-03 | Phase 3 | Pending |
| PROJ-04 | Phase 3 | Pending |
| PROJ-05 | Phase 3 | Pending |
| PROJ-06 | Phase 3 | Pending |
| GEN-01 | Phase 4 | Pending |
| GEN-02 | Phase 4 | Pending |
| GEN-03 | Phase 4 | Pending |
| GEN-04 | Phase 4 | Pending |
| GEN-05 | Phase 4 | Pending |
| GEN-06 | Phase 4 | Pending |
| GEN-07 | Phase 4 | Pending |
| GEN-08 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 30 total
- Mapped to phases: 30
- Unmapped: 0

---
*Requirements defined: 2026-03-27*
*Last updated: 2026-03-27 after roadmap creation — all 30 requirements mapped*
