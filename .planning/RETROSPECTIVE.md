# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

---

## Milestone: v2.0 — Subscriptions & sync

**Shipped:** 2026-04-04
**Phases:** 3 | **Plans:** 9 | **Tasks:** 19 | **Commits:** 25

### What Was Built

- `SubscriptionEntry` model nested under `ProjectEntry` in YAML with bidirectional config round-trip
- `vira subscription add/list/show/remove` CLI with path validation, duplicate rejection, and JSON output
- `DefaultSyncService` with one-way and bidirectional sync using NIO `Files.walkFileTree` and `Files.mismatch`
- `vira sync --project-name` with `--dry-run`, `--verbose`, `--subscription` filter, and `--json` output
- Integration tests covering all three sync directions (catalog→ws, ws→catalog, bidirectional conflict)
- README workflow: register catalog → create project → add subscription → `vira sync`

### What Worked

- Splitting sync into three plans (API types → one-way → bidirectional) gave clean compile-time gates between each wave
- Defining `SyncService` interface before implementing `DefaultSyncService` kept Phase 7 changes additive, not disruptive
- Using `Files.mismatch` for conflict detection was accurate and required zero external dependencies
- Naming `vira subscription` to mirror `vira catalog` / `vira project` gave consistent UX with no bikeshedding

### What Was Inefficient

- Phase 5 Plan 01 had to update all `ProjectEntry` constructors when adding `subscriptions` field — an `@AllArgsConstructor` arity problem that broke call sites in other tests; a `@Builder` default would have avoided this
- No milestone audit was run before completion — gaps (if any) were not formally verified before archiving

### Patterns Established

- Subscription ids are global UUIDs (not project-scoped) — enables CLI targeting without `--project` on `show`/`remove`
- Conflict default = abort (structured conflict list); optional override flags reserved for future milestones
- Bidirectional sync uses TreeSet union for deterministic path ordering, then two-pass apply
- `SyncService` interface default method wraps the full four-arg form — keeps backward-compatible one-arg usage

### Key Lessons

1. Define data types (API layer) before implementing logic — Phase 6 Plan 01 (types only) made Plans 02–03 straightforward to verify
2. Add `@Builder` with defaults for model classes that will gain fields over time — avoids constructor arity explosions across test files
3. Conflict-safe defaults first, override flags later — correct design for a sync tool aimed at CI/automation use

### Cost Observations

- Sessions: ~5 (estimate)
- Notable: All 9 plans executed in a single day (2026-04-04) — high execution velocity for a complex domain

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 4 | 11 | Initial project scaffolding; established Micronaut + picocli + XDG config patterns |
| v2.0 | 3 | 9 | Added subscription domain; sync engine required careful API-first sequencing |

### Cumulative Quality

| Milestone | Domain | LOC (Java) | Key Testing Approach |
|-----------|--------|-----------|---------------------|
| v1.0 | Config, Catalogs, Archetypes, Projects, Generate | ~3,000 | @Spec-based output capture; XDG temp dirs |
| v2.0 | Subscriptions, Sync engine, `vira sync` | ~5,191 | `Files.mismatch` temp dirs; exit code integration tests |

### Top Lessons (Verified Across Milestones)

1. Keep `@Command` groups thin — delegate to services; picocli handles parsing, Micronaut handles wiring
2. Test with real temp dirs rather than mocks — both milestones validated this for filesystem-heavy code
3. Phase plans that separate API definition from implementation pay back immediately in later plan waves
