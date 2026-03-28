# Features Research

**Domain:** Java CLI workspace management / project scaffolding
**Comparable tools:** cookiecutter, yeoman, maven archetypes, scaffdog

---

## Table Stakes

Features without which the tool feels incomplete or unprofessional:

| Feature | Detail | Complexity |
|---------|--------|------------|
| `--help` on every subcommand | `mixinStandardHelpOptions = true` on every `@Command`, not just root | Low |
| `--version` at root | Standard picocli `versionProvider` or inline version string | Low |
| Meaningful exit codes | 0 = success, 1 = runtime error, 2 = bad input — use `CommandLine.ExitCode` constants | Low |
| Actionable error messages | Name the bad input, suggest the fix. Never print stack traces to users | Low |
| Gate on init | Every command checks config exists before running; clear message if not | Low |
| Empty state handling | `list` commands show helpful empty-state message, not blank output | Low |
| Dry-run for generate | `--dry-run` flag shows what would be written without touching filesystem | Medium |
| Idempotent generate with skip logging | Log each skipped file explicitly: "Skipped (exists): path/to/file" | Low |
| Consistent option naming | `--name`, `--path`, `--project` used consistently across all subcommands | Low |
| Confirmation on mutating commands | Print "Registered publisher 'ai-idioms' at /home/.../ai-idioms" after each mutation | Low |
| Config path transparency | `config init` prints the full path where config was created | Low |

## Differentiators

Features that add real value beyond the basics:

| Feature | Detail | Complexity |
|---------|--------|------------|
| Parameter extraction at register time | Scan Freemarker AST at `pattern register` to extract variable names — no manual declaration | High |
| `pattern show` with param list | Display extracted parameters so user knows what values to supply in mappings | Low |
| Verbose generate output | `--verbose` prints each file action (created/skipped/error) during generation | Low |
| Structured JSONL logging | Machine-readable log output for tooling integration | Medium |
| Validation at add-mapping time | Verify referenced pattern exists and all required params are supplied | Medium |
| Generate summary report | End of generate: "3 files created, 2 skipped, 0 errors" | Low |
| XDG-compliant config path | `$XDG_CONFIG_HOME/viracocha/config.yaml` with `~/.config` fallback | Low |
| File and folder name templating | Expand Freemarker variables in path segments, not just file content — uncommon in scaffolders | High |

## Anti-Features (v1)

Things to explicitly not build yet:

| Feature | Reason |
|---------|--------|
| Interactive prompts during generate | Breaks scripting; user supplies all values up front via `add-mapping` |
| `vira config edit` (opens editor) | Complexity without value; users can edit YAML directly |
| `--force` overwrite on generate | Dangerous; out of scope for v1 (skip-existing is the contract) |
| Remote publishers (HTTP/Git) | Local paths only in v1 |
| Watch/sync mode | v2 feature |
| Subscriptions | v2 feature |
| Config migration tooling | Not needed until schema evolves |
| Shell completion scripts | Nice-to-have, not core |
| Multiple config profiles | Over-engineering for v1 personal use |

## UX Patterns

Conventions from comparable scaffolding tools that apply here:

- **Noun-verb subcommand structure**: `vira publisher register`, NOT `vira register-publisher` — matches git, kubectl, docker conventions
- **Root command is `vira`**, not `viracocha` — short, typeable
- **Confirmation lines on every write**: Users need to know the tool did something
- **Empty-state messages on list**: "No publishers registered. Run 'vira publisher register' to add one."
- **Errors go to stderr, output to stdout** — enables piping and scripting
- **No color by default** — JSONL logging is colorless; respect `NO_COLOR` env var if adding color

## Picocli-Specific Patterns

Best practices for this tool's picocli command structure:

- Use `IExecutionExceptionHandler` for friendly error output without stack traces in production
- Use `CommandLine.ExitCode.SOFTWARE` (1) for runtime errors, `USAGE` (2) for bad input
- Repeatable `@Option(names="--param")` for key=value pairs: `--param serviceName=foo --param env=dev`
- Validate inputs in `run()`, not in constructors — Micronaut DI runs before picocli parses args
- Register subcommands via `@Command(subcommands = {...})` OR Micronaut bean scanning — pick one approach consistently
- `mixinStandardHelpOptions = true` gives `--help` and `--version` automatically
- Use `@ParentCommand` injection to access parent command state in subcommands

---
*Research date: 2026-03-27*
