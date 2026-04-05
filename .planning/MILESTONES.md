# Milestones

## v1.0 MVP (Shipped: 2026-04-04)

**Phases completed:** 4 phases, 11 plans, 17 tasks

**Key accomplishments:**

- jackson-dataformat-yaml + logstash-logback-encoder wired to pom.xml, JSONL-only logback, XdgPaths XDG utility, and ViracochaConfig round-trip YAML POJO — 6 tests pass
- ConfigService (init/load/save over XDG YAML), full picocli command hierarchy (vira -> config -> init|show), and 17 passing tests using @Spec-based testable output
- Freemarker 2.3.34 added to classpath, typed CatalogEntry/PatternEntry POJOs created, ViracochaConfig upgraded to typed lists, and publisher/pattern group command stubs wired into the full subcommand hierarchy
- All four catalog leaf commands implemented with tests: register (path + duplicate validation), list (plain + JSON), show (plain + JSON), unregister — PUB-01 through PUB-05.
- FreemarkerVariableExtractor walks pattern trees (content + path segments), enforces D-05 malformed `${`, skips hidden subtrees; pattern commands register/list/show/unregister with extracted parameters persisted in YAML.
- Typed `projects` in config and full `vira project` command tree implemented in one pass with plans 02–03.
- `project create`, `list`, and `unregister` with ConfigService persistence and `ProjectCommandsTest` coverage.
- `project add-mapping` with repeatable `--param key=value`, `project show` (plain + JSON), and integration tests.
- Project-level default parameters on `ProjectEntry` plus a Freemarker-backed `PathExpander` for path segments, with YAML round-trip and unit tests.
- GeneratorService walks pattern trees (skipping hidden segments), merges project and mapping parameters, expands path segments and template bodies, supports dry-run and skip-existing, and returns aggregate counts plus optional verbose action lines.
- `vira generate` delegates to GeneratorService, prints workspace-relative verbose actions when requested, always prints the summary line, and includes integration tests for guard rails and flags.

---
