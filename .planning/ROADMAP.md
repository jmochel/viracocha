# Roadmap: Viracocha

## Milestones

- ✅ **v1.0 MVP** — Phases 1–4 (shipped 2026-04-04) — [`milestones/v1.0-ROADMAP.md`](milestones/v1.0-ROADMAP.md)
- 🎯 **v2.0 Subscriptions & sync** — Phases 5–7 (in planning)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–4) — SHIPPED 2026-04-04</summary>

- [x] Phase 1: Foundation (2/2 plans) — completed 2026-03-28
- [x] Phase 2: Publishers and Patterns (3/3 plans) — completed 2026-04-04
- [x] Phase 3: Projects and Mappings (3/3 plans) — completed 2026-04-04
- [x] Phase 4: Workspace Generation (3/3 plans) — completed 2026-04-04

</details>

### v2.0 — Subscriptions & sync

| # | Phase | Goal | Requirements | Success criteria (observable) |
|---|-------|------|--------------|------------------------------|
| 5 | Subscription model & CLI | Persist subscriptions in YAML; CRUD via CLI | CFG-01, CFG-02, SUB-01–SUB-07 | User can add/list/show/remove subscriptions; validation errors are clear; config round-trips |
| 6 | Sync engine | Filesystem sync with direction + conflict detection | SYN-01–SYN-05 | Unit/integration tests pass for pub→ws, ws→pub, bidirectional conflict; hidden-path behavior documented |
| 7 | `vira sync` & docs | User-facing sync command, flags, tests, README | SYN-06–SYN-09, X-01, X-02 | `vira sync --project` works with `--dry-run` / `--verbose`; summary line; README updated |

#### Phase 5: Subscription model & CLI

**Goal:** Introduce subscription entities bound to a project and publisher, with full CLI management.

**Design notes:**

- Add `SubscriptionEntry` (or equivalent) and nest subscriptions under each `ProjectEntry` in YAML (keeps project as the anchor for workspace path).
- Fields (minimum): stable **id**, **publisherName** (must match registered publisher), **sourcePath** (relative to publisher root), **destinationPath** (relative to project workspace), **direction** (`PUBLISH_TO_WORKSPACE` | `WORKSPACE_TO_PUBLISH` | `BIDIRECTIONAL`).
- Command group: `vira subscription` with `add`, `list`, `show`, `remove` (names aligned with existing picocli style). `--project <name>` required where applicable.
- Reuse `ConfigService` load/save; extend schema version if needed.

**Success criteria:**

1. User can register a subscription linking an existing publisher to paths under an existing project.
2. List/show output supports plain and JSON mirrors of other domain commands.
3. Invalid publisher/project/paths produce actionable errors.

#### Phase 6: Sync engine

**Goal:** Implement sync logic independent of the top-level `sync` command (testable service).

**Design notes:**

- Resolve absolute paths: `publisher.root / sourcePath` and `project.workspace / destinationPath`.
- Implement tree walk + copy (NIO `Files.walkFileTree` or equivalent); respect direction.
- **Conflict policy (v2.0):** When both files exist and differ, default **abort** with a structured conflict list; optional strategy flag reserved for Phase 7 if time permits (SYN-04).
- Align **hidden** file/directory handling with `GeneratorService` (reference same helper or document explicit parity).

**Success criteria:**

1. Automated tests for one-way and bidirectional scenarios using temporary directories.
2. Conflict detection demonstrated in tests (e.g. modify both sides → sync fails by default with clear message).

#### Phase 7: `vira sync` & documentation

**Goal:** Expose sync to users with CLI flags and ship documentation.

**Design notes:**

- Command: `vira sync --project <name>`; optional `--subscription <id>` to limit scope.
- Flags: `--dry-run`, `--verbose`; optional `--conflict-strategy` if implemented in Phase 6/7.
- Print summary: copied / updated / skipped / failed / conflicts.
- Update `README.md` with workflow: register publisher → create project → add subscription → `vira sync`.

**Success criteria:**

1. End-to-end test: subscription + sync modifies workspace and/or publisher trees as expected.
2. README section describes directions, limitations (local-only, no watch), and conflict behavior.

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 2/2 | Complete | 2026-03-28 |
| 2. Publishers and Patterns | v1.0 | 3/3 | Complete | 2026-04-04 |
| 3. Projects and Mappings | v1.0 | 3/3 | Complete | 2026-04-04 |
| 4. Workspace Generation | v1.0 | 3/3 | Complete | 2026-04-04 |
| 5. Subscription model & CLI | v2.0 | 3/3 | Complete | 2026-04-04 |
| 6. Sync engine | v2.0 | 0/? | Pending | — |
| 7. `vira sync` & docs | v2.0 | 0/? | Pending | — |

## Next step

**Phase 6 — Sync engine** — `/gsd-discuss-phase 6` then `/gsd-plan-phase 6`
