# Codebase Concerns

**Analysis Date:** 2026-03-27

## Build Failures

**Maven Enforcer Plugin - Broken checkSnakeYaml Rule:**
- Issue: Build fails due to deprecated Maven Enforcer Plugin API usage for the `checkSnakeYaml` rule
- Files: `pom.xml` (maven-enforcer-plugin configuration)
- Impact: Build cannot complete; `mvn clean compile` and `mvn test` fail immediately during the enforce phase
- Error: `Failed to create enforcer rules with name: checkSnakeYaml or for class: org.apache.maven.plugins.enforcer.CheckSnakeYaml`
- Fix approach: Either remove the checkSnakeYaml enforcement rule from the POM configuration, or update to a version of maven-enforcer-plugin that supports the current API. The rule is part of Micronaut's default configuration but is no longer compatible with enforcer 3.6.2

**Parent POM Dependency Management Conflict:**
- Issue: Maven parent (`micronaut-platform:4.10.10`) has duplicate dependency entries for `com.vladsch.flexmark:flexmark-html2md-converter:jar` with conflicting version properties
- Files: Inherited from `io.micronaut.platform:micronaut-platform:4.10.10` pom.xml
- Impact: Build warnings indicate malformed POM that future Maven versions may not support
- Workaround: The build still completes warnings, but the underlying issue is in the parent POM, which is outside this project's control
- Fix approach: Contact Micronaut project or upgrade to newer Micronaut platform version that fixes this conflict

## Test Coverage Gaps

**Bare Minimum Test Suite:**
- What's not tested: The application has only 1 test file (`src/test/java/org/saltations/ViracochaCommandTest.java`) that exercises only the verbose option
- Files: `src/test/java/org/saltations/ViracochaCommandTest.java`, `src/main/java/org/saltations/ViracochaCommand.java`
- Risk: No coverage for non-verbose execution path, command initialization failures, or edge cases. The `run()` method logic is minimal but untested for baseline behavior
- Priority: Medium - As the application grows, test coverage must expand

## Incomplete Implementation

**Application Logic Placeholder:**
- Issue: The `run()` method in `ViracochaCommand` contains only minimal placeholder business logic: a conditional print statement
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (lines 22-27)
- Impact: Application does not perform meaningful work; command execution results in either no output or "Hi!" message
- Current state: This appears intentional as a skeleton application, but actual business logic is missing
- Implications: No real functionality to validate, test, or deploy

## Logging Anti-Pattern

**Direct System.out Usage in Production Code:**
- Issue: Application uses `System.out.println()` for command output instead of proper logging framework
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (line 25)
- Impact: Output bypasses SLF4J/Logback configuration, makes testing brittle (relies on output stream redirection), loses structured logging benefits
- Context: Logback is configured (`src/main/resources/logback.xml`) with proper appenders, but not used in main command
- Fix approach: Replace `System.out.println("Hi!")` with SLF4J Logger calls (e.g., `log.info("Hi!")`)

**Test Uses Output Stream Redirection:**
- Issue: Test captures output via `System.setOut(new PrintStream(baos))` rather than mocking or using test utilities
- Files: `src/test/java/org/saltations/ViracochaCommandTest.java` (lines 17-18)
- Impact: Fragile test that breaks if output format changes, doesn't verify actual logging, requires System stream manipulation
- Risk: Side effects from System stream redirection can affect other tests if not cleaned up properly
- Fix approach: Mock logger or capture log output using Logback testing capabilities

## Code Quality Issues

**Missing Access Modifiers:**
- Issue: Class fields lack explicit access modifiers (package-private by default)
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (line 16: `boolean verbose;`)
- Impact: Field is accessible from other classes in the same package, violates encapsulation principle
- Convention: Should explicitly declare `private boolean verbose;`
- Note: Likely unintended due to template generation

**Incomplete Command Metadata:**
- Issue: `@Command` annotation has placeholder descriptions (`description = "..."`)
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (line 11)
- Impact: Help text is incomplete/unhelpful to users
- Also: `@Option` descriptions are placeholders (line 15)

