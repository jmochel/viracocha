# Coding Conventions

**Analysis Date:** 2026-03-27

## Naming Patterns

**Files:**
- PascalCase for Java class files: `ViracochaCommand.java`, `ViracochaCommandTest.java`
- One public class per file
- Test files append `Test` suffix: `{ClassName}Test.java`

**Classes:**
- PascalCase: `ViracochaCommand`
- Descriptive names indicating functionality or command nature
- Implementation of standard Java interfaces (e.g., `Runnable`)

**Methods:**
- camelCase: `main()`, `run()`, `testWithCommandLineOption()`
- Prefix test methods with `testXxx` pattern for clarity
- Main entry point: `main(String[] args)` as standard

**Variables:**
- camelCase: `verbose`, `baos`, `ctx`, `args`
- Boolean fields use affirmative names: `verbose` (not `notVerbose`)
- Descriptive names for streams: `baos` (ByteArrayOutputStream), `ctx` (ApplicationContext)

**Constants:**
- Annotation values use descriptive strings: `description = "..."`, `names = {"-v", "--verbose"}`

## Code Style

**Formatting:**
- Maven project (no explicit formatter plugin configured in pom.xml)
- Consistent 4-space indentation observed
- Imports organized by package (Micronaut, picocli, Java standard library)

**Linting:**
- Maven Enforcer plugin configured in `pom.xml` - enforces build rules
- No dedicated linting tool (ESLint/Checkstyle) explicitly configured
- Reliance on Java compiler and Maven plugins for code quality

## Import Organization

**Order:**
1. Framework imports (Micronaut framework): `io.micronaut.*`
2. Third-party framework imports (picocli): `picocli.*`
3. Java standard library imports: `java.io.*`, `java.lang`
4. Test framework imports: `org.junit.jupiter.api.*`
5. Static imports last: `static org.junit.jupiter.api.Assertions.*`

**Path Aliases:**
- No custom path aliases used
- Fully qualified imports throughout

## Error Handling

**Patterns:**
- Throws declarations used for propagating exceptions: `public static void main(String[] args) throws Exception`
- Test methods also throw exceptions rather than catching: `public void testWithCommandLineOption() throws Exception`
- Try-with-resources pattern used for resource management: `try (ApplicationContext ctx = ApplicationContext.run(...)) { ... }`
- Picocli framework handles command-line parsing exceptions internally

## Logging

**Framework:** Logback via SLF4J (configured in `src/main/resources/logback.xml`)

**Patterns:**
- Colored console output configured with pattern: `%cyan(%d{HH:mm:ss.SSS}) %gray([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n`
- Root log level: `info`
- Direct `System.out.println()` used in simple cases: `System.out.println("Hi!")`
- No explicit SLF4J logger creation observed in main code (minimal logging needs)

## Comments

**When to Comment:**
- Inline comments for code explanation: `// business logic here`, `// viracocha`
- Minimal but present - indicates TODOs or clarifications
- Line comments preferred over block comments

**Javadoc/TSDoc:**
- Not used in current codebase
- Annotations used for declarative documentation: `@Command`, `@Option` with `description` attributes
- Picocli annotations serve as method/parameter documentation via help generation

## Function Design

**Size:** Small, focused functions
- `main()`: Single responsibility - CLI runner setup
- `run()`: Core business logic, single entry point

**Parameters:**
- Limited parameters - 2-3 maximum
- Command-line options passed via annotations rather than function parameters
- Stream redirection in tests via `System.setOut(new PrintStream(...))`

**Return Values:**
- `void` return type used (command-line oriented design)
- Tests return void and use assertions for validation

## Module Design

**Exports:**
- Single public class per module: `ViracochaCommand`
- No explicit barrel files or re-exports
- Test classes in separate `src/test/java` directory structure

**Annotations:**
- Heavy reliance on annotations for configuration:
  - `@Command`: Marks class as CLI command
  - `@Option`: Defines command-line options
  - `@Test`: Marks test methods
  - Annotation-driven architecture (Micronaut pattern)

---

*Convention analysis: 2026-03-27*
