---
phase: 8
slug: model-config-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-08
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) via junit-jupiter-api + junit-jupiter-engine |
| **Config file** | None — Maven Surefire auto-detects JUnit 5 via junit-jupiter-engine on classpath |
| **Quick run command** | `./mvnw test -Dtest=ViracochaConfigV3Test,ConfigServiceTest -q` |
| **Full suite command** | `./mvnw test -q` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -q`
- **After every plan wave:** Run `./mvnw test -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 8-W0-01 | W0 | 0 | CFG-01 | unit | `./mvnw test -Dtest=ViracochaConfigV3Test -q` | ❌ W0 | ⬜ pending |
| 8-W0-02 | W0 | 0 | CFG-01 | unit | `./mvnw test -Dtest=ViracochaConfigV3TypedListTest -q` | ❌ W0 | ⬜ pending |
| 8-01 | 01 | 1 | CFG-01 | unit | `./mvnw test -Dtest=ViracochaConfigV3Test,ViracochaConfigV3TypedListTest -q` | ❌ W0 | ⬜ pending |
| 8-02 | 01 | 1 | CFG-02 | unit | `./mvnw test -Dtest=ConfigServiceTest -q` | ✅ | ⬜ pending |
| 8-03 | 01 | 2 | CFG-03 | smoke | `./mvnw test -Dtest=ViracochaCommandTest -q` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/org/saltations/model/ViracochaConfigV3Test.java` — stubs for CFG-01 (v3 ViracochaConfig round-trip with version:3, sources, destinations)
- [ ] `src/test/java/org/saltations/model/ViracochaConfigV3TypedListTest.java` — stubs for CFG-01 (SourceEntry, DestinationEntry, MappingEntry v3 list fields round-trip)

Note: Existing `ViracochaConfigTest.java`, `ViracochaConfigTypedListTest.java`, `ViracochaConfigProjectTypedListTest.java` must be deleted when their v2 model classes are removed.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `vira config show` displays v3 config without error | CFG-01 | Integration CLI test | Run `vira config init && vira config show`; verify output contains `sources:` and `destinations:` |
| v2 config causes clear error message + exit 1 | CFG-02 | Integration CLI test | Write a v1/v2 config file; run `vira config show`; verify error message contains "v3 format required" and process exits non-zero |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
