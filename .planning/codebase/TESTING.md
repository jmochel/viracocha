# Testing Patterns

**Analysis Date:** 2026-03-27

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) - via `org.junit.jupiter:junit-jupiter-engine` and `org.junit.jupiter:junit-jupiter-api`
- Micronaut Test support - `io.micronaut.test:micronaut-test-junit5`
- Config: No explicit `junit-platform.properties` or `junit.properties` configuration file

**Assertion Library:**
- JUnit 5 built-in assertions: `org.junit.jupiter.api.Assertions`
- Static import pattern: `import static org.junit.jupiter.api.Assertions.assertTrue;`

**Run Commands:**
```bash
mvn verify                 # Run all tests (standard Maven lifecycle)
mvn test                   # Run tests phase only
mvn test -Dtest=ViracochaCommandTest  # Run specific test class
```

## Test File Organization

**Location:**
- Co-located in separate source tree: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Mirrors main source structure: `src/main/java/org/saltations/ViracochaCommand.java`

**Naming:**
- `{ClassName}Test` pattern: `ViracochaCommandTest` for `ViracochaCommand`
- Suffix `-Test` instead of prefix
- Test method names: `test{ScenarioDescription}` - e.g., `testWithCommandLineOption()`

**Structure:**
```
src/
├── main/
│   ├── java/org/saltations/
│   │   └── ViracochaCommand.java
│   └── resources/
│       ├── application.yml
│       └── logback.xml
└── test/
    └── java/org/saltations/
        └── ViracochaCommandTest.java
```

## Test Structure

**Suite Organization:**
```java
public class ViracochaCommandTest {

    @Test
    public void testWithCommandLineOption() throws Exception {
        // Test implementation
    }
}
```

**Patterns:**
- Test method annotation: `@Test` from JUnit 5
- Single test class containing one test method (simple structure)
- No test fixtures or suites currently defined
- No `@BeforeEach`, `@AfterEach`, or `@BeforeAll` setup/teardown observed

## Mocking

**Framework:**
- No explicit mocking framework imported (e.g., Mockito, PowerMock)
- Manual setup/teardown of test environment

**Patterns:**
```java
// Output capture pattern (manual mocking of System.out)
ByteArrayOutputStream baos = new ByteArrayOutputStream();
System.setOut(new PrintStream(baos));

// Application context setup for testing
try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
    String[] args = new String[] { "-v" };
    PicocliRunner.run(ViracochaCommand.class, ctx, args);

    assertTrue(baos.toString().contains("Hi!"));
}
```

**What to Mock:**
- System streams (I/O) for capturing output
- ApplicationContext for dependency injection in tests
- Picocli runner for command execution

**What NOT to Mock:**
- Core business logic - test real execution paths
- Framework initialization - Micronaut/Picocli setup works as designed

## Fixtures and Factories

**Test Data:**
- Minimal fixtures currently used
- Command-line arguments passed directly: `new String[] { "-v" }`
- No separate factory classes or builders for test data

**Location:**
- No dedicated `fixtures/`, `data/`, or `testdata/` directories
- Test setup inline within test methods

## Coverage

**Requirements:** None enforced

**View Coverage:**
```bash
# Maven Surefire plugin used by default (configured in parent POM)
# No jacoco or coverage plugin explicitly configured in pom.xml
# Coverage would require adding plugin configuration
```

No code coverage metrics or goals specified in current build configuration.

## Test Types

**Unit Tests:**
- Scope: Testing command execution with CLI arguments
- Approach: Direct invocation of `ViracochaCommand` through PicocliRunner
- Example: `testWithCommandLineOption()` - verifies output contains expected string
- Environment: Uses `Environment.TEST` profile for isolated execution

**Integration Tests:**
- Scope: ApplicationContext setup and Micronaut framework integration
- Approach: Creating real ApplicationContext in test environment
- Pattern: `try-with-resources` for resource cleanup

**E2E Tests:**
- Framework: Not used
- Gap: No end-to-end test coverage across full command workflows

## Common Patterns

**Async Testing:**
Not applicable - synchronous command-line application

**Output/Stream Testing:**
```java
// Capture System.out for verification
ByteArrayOutputStream baos = new ByteArrayOutputStream();
System.setOut(new PrintStream(baos));

// Execute code that produces output
PicocliRunner.run(ViracochaCommand.class, ctx, args);

// Verify output captured
assertTrue(baos.toString().contains("Hi!"));
```

**Exception Testing:**
```java
// Tests throw exceptions rather than catching them
public void testWithCommandLineOption() throws Exception {
    // Test that may throw exceptions during execution
}
```

## Test Dependencies

**JUnit 5 Jupiter:**
- `junit-jupiter-api:5.x.x` - For `@Test` annotation and assertions
- `junit-jupiter-engine:5.x.x` - Execution engine

**Micronaut Test Support:**
- `micronaut-test-junit5:x.x.x` - Integration with JUnit 5
- Provides ApplicationContext helpers and test environments

**No Additional Mocking Libraries:**
- Mockito, PowerMock, or other mocking frameworks not currently in dependencies
- Manual mocking of streams and contexts used instead

---

*Testing analysis: 2026-03-27*
