---
created: "2026-05-11T17:04:58.928Z"
title: Add -n and -p aliases for name and path options
area: general
files:
  - "src/main/java/org/saltations/source/SourceAddCommand.java"
  - "src/main/java/org/saltations/destination/DestinationAddCommand.java"
---

## Problem

Users want short flags: `-n` as an alias for `--name` and `-p` for `--path` on commands that register sources and destinations (currently long options only).

## Solution

TBD — extend Picocli `@Option(names = {...})` on both add commands (and any other commands that expose the same `--name` / `--path` pair) to include `"-n"` and `"-p"`. Update class-level command synopsis comments, README examples, and tests that snapshot usage or help text.
