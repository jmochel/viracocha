---
phase: 12
slug: sync-rewrite
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-10
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Micronaut Test |
| **Config file** | `pom.xml` (Maven Surefire) |
| **Quick run command** | `./mvnw test -pl . -Dtest="*SyncService*,*SyncCommand*" -q` |
| **Full suite command** | `./mvnw test -q` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl . -Dtest="*SyncService*,*SyncCommand*" -q`
- **After every plan wave:** Run `./mvnw test -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-02-01 | 02 | 2 | SYN-01 | unit | `./mvnw test -Dtest="DefaultSyncServiceTest" -q` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 2 | SYN-02 | unit | `./mvnw test -Dtest="DefaultSyncServiceTest" -q` | ❌ W0 | ⬜ pending |
| 12-03-01 | 03 | 3 | SYN-02 | integration | `./mvnw test -Dtest="SyncCommandTest#syncCommandReturnsExitOneOnConflict" -q` | ❌ W0 | ⬜ pending |
| 12-03-02 | 03 | 3 | SYN-03 | integration | `./mvnw test -Dtest="SyncCommandTest" -q` | ❌ W0 | ⬜ pending |
| 12-03-03 | 03 | 3 | SYN-04 | integration | `./mvnw test -Dtest="SyncCommandTest" -q` | ❌ W0 | ⬜ pending |
| 12-03-04 | 03 | 3 | SYN-05 | integration | `./mvnw test -Dtest="SyncCommandTest" -q` | ❌ W0 | ⬜ pending |
| 12-03-05 | 03 | 3 | SYN-06 | integration | `./mvnw test -Dtest="SyncCommandTest" -q` | ❌ W0 | ⬜ pending |
| 12-03-06 | 03 | 3 | SYN-07 | integration | `./mvnw test -Dtest="SyncCommandTest" -q` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/org/saltations/sync/DefaultSyncServiceTest.java` — stubs for SYN-01, SYN-02
- [ ] `src/test/java/org/saltations/sync/SyncCommandTest.java` — stubs for SYN-02 exit code and SYN-03 through SYN-07

*Existing test infrastructure (JUnit 5 + Micronaut Test + @TempDir) covers all framework needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| None | — | — | — |

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
