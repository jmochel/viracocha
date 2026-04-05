# Phase 1 Context: Foundation

**Phase:** 1 — Foundation
**Requirements:** CONF-01, CONF-02, CONF-03, LOG-01, LOG-02
**Created:** 2026-03-28

---

## Locked Decisions

### Re-init behavior (CONF-01)
`vira config init` when config already exists:
- Print: `"Config already initialized at <path>"` and exit 0
- Idempotent — safe to run repeatedly, never errors on existing config

### config show format (CONF-02)
`vira config show` output:
- Line 1: `Config file: <absolute-path>`
- Blank line
- Raw YAML dump of file contents

Example:
```
Config file: /home/user/.config/viracocha/config.yaml

version: 1
publishers: []
patterns: []
projects: []
```

### Exit codes — Callable<Integer> (CONF-03 + all commands)
- All command classes implement `Callable<Integer>`, not `Runnable`
- Wire this in Phase 1 so all subsequent phases inherit the pattern
- `main()` uses `System.exit(commandLine.execute(args))`
- Standard codes: 0 = success, 1 = usage/config error
- CONF-03 ("Config not initialized") exits 1

### JUnit tests per task
- Every implemented task must have at least one JUnit 5 test verifying its behavior
- Tests must pass before a task is considered complete
- Use `@MicronautTest` for integration tests; plain JUnit for pure-logic units (e.g., `XdgPaths`, model POJOs)
- Test fixture config paths must point to `@TempDir` directories — never the real XDG config

---

## Phase Scope

Phase 1 delivers exactly:
1. `XdgPaths` utility — resolves `XDG_CONFIG_HOME` with `~/.config` fallback
2. Config model POJOs (`ViracochaConfig`) — empty shell, Lombok `@Data`, Jackson YAML-compatible
3. `ConfigService` — `init()`, `load()`, `save()` methods
4. Command hierarchy scaffold — `ViracochaCommand` → `ConfigCommand` → `InitCommand`, `ShowConfigCommand`
5. CONF-03 guard — `load()` throws `ConfigNotInitializedException`; all non-init commands call `load()` first
6. JSONL logging — `logback.xml` with `FileAppender` + `LogstashEncoder` to `~/.local/share/viracocha/vira.jsonl`; no log output on stdout/stderr
7. Missing pom.xml dependencies added: `jackson-dataformat-yaml`, `logstash-logback-encoder:7.4`

**Not in Phase 1:** Freemarker, catalogs, patterns, projects, generation — all Phase 2+

---

## Key Constraints from Research

- Lombok must remain first in `annotationProcessorPaths` — do not reorder
- Do NOT use `@Builder` alone on Micronaut beans — use `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`
- All subcommands declared statically in `@Command(subcommands = {...})` — no dynamic registration
- `XDG_CONFIG_HOME` may be null — always use the `XdgPaths` utility, never inline env reads
- JSONL log goes to file only — Logback console appender must be removed or silenced
- Do NOT parse config at startup for commands that don't need it — load lazily inside `run()`/`call()`
- `freemarker` dependency is NOT needed in Phase 1 — add it in Phase 2

---

## Deferred Ideas

- `--force` flag on `config init` to overwrite — v2 only
- Config validation / schema version checking — add when schema evolves
- Colored table output for `config show` — not needed for raw YAML dump
