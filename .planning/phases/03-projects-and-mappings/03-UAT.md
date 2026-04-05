---
status: complete
phase: projects-and-mappings
source:
  - 03-01-SUMMARY.md
  - 03-02-SUMMARY.md
  - 03-03-SUMMARY.md
started: "2026-04-04T20:00:00Z"
updated: "2026-04-04T20:05:00Z"
verification_note: |
  Automated JAR smoke run (java -jar target/viracocha-0.1.jar with isolated XDG_CONFIG_HOME)
  plus existing JUnit suite (mvn test) — no manual interactive session in this run.
---

## Current Test

[testing complete]

## Tests

### 1. Create project persists in central config
expected: |
  After `vira config init`, `vira project create --name <n> --path <existing-dir>` exits 0,
  prints confirmation, and a subsequent load of config contains the project with that name and path.
result: pass
notes: JAR smoke + ProjectCommandsTest

### 2. Add mapping with workspace path and --param key=value
expected: |
  With a registered pattern, `vira project add-mapping --project <p> --pattern <pat> --workspace <rel> --param k=v`
  exits 0 and `project show` lists the mapping with parameters.
result: pass
notes: JAR smoke + AddMappingAndShowProjectTest

### 3. Unknown pattern rejected without config change
expected: |
  `add-mapping` with a pattern name not in config exits non-zero, stderr names the missing pattern,
  and the project's mappings list is unchanged from before the command.
result: pass
notes: JAR smoke (exit 1) + AddMappingAndShowProjectTest

### 4. Project show lists workspace path and mappings
expected: |
  `vira project show --name <n>` prints Name, Path, and each mapping (pattern, workspace path, parameters);
  `--json` emits one JSON object for the project.
result: pass
notes: JAR smoke (plain output) + tests

### 5. Unregister removes project
expected: |
  `vira project unregister --name <n>` exits 0, confirms removal, and `project list` no longer lists the project.
result: pass
notes: JAR smoke + ProjectCommandsTest

### 6. Project list and JSON output
expected: |
  `vira project list` prints aligned name and path columns; `project list --json` prints one JSON object per line per project.
result: pass
notes: Covered by ListProjectsCommand + unit tests; list verified in smoke

## Summary

total: 6
passed: 6
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[]
