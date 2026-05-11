---
created: "2026-05-11T17:01:46.809Z"
title: Remove equals for CLI option values
area: general
files: []
---

## Problem

Improve CLI ergonomics or documentation so option values are not shown or expected with an equals sign (`--option=value`). Target outcome is consistent use of space-separated form (`--option value`) in help, README, scripts, and tests—unless product decision is to enforce parse-time behavior (Picocli supports both by default).

## Solution

TBD — audit `README.md`, `scripts/vira`, and picocli-based commands for `=` usage; decide whether change is docs-only or includes `CommandLine` / `@Option` / synopsis customization. Update tests that assert help text or example invocations.
