---
created: "2026-05-11T17:00:16.994Z"
title: Fix LogstashEncoder missing for JSONL appender
area: general
files:
  - "src/main/resources/logback.xml"
  - "pom.xml"
  - "/home/jmochel/.cursor/projects/home-jmochel-ai-idioms/terminals/2.txt:973-1015"
---

## Problem

At application startup, Logback fails to instantiate the JSONL `FileAppender` encoder: `java.lang.ClassNotFoundException: net.logstash.logback.encoder.LogstashEncoder`. Logback then reports `No encoder set for the appender named "JSONL"` and the app continues with a misconfigured appender.

Observed when running the CLI (stack shows `ViracochaCommand.main` → Micronaut `PicocliRunner`). The configured path `${user.home}/.local/share/viracocha/vira.jsonl` is substituted correctly; the failure is purely missing encoder class on the **runtime** classpath.

Note: `pom.xml` already declares `logstash-logback-encoder`; the fix may involve how the app is invoked (stale JAR, IDE classpath, shade/native-image bundling), or a regression that removed the artifact from the effective runtime set.

## Solution

TBD — verify `./mvnw -q -DskipTests package` and run the produced JAR; confirm dependency is `compile` or included in any shaded/fat artifact; compare with the exact command used when the error appeared. If using GraalVM native image, confirm `logstash-logback` is reachable at runtime per existing `native-image` hints in `pom.xml`.
