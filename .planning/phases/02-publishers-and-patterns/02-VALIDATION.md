---
phase: 2
slug: publishers-and-patterns
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-28
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) via Maven |
| **Config file** | `pom.xml` (already configured) |
| **Quick run command** | `./mvnw test -pl . -q` |
| **Full suite command** | `./mvnw verify -q` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl . -q`
- **After every plan wave:** Run `./mvnw verify -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 0 | PUB-01..05, PAT-01..06 | unit stubs | `./mvnw test -q` | ❌ W0 | ⬜ pending |
| 2-02-01 | 02 | 1 | PUB-01 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-02-02 | 02 | 1 | PUB-02 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-02-03 | 02 | 1 | PUB-03 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-02-04 | 02 | 1 | PUB-04 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-02-05 | 02 | 1 | PUB-05 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-01 | 03 | 1 | PAT-01 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-02 | 03 | 1 | PAT-02 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-03 | 03 | 1 | PAT-03 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-04 | 03 | 1 | PAT-04 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-05 | 03 | 1 | PAT-05 | unit | `./mvnw test -q` | ✅ | ⬜ pending |
| 2-03-06 | 03 | 1 | PAT-06 | unit | `./mvnw test -q` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/org/saltations/CatalogCommandTest.java` — stubs for PUB-01..05
- [ ] `src/test/java/org/saltations/PatternCommandTest.java` — stubs for PAT-01..06
- [ ] `src/test/java/org/saltations/FreemarkerVariableExtractorTest.java` — stubs for PAT-03, PAT-04

*Existing JUnit 5 infrastructure is already configured in pom.xml — no framework installation needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CLI help text for publisher/pattern subcommands | PUB-01, PAT-01 | Picocli-generated help output not easily asserted in unit | Run `vira catalog --help` and `vira pattern --help`; verify subcommand list |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
