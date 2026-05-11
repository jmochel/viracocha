---
phase: 10
slug: destination-mapping-commands
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-10
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) — managed by Micronaut 4.10.10 |
| **Config file** | `pom.xml` (surefire plugin via micronaut-parent) |
| **Quick run command** | `./mvnw test -Dtest="GlobMatcherTest,DestinationServiceTest" -q` |
| **Full suite command** | `./mvnw test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -Dtest="GlobMatcherTest,DestinationServiceTest" -q`
- **After every plan wave:** Run `./mvnw test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | MAP-05 | unit | `./mvnw test -Dtest="GlobMatcherTest"` | ❌ W0 | ⬜ pending |
| 10-01-02 | 01 | 1 | DEST-01,DEST-05,DEST-06 | integration | `./mvnw test -Dtest="DestinationServiceTest"` | ❌ W0 | ⬜ pending |
| 10-02-01 | 02 | 2 | DEST-01,DEST-05,DEST-06 | integration | `./mvnw test -Dtest="DestinationAddCommandTest"` | ❌ W0 | ⬜ pending |
| 10-02-02 | 02 | 2 | DEST-02 | integration | `./mvnw test -Dtest="DestinationListCommandTest"` | ❌ W0 | ⬜ pending |
| 10-02-03 | 02 | 2 | DEST-03 | integration | `./mvnw test -Dtest="DestinationShowCommandTest"` | ❌ W0 | ⬜ pending |
| 10-02-04 | 02 | 2 | DEST-04 | integration | `./mvnw test -Dtest="DestinationRemoveCommandTest"` | ❌ W0 | ⬜ pending |
| 10-03-01 | 03 | 3 | MAP-01,MAP-04 | integration | `./mvnw test -Dtest="DestinationAddMappingCommandTest"` | ❌ W0 | ⬜ pending |
| 10-03-02 | 03 | 3 | MAP-02 | integration | `./mvnw test -Dtest="DestinationListMappingsCommandTest"` | ❌ W0 | ⬜ pending |
| 10-03-03 | 03 | 3 | MAP-03 | integration | `./mvnw test -Dtest="DestinationRemoveMappingCommandTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/org/saltations/infra/GlobMatcherTest.java` — stubs for MAP-05
- [ ] `src/test/java/org/saltations/destination/DestinationServiceTest.java` — stubs for DEST-05, DEST-06, service CRUD
- [ ] `src/test/java/org/saltations/destination/DestinationAddCommandTest.java` — stubs for DEST-01, DEST-05, DEST-06
- [ ] `src/test/java/org/saltations/destination/DestinationListCommandTest.java` — stubs for DEST-02
- [ ] `src/test/java/org/saltations/destination/DestinationShowCommandTest.java` — stubs for DEST-03
- [ ] `src/test/java/org/saltations/destination/DestinationRemoveCommandTest.java` — stubs for DEST-04
- [ ] `src/test/java/org/saltations/destination/DestinationAddMappingCommandTest.java` — stubs for MAP-01, MAP-04
- [ ] `src/test/java/org/saltations/destination/DestinationListMappingsCommandTest.java` — stubs for MAP-02
- [ ] `src/test/java/org/saltations/destination/DestinationRemoveMappingCommandTest.java` — stubs for MAP-03

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Tilde path stored as-is | DEST-01 (D-07) | No expansion behavior to automate — verification is that `~` appears in config YAML unchanged | Run `vira destination add --name tilde-test --path ~/workspace`; inspect `~/.config/viracocha/config.yaml` and confirm path is `~/workspace` not expanded |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
