# Pitfalls Research

**Project:** Viracocha (`vira`) — Java CLI workspace manager
**Stack:** JDK 21, Micronaut 4.x, picocli, Freemarker, jackson-dataformat-yaml, Logback, Lombok
**Researched:** 2026-03-27
**Confidence:** MEDIUM (domain expertise; external search unavailable)

---

## Critical Pitfalls

### C1: Freemarker Filename Variable Expansion Is Not Built-In

**What goes wrong:** Freemarker processes file *content*, not file *paths*. If a pattern source tree has a folder named `${projectName}`, walking it and mapping source paths to destination paths produces a literal directory named `${projectName}` in the output. The template engine has no concept of the destination path.

**Why it happens:** `Template.process(dataModel, writer)` writes expanded content to a `Writer`. The destination path is a Java `Path` determined by the caller before `process()` is invoked. Variable expressions in source directory/file names are copied literally unless explicitly expanded first.

**Consequences:** Generated workspace has directories named `${projectName}` rather than the resolved value. Silent corruption — no exception thrown, files write successfully to the wrong location.

**Prevention:**
- Write a `PathExpander` utility that takes a path string and a `Map<String, Object>` data model and returns the resolved path string with `${var}` tokens replaced.
- Never treat source-to-destination path mapping as a simple `relativize()` + `resolve()` operation.
- Test `PathExpander` independently before any file-writing code.

**Warning signs:**
- `${` appearing literally in generated directory or file names
- Content-level assertions pass in generation tests but path assertions fail

**Phase:** Workspace generation — design `PathExpander` as the first task.

---

### C2: Picocli Subcommands Must Be Declared in `@Command` Annotation

**What goes wrong:** Subcommands registered dynamically via `CommandLine.addSubcommand()` or discovered via Micronaut bean scanning are invisible to `PicocliRunner`. The `CommandLine` object is constructed before `run()` is called. Only subcommands declared in `@Command(subcommands = {...})` are known to picocli.

**Consequences:** Subcommands absent from `--help`. `UnmatchedArgumentException` at runtime. No error at startup — failure only at invocation time.

**Prevention:**
- Declare all subcommands in `@Command(subcommands = {ConfigCommand.class, PatternCommand.class, ...})` on the root command.
- Each subcommand class must be a Micronaut `@Singleton` for DI injection to work.
- Sub-subcommands (`pattern register`, `pattern list`) require a parent `@Command` declaring its own `subcommands` attribute.

**Warning signs:**
- `vira --help` shows only root command options with no subcommands listed
- `UnmatchedArgumentException: Unmatched argument at index 0: 'pattern'`

**Phase:** Subcommand scaffold — get the declaration hierarchy right before implementing any command logic.

---

### C3: Micronaut ApplicationContext Startup Cost on Every CLI Invocation

**What goes wrong:** Micronaut's full DI context starts on every `vira` invocation, adding 300–800 ms on JVM. For a daily-driver tool expected to feel snappy, this is perceptibly sluggish.

**Prevention:**
- Keep the `@Singleton` graph shallow. Every `@PostConstruct` adds startup cost.
- Do NOT eagerly load Freemarker `Configuration` at startup — use lazy initialization, only when `generate` is called.
- Do NOT parse the YAML config at startup for commands that don't need it.
- Document JVM startup latency as a known v1 limitation.
- GraalVM native image (already scaffolded in pom.xml) eliminates JVM startup — but see C4.

**Phase:** Ongoing design concern. Use lazy initialization patterns from the start.

---

### C4: GraalVM Native Image Breaks Reflection-Heavy Code

**What goes wrong:** The project already has `micronaut-graal` annotation processor configured. Native image fails at runtime for any class accessed reflectively — including Jackson YAML deserialization targets, Freemarker internals, and Logback XML config parsing.

**Consequences:** `native-image` build succeeds but binary crashes at runtime with `ClassNotFoundException`. Failures only appear in native binary, not JVM test runs. Debug cycle is long.

**Prevention:**
- For v1 on JVM: gate native builds behind a separate Maven profile, do not run in CI or dev loop.
- If native image is eventually needed: annotate all config model classes with `@Introspected`; use Micronaut Serialization (`micronaut-serde-jackson`) instead of classic Jackson `ObjectMapper`.

