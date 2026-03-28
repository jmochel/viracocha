# Roadmap: Viracocha

## Overview

Build a personal CLI workspace manager (`vira`) from an existing Micronaut + picocli skeleton into a fully functional tool. The journey proceeds bottom-up: establish the config foundation that everything depends on, add publisher and pattern management with Freemarker parameter extraction, layer project and mapping management on top, and finish with the workspace generation engine that delivers the core value. Each phase leaves the tool in a usable, testable state before the next begins.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation** - Config model, ConfigService, command hierarchy scaffold, JSONL logging, exit code wiring (completed 2026-03-28)
- [ ] **Phase 2: Publishers and Patterns** - Register/list/show/unregister for publishers and patterns, Freemarker param extraction
- [ ] **Phase 3: Projects and Mappings** - Project CRUD, mapping addition with pattern validation and param values
- [ ] **Phase 4: Workspace Generation** - PathExpander, GeneratorService, skip-existing, dry-run, verbose, JSONL events

## Phase Details

### Phase 1: Foundation
**Goal**: Users can initialize and inspect a config file, and the full command hierarchy is wired and self-documenting
**Depends on**: Nothing (first phase)
**Requirements**: CONF-01, CONF-02, CONF-03, LOG-01, LOG-02
**Success Criteria** (what must be TRUE):
  1. User can run `vira config init` and see a confirmation with the full path where `config.yaml` was created
  2. User can run `vira config show` to display the config file path and its current contents
  3. Running any non-init command before `vira config init` prints "Config not initialized. Run 'vira config init' first." and exits cleanly
  4. Every subcommand responds to `--help` with correct usage information
  5. Structured log output appears in `~/.local/share/viracocha/vira.jsonl` without polluting stdout or stderr
**Plans**: 2 plans

Plans:
- [x] 01-01-PLAN.md — Add pom.xml deps, replace logback.xml, create XdgPaths + ViracochaConfig model with tests
- [x] 01-02-PLAN.md — Build ConfigService, ConfigNotInitializedException, full command hierarchy, and all integration tests

### Phase 2: Publishers and Patterns
**Goal**: Users can register named publishers and patterns, with Freemarker variables automatically extracted at registration time
**Depends on**: Phase 1
**Requirements**: PUB-01, PUB-02, PUB-03, PUB-04, PUB-05, PAT-01, PAT-02, PAT-03, PAT-04, PAT-05, PAT-06
**Success Criteria** (what must be TRUE):
  1. User can register a publisher by name and path, and it persists across invocations in central config
  2. User can list all registered publishers and see each name and path; unregistering removes it from the list
  3. User can register a pattern, and its Freemarker variable names are automatically extracted and stored without manual enumeration
  4. User can run `vira pattern show --name <name>` and see the list of extracted parameter names alongside the path
  5. Registering a publisher or pattern with a non-existent path prints a clear error and does not modify config
**Plans**: 3 plans

Plans:
- [ ] 02-01-PLAN.md — Add freemarker dep, create PublisherEntry + PatternEntry POJOs, upgrade ViracochaConfig typed lists, wire PublisherCommand + PatternCommand stubs into ViracochaCommand
- [ ] 02-02-PLAN.md — Implement all four publisher leaf commands (register/list/show/unregister) with tests (PUB-01 to PUB-05)
- [ ] 02-03-PLAN.md — Implement FreemarkerVariableExtractor and all four pattern leaf commands with tests (PAT-01 to PAT-06)

### Phase 3: Projects and Mappings
**Goal**: Users can define projects and attach pattern mappings with per-mapping parameter values, fully specifying what a workspace generate will produce
**Depends on**: Phase 2
**Requirements**: PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05, PROJ-06
**Success Criteria** (what must be TRUE):
  1. User can create a project by name and workspace path, and it persists in central config
  2. User can add a mapping to a project referencing an existing pattern, with a destination relative path and optional key=value parameters
  3. Adding a mapping that references a pattern name not in central config prints a clear error and does not modify config
  4. User can run `vira project show --name <name>` and see workspace path plus all mappings with their parameter values
  5. User can unregister a project and it is removed from central config
**Plans**: TBD

### Phase 4: Workspace Generation
**Goal**: Users can generate a correctly-structured workspace from project mappings in a single command, with safe skip-existing semantics and actionable output
**Depends on**: Phase 3
**Requirements**: GEN-01, GEN-02, GEN-03, GEN-04, GEN-05, GEN-06, GEN-07, GEN-08
**Success Criteria** (what must be TRUE):
  1. User can run `vira generate --project-name <name>` and have Freemarker templates expanded into the workspace path, with intermediate directories created automatically
  2. Freemarker variables in file names and folder names are expanded correctly (not left as literal `${varName}`)
  3. Running generate a second time leaves existing files untouched — no overwrites, no errors
  4. User can pass `--dry-run` and see what would be written without any filesystem changes occurring
  5. User can pass `--verbose` to see per-file action lines (Created / Skipped / Failed + path), and a summary line always appears ("Generated: N files, Skipped: M files, Failed: K files")
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 2/2 | Complete   | 2026-03-28 |
| 2. Publishers and Patterns | 0/3 | Not started | - |
| 3. Projects and Mappings | 0/? | Not started | - |
| 4. Workspace Generation | 0/? | Not started | - |
