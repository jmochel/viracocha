# Research Summary: Viracocha

**Project:** Viracocha (`vira`)
**Domain:** Java CLI workspace manager — Freemarker-based scaffolding tool for AI-assisted dev workflows
**Researched:** 2026-03-27
**Confidence:** MEDIUM-HIGH (stack confirmed from pom.xml; patterns from domain expertise)

## Executive Summary

Viracocha is a personal CLI workspace manager that falls into the well-understood category of scaffolding/template-expansion tools (think cookiecutter, maven archetypes), but implemented in Java with a specific opinionated stack. The existing project skeleton provides a solid foundation: Micronaut 4.10.10 + picocli is already wired and compiles. The core build challenge is not architectural novelty — the layered service design is conventional — but rather the careful sequencing of three mutually dependent concerns: config persistence (YAML), template variable extraction (Freemarker), and filesystem generation (path expansion + skip-existing semantics).

The recommended approach is a strict bottom-up build order: config model and ConfigService must exist and be tested before any other component is written, because every command depends on it. Catalog and Pattern management follow as independent leaf services. Project/Mapping management sits above them (references both). Generation is the capstone phase and is the highest-risk component because it combines Freemarker template expansion, path variable substitution (which Freemarker does NOT handle automatically), and atomic skip-existing file writes.

The top risk is silent corruption in the generation phase: Freemarker does not expand variables in file or directory names — the caller must do this explicitly via a path-expansion utility. This pitfall has no compile-time or runtime warning; generated files silently appear at the wrong path. A secondary risk is the three missing pom.xml dependencies (`freemarker`, `jackson-dataformat-yaml`, `logstash-logback-encoder`) — no production code should be written in phases 2+ until these are added.

---

## Key Findings

- **Three dependencies are missing from pom.xml.** `org.freemarker:freemarker:2.3.33`, `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` (BOM-managed), and `net.logstash.logback:logstash-logback-encoder:7.4` must be added before any feature work in phases 2+. Adding them is the very first task of Phase 1.
- **ConfigService is the critical path blocker.** Every command loads config at startup. Nothing else can be meaningfully implemented or tested until `ConfigService` (load/save/init) and the config model POJOs exist with tests.
- **Freemarker does not expand path variables — this must be hand-rolled.** A `PathExpander` utility must be built and tested independently before any file-writing code in the generation phase.
- **Picocli subcommand wiring is all-or-nothing at annotation declaration time.** The full `@Command(subcommands={...})` hierarchy — root → group → leaf — must be declared correctly before any runtime testing. Dynamic registration does not work with `PicocliRunner`.
- **Exit codes require `Callable<Integer>`, not `Runnable`.** The `PicocliRunner` contract discards return values from `Runnable` commands. All command classes must implement `Callable<Integer>` from phase 1 to get correct exit code semantics throughout.

---

## Stack Decisions

### Confirmed (present in pom.xml)

| Component | Version | Notes |
|-----------|---------|-------|
| JDK | 21 | Required — do not downgrade |
| Micronaut BOM | 4.10.10 | Manages picocli, Logback, Lombok, SnakeYAML, Jackson versions |
| picocli | BOM-managed | Via `micronaut-picocli` |
| Logback | BOM-managed | Via `micronaut-logging` |
| Lombok | BOM-managed | Annotation processor order already correct — do not reorder |
| SnakeYAML | transitive | Pulled by Micronaut core; do not use directly — use Jackson YAML |

### Missing — Must Add to pom.xml in Phase 1