**Phase:** Architecture decision — explicitly defer native image to post-v1.

---

### C5: YAML Config Schema Evolution Without Validation Causes Silent Nulls

**What goes wrong:** New fields added to the config schema silently produce null values in old config files. Jackson's YAML deserialization nulls missing optional fields with no error. Nulls propagate into generation logic, producing files with literal `${variableName}` placeholders.

**Prevention:**
- Write a `ConfigValidator` that checks required fields after deserialization and throws `ConfigValidationException` with a human-readable message.
- Set `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` to `false` for forward compatibility.
- Add a version-check gate: if `version` is absent or mismatched, emit a clear error rather than continuing.

**Phase:** Config initialization — design config model with validation from day one.

---

## Common Mistakes

### M1: Freemarker Template Loading: Classpath vs. Filesystem

**What goes wrong:** Using `ClassPathTemplateLoader` for user-registered patterns on the filesystem. Results in `TemplateNotFoundException` for paths that clearly exist on disk.

**Prevention:** Use `FileTemplateLoader` or `cfg.setDirectoryForTemplateLoading(new File(patternRootDir))` for all user-provided patterns.

**Warning signs:** Works in unit tests (classpath resources) but fails in integration tests using temp dirs.

---

### M2: Freemarker Variable Extraction Is Not a Simple String Search

**What goes wrong:** Grepping for `${varName}` misses variables in FTL directives (`<#if varName?has_content>`), iteration variables (`<#list items as item>`), and nested expressions (`${obj.field}`).

**Prevention:**
- Use the Freemarker AST via `Template.getRootTreeNode()` for complete extraction.
- Pragmatic fallback: scan for `\$\{([a-zA-Z_][a-zA-Z0-9_]*)[^}]*\}` and document the limitation.

**Warning signs:** `pattern show` reports zero parameters for a template known to have variables; `generate` fails with `UndefinedVariableException` for an unlisted variable.

---

### M3: Picocli Exit Code Handling Is Not Automatic

**What goes wrong:** Commands implementing `Runnable` always exit 0. `PicocliRunner.run()` discards the `CommandLine.execute()` return value. Uncaught exceptions are swallowed.

**Prevention:**
- Implement `Callable<Integer>` instead of `Runnable` for all command classes.
- Register an `IExecutionExceptionHandler` to convert exceptions to exit codes consistently.
- Use `System.exit(commandLine.execute(args))` in `main()`.
- Standard codes: 0 = success, 1 = usage error, 2 = config error, 3 = generation error.

**Warning signs:** `echo $?` always returns 0 after deliberate failure.

---

### M4: JSONL Logging Collides with Normal Command Output on Stdout

**What goes wrong:** Logback's default appender writes to stdout. Human-readable command output and JSONL log lines mix on stdout, breaking any downstream parsing.

**Prevention:**
- Route Logback to stderr: `<target>System.err</target>` in `logback.xml`.
- Use stdout exclusively for user-facing command output (tables, confirmations).

**Warning signs:** `vira pattern list | grep mypattern` produces JSON noise in grep output.

---

### M5: XDG Config Path NPE When `XDG_CONFIG_HOME` Is Unset

**What goes wrong:** `System.getenv("XDG_CONFIG_HOME")` returns `null` on macOS and many Linux installs. Direct use in `Path.of(null, ...)` throws `NullPointerException`.

**Prevention:**
```java
String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
Path configBase = (xdgConfigHome != null && !xdgConfigHome.isBlank())
    ? Path.of(xdgConfigHome)
    : Path.of(System.getProperty("user.home"), ".config");
```
Encapsulate in a single `XdgPaths` utility class. All other classes go through it.

**Warning signs:** NPE in `Path.of()` on first run; tests that set `XDG_CONFIG_HOME` pass but production run fails.

---

### M6: Lombok + Micronaut Annotation Processor Ordering

**What goes wrong:** If Lombok runs after `micronaut-inject-java`, Micronaut cannot see Lombok-generated constructors/getters, causing `BeanInstantiationException`.

**Prevention:**
- Lombok MUST appear first in `annotationProcessorPaths`. Current pom.xml already has this correct — preserve the order and guard it with a comment.
- Do not use `@Builder` alone on Micronaut-managed beans — it removes the no-arg constructor. Use `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` together.

