---
phase: 7
slug: vira-sync-documentation
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-04
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) |
| **Config file** | `pom.xml` (Surefire) |
| **Quick run command** | `./mvnw -q test` |
| **Full suite command** | `./mvnw test` |
| **Estimated runtime** | ~30–90 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw -q test`
- **After every plan wave:** Run `./mvnw test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | SYN-06–SYN-09 (engine) | unit | `./mvnw -q test` | ✅ | ⬜ pending |
| 07-01-02 | 01 | 1 | SYN-07–SYN-08 (engine) | unit | `./mvnw -q test` | ✅ | ⬜ pending |
| 07-02-01 | 02 | 1 | SYN-06–SYN-09 (CLI) | unit | `./mvnw -q test` | ✅ | ⬜ pending |
| 07-03-01 | 03 | 2 | X-01, X-02 | integration | `./mvnw test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] Existing JUnit 5 + Micronaut test context — **Wave 0 satisfied**
- [ ] No new framework install required

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|---------------------|
| *None planned* | — | All behaviors covered by automated tests | — |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
