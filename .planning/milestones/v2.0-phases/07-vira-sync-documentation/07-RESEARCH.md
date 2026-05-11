# Phase 7 — Technical Research

## User Constraints

Copied verbatim from `07-CONTEXT.md` — **non-negotiable for planning:**

- **D-01–D-04:** `--project-name` (required), optional `--subscription <uuid>`, `--dry-run`, `--verbose`, `--json` single object on stdout (no human summary when JSON).
- **D-05–D-08:** Exit `0` only with zero conflicts and zero failed copies; exit `1` otherwise; human summary on stdout; conflict/error detail on stderr; JSON only on stdout when `--json`.
- **D-09–D-11:** Dry-run = analyze-only, no mutations; verbose = per-path lines; combined = planned actions without writes.
- **D-12–D-13:** No `--conflict-strategy` in v2.0; README updates for subscriptions + sync workflow.

**Deferred (ignore in plans):** conflict-strategy flags, finer exit codes, watch mode.

---

## Standard Stack

| Layer | Choice | Confidence |
|-------|--------|--------------|
| CLI | picocli + Micronaut `PicocliRunner` / `MicronautFactory` | HIGH |
| DI | Micronaut `@Singleton` commands | HIGH |
| JSON stdout | `com.fasterxml.jackson.databind.ObjectMapper` (same as `ShowProjectCommand`, `ShowSubscriptionCommand`) | HIGH |
| Sync engine | Existing `DefaultSyncService` — extend with subscription filter + dry-run + verbose collection | HIGH |
| Tests | JUnit 5, `@TempDir`, `ApplicationContext.run(Environment.CLI, Environment.TEST)` | HIGH |

---

## Architecture Patterns

1. **Command parity with `GenerateCommand`:** `--project-name`, `--dry-run`, `--verbose`; `Callable<Integer>`; `CommandSpec` for stdout/stderr.
2. **Engine overload:** `SyncService` gains a full entry point `syncProject(String projectName, String subscriptionIdOrNull, boolean dryRun, boolean verbose)`; existing `syncProject(String)` delegates to `(name, null, false, false)` so Phase 6 tests stay valid.
3. **Subscription filter:** Before iterating `project.getSubscriptions()`, if `subscriptionIdOrNull != null`, retain only `SubscriptionEntry` whose `getId()` equals the string; if none match → `IllegalArgumentException` with clear message (maps to exit 1).
4. **Dry-run:** Thread `dryRun` through `syncOneWay` / `syncBidirectional`; skip `Files.copy`, `Files.createDirectories` when `dryRun`; still increment `filesCopied` / `filesSkipped` / conflicts as if analyze would report (per D-09).
5. **Verbose:** When `verbose`, append one line per significant path action to `SyncSubscriptionResult.verboseLines` (new `List<String>` field), mirroring `GenerationResult.verboseLines()` pattern.
6. **CLI output mapping:** After engine returns, if not `--json`: compute aggregate totals across `subscriptionResults` for summary line; print each `conflictRecords` line to stderr; print verbose lines to stdout before summary when verbose.

---

## Validation Architecture

**Nyquist / automated verification for Phase 7:**

| Dimension | Approach |
|-----------|----------|
| Unit / service | Extend existing `DefaultSyncService*Test` or add focused tests for filter + dry-run |
| CLI | New test class: run `CommandLine` with `ViracochaCommand` + temp config + temp publisher/workspace trees |
| Regression | `mvn test` must stay green |

**Feedback loop:** Run `./mvnw test` after each plan wave; full suite before phase verify.

**Quick command:** `./mvnw -q test`  
**Full suite:** `./mvnw test`

---

## Project Constraints (from `.cursor/rules/`)

- Conventional Commits for commits (orchestrator/executor).
- Java 21, Micronaut 4, picocli — no new stack choices.
- YAML-only central config; local filesystem only.

---

## Common Pitfalls

- **Double-counting paths** when adding verbose — match `generate` granularity (one line per file outcome).
- **JSON serialization:** Lombok `@Data` POJOs need default constructors for Jackson; `List` fields initialized.
- **Bidirectional dry-run:** Must run full analyze; if conflicts, no apply; if no conflicts, walk apply loops without `Files.copy` (per D-09).
- **stderr vs stdout:** Conflicts must not appear in the single-line human summary on stdout (D-07).

---

## Code Examples (references)

- `src/main/java/org/saltations/generate/GenerateCommand.java` — CLI flags + result printing
- `src/main/java/org/saltations/generate/GeneratorService.java` — `dryRun` / `verbose` threading
- `src/main/java/org/saltations/subscription/ShowSubscriptionCommand.java` — `ObjectMapper` JSON to stdout
- `src/main/java/org/saltations/sync/DefaultSyncService.java` — engine to extend

---

## RESEARCH COMPLETE

Research covers: CLI wiring, engine extensions for D-01–D-11, JSON/output/exit mapping, README + integration test strategy.
