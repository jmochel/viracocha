# Architecture

**Analysis Date:** 2026-03-27

## Pattern Overview

**Overall:** CLI Application with Micronaut Dependency Injection

**Key Characteristics:**
- Command-line interface driven by PicoCLI framework
- Lightweight Micronaut framework for dependency injection and configuration
- Single entry point with pluggable command structure
- Minimal coupling with focus on extensibility through annotations
- Build-time optimized with Micronaut processing and GraalVM support

## Layers

**Application Entry Point:**
- Purpose: CLI command dispatcher and application bootstrap
- Location: `src/main/java/org/saltations/ViracochaCommand.java`
- Contains: Main command class implementing Runnable interface
- Depends on: Micronaut PicoCLI configuration, Java standard library
- Used by: Java runtime as entry point (defined in pom.xml as exec.mainClass)

**Configuration & Context:**
- Purpose: Application context initialization and configuration management
- Location: `src/main/resources/application.yml`
- Contains: Micronaut application configuration (application name, logging settings)
- Depends on: Micronaut core framework
- Used by: ApplicationContext during startup via PicocliRunner

**Testing Layer:**
- Purpose: Verify command execution and output behavior
- Location: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Contains: JUnit 5 test classes with Micronaut test context
- Depends on: JUnit 5 API/Engine, Micronaut testing support
- Used by: Maven build system (test phase)

## Data Flow

**Command Execution:**

1. JVM invokes `ViracochaCommand.main(String[] args)`
2. `PicocliRunner.run()` parses command-line arguments using PicoCLI
3. Micronaut ApplicationContext is created with CLI and optional TEST environments
4. PicoCLI annotations on ViracochaCommand are processed (e.g., `@Option`, `@Command`)
5. Command options (such as `-v/--verbose`) are bound to instance fields
6. `run()` method is invoked as the command implementation
7. Output is printed to stdout or stderr

**Dependency Resolution:**

- Micronaut annotation processor (`micronaut-inject-java`) runs at compile time
- Dependencies and subcommands are registered via classpath scanning and annotations
- ApplicationContext uses build-time metadata to instantiate and wire components

## Key Abstractions

**Command:**
- Purpose: Represents a CLI command with options, parameters, and runnable logic
- Examples: `src/main/java/org/saltations/ViracochaCommand.java`
- Pattern: PicoCLI `@Command` annotation with `Runnable` interface

**Option:**
- Purpose: Define command-line flags and switches
- Examples: `@Option(names = {"-v", "--verbose"})` binding to `boolean verbose` field
- Pattern: PicoCLI `@Option` annotation with field-level binding

**Application Context:**
- Purpose: Dependency injection container managing singletons and prototype beans
- Examples: Created via `ApplicationContext.run()` in tests and via `PicocliRunner` in main
- Pattern: Micronaut's lightweight DI with annotation-based configuration

## Entry Points

**Main Entry Point:**
- Location: `src/main/java/org/saltations/ViracochaCommand.java`
- Triggers: `java -jar` or executable JAR invocation
- Responsibilities:
  - Parse command-line arguments
  - Apply option values to command instance
  - Execute run() method with configured options
  - Return exit code to OS

**Test Entry Point:**
- Location: `src/test/java/org/saltations/ViracochaCommandTest.java`
- Triggers: Maven `mvn test` or IDE test runner
- Responsibilities:
  - Create Micronaut ApplicationContext in CLI and TEST environments
  - Invoke PicocliRunner with specific command arguments
  - Capture and verify stdout output

## Error Handling

**Strategy:** Console output capture and assertion

**Patterns:**
- Tests redirect System.out via `PrintStream` wrapper to capture output
- Command execution success determined by presence of expected output strings
- No exception handling visible in current implementation; failures would propagate as uncaught exceptions

## Cross-Cutting Concerns

**Logging:**
- Logback configured via `src/main/resources/logback.xml`
- Micronaut uses SLF4J API with Logback implementation
- Configuration includes appenders for console output in CLI environment

**Configuration:**
- YAML-based configuration via `application.yml` (application name, profiles)
- Environment-specific settings via Micronaut `Environment` enum (CLI, TEST)
- Annotation processor compiles configuration metadata at build time

**Testing:**
- JUnit 5 framework with Micronaut test support
- Test classes create isolated ApplicationContext per test
- Output verification via ByteArrayOutputStream capture pattern

---

*Architecture analysis: 2026-03-27*
