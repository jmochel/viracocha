---
status: complete
phase: publishers-and-patterns
source:
  - 02-01-SUMMARY.md
  - 02-02-SUMMARY.md
  - 02-03-SUMMARY.md
started: "2026-04-04T13:16:00Z"
updated: "2026-04-04T18:00:00Z"
---

## Current Test

[testing complete]

## Tests

### 1. Publisher and pattern help surfaces
expected: |
  Help for `vira catalog` and `vira pattern` shows four subcommands each (register, list, show, unregister)
  with descriptions; `vira catalog register --help` shows --name and --path options.
result: pass

### 2. Config gate before publisher/pattern commands
expected: |
  With XDG config pointing at a directory where config has never been initialized
  (e.g. empty `XDG_CONFIG_HOME` temp dir), `vira catalog list` exits non-zero and stderr contains
  a clear message to run config init first (e.g. contains "Config not initialized").
result: pass

### 3. Publisher register → list → show → unregister
expected: |
  After `vira config init`, register a publisher with `--name` and `--path` to an existing directory;
  `publisher list` shows name and path in columns; `publisher show --name <n>` shows Name and Path lines;
  `publisher unregister --name <n>` confirms removal; a second list is empty.
result: pass

### 4. Publisher rejects bad path and duplicate name
expected: |
  Register with `--path` to a non-existent path prints an error and does not change config;
  registering the same name twice prints a duplicate message and leaves a single entry.
result: pass

### 5. Pattern register extracts `${...}` names into config
expected: |
  Given a pattern directory containing a file with `${foo}` and `${bar}`, `pattern register` succeeds,
  and `pattern show --name <n>` lists Parameters including foo and bar (order may be sorted);
  `pattern list` includes a param count of 2.
result: pass

### 6. Pattern register fails on malformed template and leaves config unchanged
expected: |
  A pattern directory whose only template contains an unclosed `${` causes register to fail with
  stderr mentioning malformed Freemarker; `pattern list` shows no new pattern.
result: pass

### 7. JSON output for list and show
expected: |
  `publisher list --json` prints one JSON object per line with name and path;
  `pattern list --json` includes a parameters field (array); `pattern show --name x --json` is one JSON object.
result: pass

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

(none yet)
