<!-- GSD:project-start source:PROJECT.md -->
## Project

**Viracocha**

A personal CLI workspace manager (`vira`) for AI-assisted development workflows. It manages how developer workspaces are populated — by registering reusable Freemarker-templated patterns and folder sources (publishers), then generating workspace content from those registrations. Aimed at eliminating manual copy-paste when bootstrapping or updating AI assistant configuration across multiple projects.

**Core Value:** A developer can register patterns and publishers once, then generate a correctly-structured workspace with a single command — and regenerating is safe (skips existing files).

### Constraints

- **Tech Stack**: JDK 21, Micronaut (DI), picocli, Project Lombok, Apache Freemarker, jackson-dataformat-yaml, Logback — no deviations in v1
- **Config format**: YAML only (no JSON, TOML, or properties files for central config)
- **Regeneration**: Generate must never overwrite existing workspace files (skip-existing semantics)
- **Scope**: Local filesystem only — no network, no Git operations in v1
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- Java 21 - Core application language for CLI tool and business logic
- YAML - Configuration files for Micronaut application
## Runtime
- JDK 21 (Temurin distribution)
- Java 21 is configured as the release version in `pom.xml`
- Maven 3+ (via mvnw wrapper)
- Lockfile: Maven pom.xml (present at `./viracocha/pom.xml`)

