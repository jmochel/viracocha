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

## Milestone: v3.0 — Unified Sources & Destinations

**Shipped:** 2026-05-11
**Phases:** 5 | **Plans:** 16 | **Tasks:** 32 | **Commits:** ~96

### What Was Built

- Complete model overhaul: replaced v2 catalog/archetype/project/subscription schema with unified v3 sources/destinations/mappings — 36 v2 files deleted, compile-clean from day one
- `HiddenPathFilter` and `FreemarkerVariableExtractor` relocated to `infra/`; `ConfigVersionException` for v2 config guard
- `vira source add/list/show/remove` with Freemarker variable extraction at registration time
- `vira destination add/list/show/remove` plus `add-mapping/list-mappings/remove-mapping` with `GlobMatcher` wrapping JDK PathMatcher
- `GeneratorService.generate()` fully rewritten: 6-step traversal, glob+recurse filtering, hidden path exclusion, Freemarker expansion in paths and content, binary byte-copy
- `DefaultSyncService.sync()` fully rewritten: source→destination only, timestamp conflict detection via `Files.getLastModifiedTime`, `REPLACE_EXISTING` copy semantics
- Wave 0 Nyquist scaffold pattern applied to Phases 11 and 12: `@Disabled` test stubs created before implementation

### What Worked

- Wave 0 test scaffolding (pre-create `@Disabled` stubs) gave clean compile-time gates before any implementation — plan verification became reliable
- Raw-string traversal check before `Path.of()` solved path normalization bypass without extra complexity; pattern now applied consistently across both SourceService and DestinationService
- `ConfigVersionException extends IOException` required zero changes to command-layer catch blocks — clean extension point
- Separating infra utilities (`HiddenPathFilter`, `FreemarkerVariableExtractor`, `GlobMatcher`) into `infra/` made them independently testable and discoverable
- Content-identity check (`Files.mismatch == -1L`) taking priority over mtime in sync logic prevented spurious copies when timestamps drifted

### What Was Inefficient

- Phase 8 ROADMAP.md tracking was never updated after completion (stayed as `[ ]` / `Not started` / `0/2`); caught at milestone audit but should be caught at plan completion
- Phase 8 SUMMARY.md files had `one_liner:` headers without values — the `summary-extract` tool emitted empty strings into MILESTONES.md; required manual fix at archival
- All 5 VALIDATION.md files were created in `status: draft` and never updated post-execution — Nyquist tracking gap flagged by audit

### Patterns Established

- **Wave 0 scaffold**: Phase plans that touch complex logic should include a Wave 0 plan that creates `@Disabled` test stubs; subsequent plans enable and implement one group at a time
- **Infra package**: Cross-cutting utilities (path filters, extractors, matchers) go in `org.saltations.infra` not in domain packages
- **Raw-string traversal check**: Validate `rawPath.contains("..")` before `Path.of()`; normalizing first allows bypass
- **Content-identity before mtime**: In sync logic, always check `Files.mismatch == -1L` before comparing timestamps
- **`@Disabled` stub naming**: Stub Javadoc must not contain the literal `@Disabled` string (causes false positive in acceptance criteria grep checks)

### Key Lessons

1. Track phase completion in ROADMAP.md immediately — stale `[ ]` checkboxes create audit noise even when work is done
2. Fill `one_liner:` fields in SUMMARY.md at plan completion, not retroactively — gsd-tools reads these for MILESTONES.md
3. Wave 0 scaffold pattern pays back double: the `@Disabled` stubs serve as a living test spec during planning AND become the acceptance test harness during execution
4. A breaking-change milestone (v3.0 deleted 36 files on day one) benefits from "compile-clean at every commit" as a hard gate — no partial states

### Cost Observations

- Timeline: 4 days (2026-05-08 → 2026-05-11)
- Notable: 161 tests, 0 failures at archive; formal milestone audit run before completion

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Key Change |
|-----------|--------|-------|------------|
| v1.0 | 4 | 11 | Initial project scaffolding; established Micronaut + picocli + XDG config patterns |
| v2.0 | 3 | 9 | Added subscription domain; sync engine required careful API-first sequencing |
| v3.0 | 5 | 16 | Breaking model rewrite; Wave 0 Nyquist scaffold pattern introduced; formal audit added |

### Cumulative Quality

| Milestone | Domain | LOC Java (main+test) | Key Testing Approach |
|-----------|--------|---------------------|---------------------|
| v1.0 | Config, Catalogs, Archetypes, Projects, Generate | ~3,000 | @Spec-based output capture; XDG temp dirs |
| v2.0 | Subscriptions, Sync engine, `vira sync` | ~5,191 | `Files.mismatch` temp dirs; exit code integration tests |
| v3.0 | Sources, Destinations, Mappings, Generate v3, Sync v3 | 5,544 (2,469+3,075) | Wave 0 `@Disabled` scaffolds; JUnit 5 temp dirs; CommandLine-rooted command tests |

### Top Lessons (Verified Across Milestones)

1. Keep `@Command` groups thin — delegate to services; picocli handles parsing, Micronaut handles wiring
2. Test with real temp dirs rather than mocks — all three milestones validated this for filesystem-heavy code
3. Phase plans that separate API definition from implementation pay back immediately in later plan waves
4. Wave 0 test scaffold (pre-create `@Disabled` stubs) is worth the extra plan — it turns verification into a pass/fail gate rather than a code review
