---
phase: 6
slug: sync-engine
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-04
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) + Micronaut Test |
| **Config file** | `pom.xml` (surefire) |
| **Quick run command** | `mvn -q test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~30–90 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q test`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | SYN-01–SYN-05 (types) | compile + unit | `mvn -q test` | ✅ | ⬜ pending |
| 06-01-02 | 01 | 1 | SYN-01–SYN-05 (types) | compile + unit | `mvn -q test` | ✅ | ⬜ pending |
| 06-02-01 | 02 | 2 | SYN-01, SYN-02, SYN-04 | integration | `mvn -q test` | ✅ | ⬜ pending |
| 06-02-02 | 02 | 2 | SYN-05 | integration | `mvn -q test` | ✅ | ⬜ pending |
| 06-03-01 | 03 | 3 | SYN-03 | integration | `mvn -q test` | ✅ | ⬜ pending |
| 06-03-02 | 03 | 3 | SYN-01–SYN-05 | integration | `mvn -q test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements — JUnit 5, `@TempDir`, Micronaut `@MicronautTest` already used in project.

---

## Manual-Only Verifications

All phase behaviors have automated verification (filesystem scenarios in temp dirs).

---

## Validation Sign-Off

- [x] All tasks have automated verify via `mvn test`
- [x] Sampling continuity: tests run after each plan
- [x] Wave 0 — N/A (existing infra)
- [x] No watch-mode flags
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