| Component | Artifact | Version | Why Needed |
|-----------|----------|---------|------------|
| Freemarker | `org.freemarker:freemarker` | `2.3.33` (explicit — not in BOM) | Template expansion and variable extraction |
| Jackson YAML | `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | BOM-managed (omit version) | ObjectMapper-based config read/write; SnakeYAML alone is insufficient |
| Logstash Logback | `net.logstash.logback:logstash-logback-encoder` | `7.4` (explicit — not in BOM) | JSONL structured log output |

### What Not to Deviate From

- Do not use Micronaut `@ConfigurationProperties` or `@Value` for the central `config.yaml` — that file is user domain data, not application config.
- Do not share a single `FileTemplateLoader` across multiple pattern directories — one Freemarker `Configuration` per pattern source path.
- Do not pursue GraalVM native image in v1 — reflection-heavy dependencies (Jackson, Freemarker, Logback XML) require substantial `@Introspected` annotation work that is out of scope.

---

## Architecture Blueprint

### Component Layers and Build Order

```
infra/           XdgPaths, FreemarkerFactory       — cross-cutting utilities
model/           ViracochaConfig, *Entry POJOs     — pure data; no logic
config/          ConfigService + InitCommand        — CRITICAL PATH BLOCKER
catalog/       CatalogService + commands        — depends on ConfigService
pattern/         PatternService + commands          — depends on ConfigService + Freemarker
project/         ProjectService + commands          — depends on ConfigService
generate/        GeneratorService + GenerateCommand — depends on all above
```

### Component Responsibilities

| Component | Package | Responsibility |
|-----------|---------|----------------|
| Command Layer | `org.saltations.*Command` | Thin CLI dispatchers — parse args, call services, print to stdout |
| Config Service | `org.saltations.config` | Read/write/init central YAML config; never cached across invocations |
| Catalog Service | `org.saltations.catalog` | Register and list named catalog entries in config |
| Pattern Service | `org.saltations.pattern` | Register/list patterns; extract Freemarker variable names at registration |
| Project Service | `org.saltations.project` | Create/list projects; add/list mappings with per-mapping param values |
| Generator Service | `org.saltations.generate` | Expand pattern templates to workspace; path expansion; skip-existing logic |
| Model | `org.saltations.model` | POJOs for YAML schema (`ViracochaConfig`, `CatalogEntry`, `PatternEntry`, `ProjectEntry`, `MappingEntry`) |
| Infrastructure | `org.saltations.infra` | `XdgPaths` (config path resolution), `FreemarkerFactory` (per-pattern config), `PathExpander` (path variable substitution) |

### Key Data Flow: `vira generate`

```
GenerateCommand.run()
  → ConfigService.load()                   // fresh load every invocation
  → ProjectService.findProject(name)
  → for each mapping:
      → PatternService.findPattern(name)
      → merge params (project.params + mapping.values)
      → GeneratorService.generate(pattern, workspacePath, params)
          → PathExpander.expand(relPath, params)   // MUST happen before any File I/O
          → walk pattern source tree
          → for each file:
              → expand path segments via PathExpander
              → Files.newOutputStream(dest, CREATE_NEW)   // atomic skip-existing
              → expand content via Freemarker Template.process()
  → print summary: "N created, M skipped, K failed"
