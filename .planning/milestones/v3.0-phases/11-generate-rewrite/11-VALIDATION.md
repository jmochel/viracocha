---
phase: 11
slug: generate-rewrite
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-10
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) — `junit-jupiter-api` + `junit-jupiter-engine` via Micronaut BOM |
| **Config file** | none — Maven Surefire picks up JUnit 5 automatically |
| **Quick run command** | `./mvnw test -Dtest="GeneratorServiceTest,GenerateCommandTest" -q` |
| **Full suite command** | `./mvnw test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -Dtest="GeneratorServiceTest,GenerateCommandTest" -q`
- **After every plan wave:** Run `./mvnw test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-W0-01 | W0 | 0 | GEN-01..07 | stub | `./mvnw test -Dtest="GeneratorServiceTest,GenerateCommandTest" -q` | ❌ W0 | ⬜ pending |
| 11-01-01 | 01 | 1 | GEN-01 | integration | `./mvnw test -Dtest="GeneratorServiceTest#*flat*,GeneratorServiceTest#*recurse*"` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | GEN-02 | integration | `./mvnw test -Dtest="GeneratorServiceTest#*skipExisting*"` | ❌ W0 | ⬜ pending |
| 11-01-03 | 01 | 1 | GEN-03 | integration | `./mvnw test -Dtest="GeneratorServiceTest#*template*"` | ❌ W0 | ⬜ pending |
| 11-01-04 | 01 | 1 | GEN-04 | integration | `./mvnw test -Dtest="GeneratorServiceTest#*binary*"` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 2 | GEN-05 | integration | `./mvnw test -Dtest="GenerateCommandTest#*destinationName*"` | ❌ W0 | ⬜ pending |
| 11-02-02 | 02 | 2 | GEN-06 | integration | `./mvnw test -Dtest="GenerateCommandTest#*dryRun*"` | ❌ W0 | ⬜ pending |
| 11-02-03 | 02 | 2 | GEN-07 | integration | `./mvnw test -Dtest="GenerateCommandTest#*verbose*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/org/saltations/generate/GeneratorServiceTest.java` — full stubs covering GEN-01 through GEN-04 (flat walk, recurse walk, skip-existing, template expansion, binary copy)
- [ ] `src/test/java/org/saltations/generate/GenerateCommandTest.java` — full stubs covering GEN-05 through GEN-07 (`--dest`, `--dry-run`, `--verbose`)
- [ ] `src/test/resources/fixtures/sample.bin` — binary fixture file for GEN-04 byte-integrity test

*All three are currently placeholder stubs or absent — Wave 0 must create them before implementation waves.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Destination creation prompt (interactive stdin) | D-05, D-06, D-07 | Interactive stdin read from terminal cannot be automated without System.in injection | Run `vira generate --dest my-ws` against a non-existent destination path; verify prompt appears, type `y`, confirm directory created; repeat with `N`, confirm exit 0 and no directory |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