**Bare Throws Declaration:**
- Issue: `main()` method declares `throws Exception` broadly
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (line 18)
- Impact: Suppresses all exceptions without handling, makes error cases opaque
- Better approach: Handle specific exceptions or let PicocliRunner handle them

## Version & Dependency Considerations

**Java 21 Target:**
- Version: Project targets Java 21 explicitly (jdk.version, release.version, compilation target)
- Context: Java 21 is an LTS release (Sept 2023), but projects may need to support earlier JDKs
- Risk: No backward compatibility for users on Java 17, 11, or earlier
- Consideration: Ensure deployment environment has Java 21 available

**GraalVM Native Image Setup:**
- Status: Project is configured for GraalVM native image compilation (annotation processors for Micronaut Graal included)
- Files: `pom.xml` - micronaut-graal annotation processor
- Risk: GraalVM native image compilation has many pitfalls (reflection, serialization); no evidence of testing native image builds
- Recommendation: Add CI step to validate native image compilation if distributing as native binary

**SnakeYAML Without Explicit Version:**
- Issue: SnakeYAML dependency (`org.yaml:snakeyaml`) has no explicit version in pom.xml
- Files: `pom.xml` (dependency declared without version)
- Impact: Version is inherited from Micronaut parent POM; version mismatch with actual SnakeYAML releases possible
- Security note: SnakeYAML has had CVEs (e.g., unsafe YAML deserialization); explicit version pinning improves security audit trail
- Recommendation: Add explicit `<version>2.0+</version>` or later to lock version and prevent transitive downgrades

## Missing Configuration

**No Application Properties Beyond Basics:**
- Files: `src/main/resources/application.yml` (minimal - only application name)
- Gap: No configuration for logging levels, profiles, or any runtime overrides
- Risk: Cannot tune behavior without code changes or command-line overrides

**Logback Configuration Lacks Production Readiness:**
- Files: `src/main/resources/logback.xml`
- Issues:
  - No file appender (only console appender)
  - No log rotation configured
  - Colored output in pattern (magenta, cyan, highlight) may be problematic in automated environments
  - No error appender for stderr
- Recommendation: Add file appender with daily rollover for production use

## Documentation Gaps

**No Implementation Guidance in Code:**
- Issue: The placeholder comment "// business logic here" is the only guidance in the main command
- Files: `src/main/java/org/saltations/ViracochaCommand.java` (line 23)
- Impact: New contributors have no context for where to add functionality
- Recommendation: Add JavaDoc to ViracochaCommand class explaining its purpose and extension points

**Incomplete README:**
- Files: `README.md` is auto-generated documentation links to Micronaut and plugin docs
- Gap: No actual project description, build instructions, usage examples, or contribution guidelines

## Fragile Areas

**Single Command Entry Point:**
- Component: `ViracochaCommand` class
- Files: `src/main/java/org/saltations/ViracochaCommand.java`
- Why fragile: All CLI functionality funnels through a single class; no subcommand structure or layered architecture
- Test coverage: Only 1 test case exercising verbose flag
- Safe modification: Create a proper command hierarchy using PicoCLI subcommands before adding complex features

**System.out Dependency in Tests:**
- Component: Test verification strategy
- Files: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Why fragile: Test passes/fails based on exact output string matching; any output formatting change breaks tests
- Alternative approach: Use Logback appender testing or dependency injection to verify behavior without stream redirection

## Scaling Limits

**Not Applicable - Prototype Stage:**
- Current codebase is minimal (28 lines main + 28 lines test)
- No data persistence, network I/O, or resource-intensive operations
- Scaling concerns will emerge when real business logic is added

## Missing Critical Features

**No Error Handling:**
- Issue: Application has no structured error handling
- Impact: Any exception propagates uncaught; users see stack traces instead of helpful error messages
- Blocks: Cannot build robust CLI that gracefully handles invalid input

**No Configuration Management:**
- Issue: No support for reading configuration from files, environment variables, or configuration servers
- Blocks: Cannot customize behavior per deployment environment

**No Logging Output:**
- Issue: Application uses System.out instead of SLF4J/Logback
- Blocks: Cannot monitor, debug, or control logging levels in production

---

*Concerns audit: 2026-03-27*