**Warning signs:** `No bean of type [X] found` for a Lombok-annotated class; compile succeeds but context startup fails.

---

### M7: Skip-Existing Has a TOCTOU Race

**What goes wrong:** `Files.exists(destination)` + `Files.write(destination, ...)` is a Time-Of-Check-Time-Of-Use race. A concurrent process can create the file between check and write, causing an overwrite.

**Prevention:**
- Use `Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)` — atomically creates or throws `FileAlreadyExistsException`. Catch `FileAlreadyExistsException` as the skip signal.

---

### M8: Partial Generation Leaves Workspace in Inconsistent State

**What goes wrong:** File 15 of 20 fails. Files 1–14 are written. Next run skips 1–14 (they exist). Files 15–20 are still missing with no indication.

**Prevention:**
- Accumulate per-file outcomes (created / skipped / failed). Print summary at exit: `"Generated: 14 files, Skipped: 0, Failed: 6"`.
- Log each failure with file path and reason.
- Emit JSONL events per file: `{"event":"file_result","path":"...","status":"created|skipped|failed"}`.

---

### M9: `jackson-dataformat-yaml` and `freemarker` Are Missing from the POM

**What goes wrong:** Neither dependency is in the current `pom.xml`. Implementing config loading or template expansion without adding them produces `ClassNotFoundException` at runtime.

**Prevention:**
- Add both dependencies before writing any code that uses them.
- `jackson-dataformat-yaml`: omit version (Micronaut BOM manages Jackson versions).
- `freemarker`: pin explicit version `2.3.33` (not in Micronaut BOM).

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|----------------|------------|
| Initial subcommand scaffold | Subcommands missing from `@Command(subcommands=...)` | Declare full hierarchy before any logic |
| Config init | XDG path NPE when `XDG_CONFIG_HOME` unset | `XdgPaths` utility with fallback |
| Config model | Missing `jackson-dataformat-yaml` in POM | Add dependency before writing any config code |
| Config model | Silent null fields from missing optional YAML fields | `ConfigValidator` immediately after deserialization |
| Pattern registration | Freemarker param extraction misses non-`${}` usages | Document scope; plan AST walk for completeness |
| Pattern registration | `ClassPathTemplateLoader` for filesystem templates | Use `FileTemplateLoader` for all user patterns |
| Workspace generation | Filename variable not expanded by Freemarker | Implement `PathExpander` first; test independently |
| Workspace generation | TOCTOU on skip-existing check | Use `CREATE_NEW` open option |
| Workspace generation | Partial failure with no summary | Accumulate outcomes; print summary on exit |
| Logging setup | JSONL mixed with stdout | Configure Logback to stderr only |
| Exit codes | All failures exit 0 | Switch to `Callable<Integer>` before first real command |
| Annotation processors | Lombok reordered after Micronaut processor | Guard ordering with comment in pom.xml |

---

## Testing Gotchas

### T1: Micronaut ApplicationContext in Tests Is Slow and Stateful

Use `@MicronautTest(environments = "test")` with `application-test.yml` pointing config paths to temp directories. For unit tests, use `ApplicationContext.builder().properties(...)` directly to avoid full context overhead.

### T2: Freemarker Template Tests Need Temp Directories

Use JUnit 5 `@TempDir`. Write template files programmatically: `Files.writeString(tempDir.resolve("template.md"), "Hello ${name}")`. Point `FileTemplateLoader` at the temp dir. Makes tests hermetic.

### T3: Testing Exit Codes — Avoid `System.exit()` in Test JVM

Do not call `System.exit()` inside service or command logic. Return exit codes through `Callable<Integer>`. In tests, call `CommandLine.execute()` directly and assert on the returned int.

### T4: YAML Config Round-Trip Tests Catch Schema Drift

Maintain fixture YAML files in `src/test/resources/fixtures/` for each schema version. Write round-trip tests: deserialize → serialize → deserialize → assert equality.

### T5: Logback Output in Tests Requires `ListAppender`

Add `logback-test.xml` in `src/test/resources/` configuring a `ListAppender<ILoggingEvent>`. Retrieve it from the logger in tests and assert on `appender.list`. The existing `System.setOut()` approach in `ViracochaCommandTest` is fragile — replace with this pattern when implementing structured logging.

---
*Research date: 2026-03-27*
