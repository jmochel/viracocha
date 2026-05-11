---
created: "2026-05-11T17:07:03.009Z"
title: Eliminate default Logback configuration status messages
area: general
files:
  - "src/main/resources/logback.xml"
  - "/home/jmochel/.cursor/projects/home-jmochel-ai-idioms/terminals/2.txt:947-965"
---

## Problem

When the CLI starts, Logback emits default **status/configuration** lines to the console (for example: logback-core/classic version discovery showing `?`, version mismatch `WARN`, `ContextInitializer` trying `DefaultJoranConfigurator`, resource discovery for `logback.xml`, `AppenderModelHandler` processing `JSONL`). That noise conflicts with the product goal of structured logs only under `~/.local/share/viracocha/vira.jsonl` and a clean CLI stdout/stderr for users.

## Solution

TBD — typically disable Logback's status printing via `logback.xml` (`<configuration status="OFF">` where supported), a `NopStatusListener`, and/or JVM system property `logback.statusListenerClass`; confirm interaction with Micronaut defaults and native image. Verify no regression for real logging to the JSONL file.