```

### Phase-to-Component Mapping

| Phase | Components Built |
|-------|-----------------|
| Phase 1: Foundation | model POJOs, XdgPaths, ConfigService, InitCommand, pom.xml deps, Logback config, exit code wiring |
| Phase 2: Publishers + Patterns | CatalogService, catalog commands, FreemarkerFactory, PatternService, pattern commands |
| Phase 3: Projects + Mappings | ProjectService, project commands, AddMappingCommand with param validation |
| Phase 4: Generation | PathExpander, GeneratorService, GenerateCommand, JSONL log events, dry-run flag |

---

## Critical Warnings

### W1: Freemarker Does Not Expand Path Variables (affects Phase 4)
Freemarker's `Template.process()` writes expanded content to a `Writer` — it has no awareness of output paths in the workspace. A pattern source tree with a directory named `${projectName}` will generate a literal directory named `${projectName}` in the workspace unless the caller explicitly expands it first. **Mitigation:** Implement `PathExpander` as the first task of Phase 4 and test it independently with unit tests before writing any file-output code.

### W2: All Three Missing Dependencies Must Be Added Before Phase 2 (affects Phase 1)
`freemarker`, `jackson-dataformat-yaml`, and `logstash-logback-encoder` are absent from pom.xml. Any code referencing these classes will produce `ClassNotFoundException` at runtime. **Mitigation:** Add all three as the opening task of Phase 1 before any other code is written. Verify with a clean `mvn compile`.

### W3: Subcommand Hierarchy Must Be Fully Declared Before Any Runtime Test (affects Phase 1)
`PicocliRunner` only sees subcommands declared statically in `@Command(subcommands={...})`. Dynamic `addSubcommand()` calls are invisible to the runner. Partially-declared hierarchies fail silently — `vira --help` shows no subcommands and invocations throw `UnmatchedArgumentException`. **Mitigation:** Declare the full two-level command hierarchy (root → group → leaf stubs) as the first code task, even if commands are no-ops.

### W4: Config YAML Schema Nulls Propagate Silently Into Generation (affects Phases 1 + 4)
Jackson YAML silently nulls any field absent from the config file, including `catalogs`, `patterns`, and `projects` lists. Null lists crash generation or silently produce literal `${varName}` in output. **Mitigation:** Implement a `ConfigValidator` that checks required fields immediately after deserialization. Annotate list fields with `@JsonSetter(nulls = Nulls.AS_EMPTY)` or initialize in the no-arg constructor.

### W5: Skip-Existing Uses `EXISTS` Check — Must Use `CREATE_NEW` Instead (affects Phase 4)
`Files.exists(dest)` followed by `Files.write(dest)` is a TOCTOU race. More critically, it can produce incorrect behavior in generation loops. **Mitigation:** Use `Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)` and catch `FileAlreadyExistsException` as the skip signal. This is also the correct semantic for the idempotent-generate contract.

---

## Feature Scope Confirmation

### Table Stakes (must ship in v1)

| Feature | Phase | Notes |
|---------|-------|-------|
| `--help` on every subcommand | 1 | `mixinStandardHelpOptions = true` on every `@Command` |
| `--version` at root | 1 | Inline version string in root command |
| Meaningful exit codes (0/1/2) | 1 | Requires `Callable<Integer>` on all commands |
| Actionable error messages to stderr | 1 | `IExecutionExceptionHandler`; no stack traces to users |
| Gate on config init (all commands) | 1 | `ConfigNotInitializedException` → friendly message |
| Empty-state messages on list commands | 2-3 | "No catalogs registered. Run 'vira catalog register'." |
| Confirmation line on every write | 2-3 | "Registered catalog 'x' at /path" |
| Config path transparency on init | 1 | Print full path where config.yaml was created |
| Idempotent generate with skip logging | 4 | Log each skipped file: "Skipped (exists): path/to/file" |
| Generate summary report | 4 | "N files created, M skipped, K failed" |

### Differentiators (high value, ship in v1)

| Feature | Phase | Notes |
|---------|-------|-------|
| Parameter extraction at register time | 2 | Scan Freemarker content at `pattern register`; document regex scope limit |
| `pattern show` with param list | 2 | Displays extracted parameters — unique UX value |
| File and folder name templating | 4 | `PathExpander` — uncommon in scaffolding tools, core to this use case |
| JSONL structured logging | 4 | Machine-readable output; Logback to file, not stdout |
| Validation at `add-mapping` time | 3 | Verify referenced pattern exists and all required params supplied |
| `--dry-run` on generate | 4 | Shows what would be written without touching filesystem |
| Verbose generate output | 4 | Per-file action log (created/skipped/error) |

### Deferred to v2+

| Feature | Reason |
|---------|--------|
| `vira sync` / subscriptions | Requires sync semantics, conflict resolution — out of scope |
| Watch mode | Background process complexity; not needed for core use case |
| Remote catalogs (HTTP/Git) | Local paths only in v1 |
| Interactive prompts during generate | Breaks scripting; all values supplied upfront |
| `--force` overwrite on generate | Dangerous; skip-existing is the v1 contract |
| Shell completion scripts | Nice-to-have after core is stable |
| GraalVM native image | Significant reflection annotation work; defer post-v1 |

---

## Recommended Phase Order

### Ordering Rationale

The phase order is determined by hard dependency chains:

1. **Config must come first.** Every command, service, and test fixture depends on `ConfigService`. It is the only component with zero dependencies on other project code. Nothing else can be tested end-to-end until it exists.

2. **Catalogs and Patterns are parallel but pattern depends on Freemarker.** Both read/write config. Catalogs are simpler (no external library). Patterns add Freemarker parameter extraction. Build together but implement catalog first as the simpler warm-up.

3. **Projects depend on patterns and catalogs.** A `MappingEntry` references a pattern by name, so pattern registration must exist before mapping addition makes sense. Project CRUD can be scaffolded earlier, but `add-mapping` validation requires Phase 2 to complete first.

4. **Generation is the riskiest phase and depends on everything.** It combines Freemarker template expansion, path variable substitution, atomic file writes, and structured logging. The `PathExpander` pitfall (W1) means the utility must be built and tested before any file I/O code is written.

### Suggested Phase Sequence

**Phase 1: Foundation**
Deliver a compiling, testable skeleton with correct command hierarchy, config YAML round-trip, XDG path resolution, JSONL logging wired to file, and exit code semantics in place. Add missing pom.xml dependencies. Every subsequent phase builds on this base.
- Must avoid: W2 (missing deps), W3 (subcommand hierarchy), W4 (config null fields)
- Needs research: No — patterns are standard
- Key outputs: `ViracochaConfig` POJO, `ConfigService`, `InitCommand`, `XdgPaths`, full subcommand stub hierarchy, `logback.xml`

**Phase 2: Catalogs and Patterns**
Deliver working register/list for both catalogs and patterns, including Freemarker parameter extraction at registration time. Users can populate config with real data after this phase.
- Must avoid: M1 (ClassPathTemplateLoader for filesystem templates), M2 (regex extraction gaps — document limitation)
- Needs research: No — standard CRUD + regex extraction
- Key outputs: `CatalogService`, `PatternService`, `FreemarkerFactory`, all register/list commands

**Phase 3: Projects and Mappings**
Deliver project creation and mapping addition with validation (referenced pattern exists, required params present). After this phase users can fully specify what to generate.
- Must avoid: M3 (exit code handling — must be consistent by now)
- Needs research: No — CRUD with validation is standard
- Key outputs: `ProjectService`, all project/mapping commands, mapping validation logic

**Phase 4: Workspace Generation**
Deliver the `vira generate` command with `PathExpander`, skip-existing semantics, dry-run, verbose output, JSONL per-file events, and generation summary. This is the highest-risk phase.
- Must avoid: W1 (path expansion), W5 (TOCTOU/CREATE_NEW), M8 (partial failure summary)
- Needs research: Consider `/gsd:research-phase` for Freemarker AST-based variable extraction if regex approach proves insufficient during Phase 2
- Key outputs: `PathExpander`, `GeneratorService`, `GenerateCommand`, JSONL event schema

### Research Flags

- **Phase 4:** Flag for deeper Freemarker path expansion research if `PathExpander` proves more complex than a simple string-replace (e.g., nested expressions, special characters in paths).
- **All phases:** Exit code semantics are well-documented in picocli — no additional research needed.
- **Phase 1:** GraalVM native image is explicitly deferred — do not research or prototype this in v1.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Confirmed directly from pom.xml; missing deps identified by name and version |
| Features | HIGH | Requirements in PROJECT.md are specific and cross-validated with comparable tools |
| Architecture | HIGH | Component decomposition is conventional; data flow is fully specified in ARCHITECTURE.md |
| Pitfalls | MEDIUM | Domain expertise; Freemarker/picocli/Micronaut interaction patterns are well-understood but external validation unavailable |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Logstash encoder compatibility:** `logstash-logback-encoder:7.4` compatibility with the Logback version pulled by Micronaut BOM 4.10.10 should be verified at the start of Phase 1 before writing any logging code. If incompatible, version 8.x may be needed.
- **Freemarker variable extraction completeness:** The regex approach (`\$\{([^}?!:]+)[^}]*\}`) covers simple `${varName}` and `${varName?default("")}` expressions but misses `<#if varName?has_content>` and iteration variables. This is documented as acceptable for v1 — validate the scope with a representative real pattern before finalizing Phase 2.
- **Micronaut native image configuration:** The skeleton already includes `micronaut-graal` annotation processor. Confirm it does not force native compilation in the default Maven lifecycle — if it does, move it to a separate Maven profile to avoid build complexity.

---

## Sources

### Primary (HIGH confidence)
- `pom.xml` (project file) — all confirmed stack versions, existing dependencies, annotation processor order
- `.planning/PROJECT.md` — authoritative requirements, constraints, and out-of-scope decisions

### Secondary (MEDIUM confidence)
- `.planning/research/STACK.md` — integration patterns, anti-patterns, XDG path resolution snippet
- `.planning/research/ARCHITECTURE.md` — component structure, data flow walkthrough, build order rationale
- `.planning/research/FEATURES.md` — table stakes, differentiators, UX conventions from comparable tools
- `.planning/research/PITFALLS.md` — critical pitfalls with prevention strategies and phase-specific warnings

---
*Research completed: 2026-03-27*
*Ready for roadmap: yes*