## Frameworks
- Micronaut 4.10.10 - Java application framework using dependency injection and annotation processing
- Picocli - CLI command framework for building command-line applications
- Micronaut PicocliRunner - Integration between Micronaut and PicocliRunner for CLI applications
- Jackson - JSON serialization and deserialization via `micronaut-serde-jackson`
- SnakeYAML - YAML parsing for configuration files
- JUnit 5 (Jupiter) - Testing framework
- Micronaut Test JUnit5 - Integration testing support for Micronaut applications
- Maven Enforcer Plugin - Enforces Maven and Java version constraints
- Lombok - Code generation for reducing boilerplate (getters, setters, constructors)
- Micronaut Maven Plugin - Plugin for building and managing Micronaut applications
- GraalVM support - Annotation processing for native image compilation
## Key Dependencies
- Micronaut Core (io.micronaut:micronaut-inject-java) - Dependency injection and annotation processing
- Picocli (info.picocli:picocli) - CLI command parsing and execution
- Logback Classic (ch.qos.logback:logback-classic) - Logging framework at runtime
- Micronaut Serde Jackson - Serialization with Jackson integration
- Micronaut Graal - GraalVM native image compilation support
- SnakeYAML - YAML configuration parsing
## Configuration
- Application configuration via `application.yml` located at `src/main/resources/application.yml`
- Micronaut application name: `viracocha`
- Micronaut processing group: `org.saltations`
- Micronaut processing module: `viracocha`
- Maven POM configuration at `pom.xml`
- Maven Compiler configuration with annotation processing paths
- Annotation processors: Lombok, Micronaut Inject, PicocliCodegen, Micronaut Graal, Micronaut Serde Processor
## Platform Requirements
- JDK 21
- Maven 3.6.0 or higher
- Linux, macOS, or Windows with Java support
- JDK 21 runtime
- Deployed as JAR executable (`viracocha-0.1.jar`)
- Entry point class: `org.saltations.ViracochaCommand`
## Build Artifacts
- Default packaging: JAR
- GraalVM native image compilation support enabled
- Maven Shade plugin may be used for fat JAR creation (referenced in micronaut-cli.yml features)
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- PascalCase for Java class files: `ViracochaCommand.java`, `ViracochaCommandTest.java`
- One public class per file
- Test files append `Test` suffix: `{ClassName}Test.java`
- PascalCase: `ViracochaCommand`
- Descriptive names indicating functionality or command nature
- Implementation of standard Java interfaces (e.g., `Runnable`)
- camelCase: `main()`, `run()`, `testWithCommandLineOption()`
- Prefix test methods with `testXxx` pattern for clarity
- Main entry point: `main(String[] args)` as standard
- camelCase: `verbose`, `baos`, `ctx`, `args`
- Boolean fields use affirmative names: `verbose` (not `notVerbose`)
- Descriptive names for streams: `baos` (ByteArrayOutputStream), `ctx` (ApplicationContext)
- Annotation values use descriptive strings: `description = "..."`, `names = {"-v", "--verbose"}`
## Code Style
- Maven project (no explicit formatter plugin configured in pom.xml)
- Consistent 4-space indentation observed
- Imports organized by package (Micronaut, picocli, Java standard library)
- Maven Enforcer plugin configured in `pom.xml` - enforces build rules
- No dedicated linting tool (ESLint/Checkstyle) explicitly configured
- Reliance on Java compiler and Maven plugins for code quality
## Import Organization
- No custom path aliases used
- Fully qualified imports throughout
## Error Handling
- Throws declarations used for propagating exceptions: `public static void main(String[] args) throws Exception`
- Test methods also throw exceptions rather than catching: `public void testWithCommandLineOption() throws Exception`
- Try-with-resources pattern used for resource management: `try (ApplicationContext ctx = ApplicationContext.run(...)) { ... }`
- Picocli framework handles command-line parsing exceptions internally
## Logging
- Colored console output configured with pattern: `%cyan(%d{HH:mm:ss.SSS}) %gray([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n`
- Root log level: `info`
- Direct `System.out.println()` used in simple cases: `System.out.println("Hi!")`
- No explicit SLF4J logger creation observed in main code (minimal logging needs)
## Comments
- Inline comments for code explanation: `// business logic here`, `// viracocha`
- Minimal but present - indicates TODOs or clarifications
- Line comments preferred over block comments
- Not used in current codebase
- Annotations used for declarative documentation: `@Command`, `@Option` with `description` attributes
- Picocli annotations serve as method/parameter documentation via help generation
## Function Design
- `main()`: Single responsibility - CLI runner setup
- `run()`: Core business logic, single entry point
- Limited parameters - 2-3 maximum
- Command-line options passed via annotations rather than function parameters
- Stream redirection in tests via `System.setOut(new PrintStream(...))`
- `void` return type used (command-line oriented design)
- Tests return void and use assertions for validation
## Module Design
- Single public class per module: `ViracochaCommand`
- No explicit barrel files or re-exports
- Test classes in separate `src/test/java` directory structure
- Heavy reliance on annotations for configuration:
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- Command-line interface driven by PicoCLI framework
- Lightweight Micronaut framework for dependency injection and configuration
- Single entry point with pluggable command structure
- Minimal coupling with focus on extensibility through annotations
- Build-time optimized with Micronaut processing and GraalVM support
## Layers
- Purpose: CLI command dispatcher and application bootstrap
- Location: `src/main/java/org/saltations/ViracochaCommand.java`
- Contains: Main command class implementing Runnable interface
- Depends on: Micronaut PicoCLI configuration, Java standard library
- Used by: Java runtime as entry point (defined in pom.xml as exec.mainClass)
- Purpose: Application context initialization and configuration management
- Location: `src/main/resources/application.yml`
- Contains: Micronaut application configuration (application name, logging settings)
- Depends on: Micronaut core framework
- Used by: ApplicationContext during startup via PicocliRunner
- Purpose: Verify command execution and output behavior
- Location: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Contains: JUnit 5 test classes with Micronaut test context
- Depends on: JUnit 5 API/Engine, Micronaut testing support
- Used by: Maven build system (test phase)
## Data Flow
- Micronaut annotation processor (`micronaut-inject-java`) runs at compile time
- Dependencies and subcommands are registered via classpath scanning and annotations
- ApplicationContext uses build-time metadata to instantiate and wire components
## Key Abstractions
- Purpose: Represents a CLI command with options, parameters, and runnable logic
- Examples: `src/main/java/org/saltations/ViracochaCommand.java`
- Pattern: PicoCLI `@Command` annotation with `Runnable` interface
- Purpose: Define command-line flags and switches
- Examples: `@Option(names = {"-v", "--verbose"})` binding to `boolean verbose` field
- Pattern: PicoCLI `@Option` annotation with field-level binding
- Purpose: Dependency injection container managing singletons and prototype beans
- Examples: Created via `ApplicationContext.run()` in tests and via `PicocliRunner` in main
- Pattern: Micronaut's lightweight DI with annotation-based configuration
## Entry Points
- Location: `src/main/java/org/saltations/ViracochaCommand.java`
- Triggers: `java -jar` or executable JAR invocation
- Responsibilities:
- Location: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Triggers: Maven `mvn test` or IDE test runner
- Responsibilities:
## Error Handling
- Tests redirect System.out via `PrintStream` wrapper to capture output
- Command execution success determined by presence of expected output strings
- No exception handling visible in current implementation; failures would propagate as uncaught exceptions
## Cross-Cutting Concerns
- Logback configured via `src/main/resources/logback.xml`
- Micronaut uses SLF4J API with Logback implementation
- Configuration includes appenders for console output in CLI environment
- YAML-based configuration via `application.yml` (application name, profiles)
- Environment-specific settings via Micronaut `Environment` enum (CLI, TEST)
- Annotation processor compiles configuration metadata at build time
- JUnit 5 framework with Micronaut test support
- Test classes create isolated ApplicationContext per test
- Output verification via ByteArrayOutputStream capture pattern
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
