# Codebase Structure

**Analysis Date:** 2026-03-27

## Directory Layout

```
viracocha/
├── .github/
│   └── workflows/              # CI/CD pipeline definitions
├── .mvn/                       # Maven wrapper configuration
├── .planning/                  # GSD documentation (generated)
│   └── codebase/              # Architecture and codebase analysis documents
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/saltations/  # Primary source code
│   │   └── resources/           # Configuration and runtime resources
│   └── test/
│       └── java/
│           └── org/saltations/  # Test source code
├── target/                     # Maven build output (compiled classes, JARs)
├── pom.xml                     # Maven project configuration
├── micronaut-cli.yml           # Micronaut CLI configuration
├── mvnw                        # Maven wrapper executable (Unix)
├── mvnw.bat                    # Maven wrapper executable (Windows)
└── README.md                   # Project documentation
```

## Directory Purposes

**`src/main/java/org/saltations/`:**
- Purpose: Application source code
- Contains: Command implementation classes with PicoCLI annotations
- Key files: `ViracochaCommand.java` (main entry point)

**`src/main/resources/`:**
- Purpose: Application configuration and logging setup
- Contains: YAML configuration files and Logback XML configuration
- Key files:
  - `application.yml`: Micronaut application settings (application name)
  - `logback.xml`: Logging configuration

**`src/test/java/org/saltations/`:**
- Purpose: Test suite for command functionality
- Contains: JUnit 5 test classes using Micronaut testing framework
- Key files: `ViracochaCommandTest.java` (command output verification tests)

**`.github/workflows/`:**
- Purpose: GitHub Actions CI/CD definitions
- Contains: Maven build and test pipeline configuration
- Key files: `maven.yml` (build, test, package steps)

**`.mvn/`:**
- Purpose: Maven wrapper configuration and dependencies
- Contains: Maven wrapper distribution and settings
- Generated: Yes (manages consistent Maven version across environments)
- Committed: Yes

**`target/`:**
- Purpose: Maven build artifacts
- Contains: Compiled classes, test classes, packaged JAR, reports
- Generated: Yes (created by `mvn clean install`)
- Committed: No

## Key File Locations

**Entry Points:**
- `src/main/java/org/saltations/ViracochaCommand.java`: Main CLI command class with PicoCLI annotations; execution begins in `main(String[] args)`

**Configuration:**
- `pom.xml`: Maven build configuration, dependencies, plugins, and main class definition
- `src/main/resources/application.yml`: Application name and environment-specific Micronaut settings
- `src/main/resources/logback.xml`: Logging framework configuration

**Core Logic:**
- `src/main/java/org/saltations/ViracochaCommand.java`: Contains `@Command` annotation, `@Option` fields, and `run()` method implementation

**Testing:**
- `src/test/java/org/saltations/ViracochaCommandTest.java`: JUnit 5 tests with ApplicationContext creation and output verification

## Naming Conventions

**Files:**
- Java source files: PascalCase with `.java` extension (e.g., `ViracochaCommand.java`)
- Configuration files: lowercase with hyphens and file extension (e.g., `application.yml`, `logback.xml`)
- Test files: Test class name with `Test` suffix (e.g., `ViracochaCommandTest.java`)

**Directories:**
- Package directories: lowercase with dots (e.g., `org/saltations/`)
- Build directories: lowercase (e.g., `src`, `target`, `resources`)
- Configuration directories: lowercase with hyphens (e.g., `.github`, `.mvn`)

**Classes:**
- Command classes: PascalCase ending in `Command` (e.g., `ViracochaCommand`)
- Test classes: PascalCase ending in `Test` (e.g., `ViracochaCommandTest`)

**Package Structure:**
- Group ID: `org.saltations` (reverse domain convention)
- Artifact ID: `viracocha` (lowercase hyphenated if multi-word)

## Where to Add New Code

**New Command/Subcommand:**
- Primary code: `src/main/java/org/saltations/` - Create new class extending or implementing command pattern
- Tests: `src/test/java/org/saltations/` - Create corresponding `*Test.java` class using same testing patterns as `ViracochaCommandTest.java`
- Pattern: Use `@Command` annotation for command definition, `@Option` for flags, implement `Runnable` interface

**New Configuration:**
- Properties: `src/main/resources/application.yml` - Add under `micronaut:` root key for Micronaut-specific config
- Logging: `src/main/resources/logback.xml` - Add appenders or logger definitions for new components

**Utilities or Shared Code:**
- Location: `src/main/java/org/saltations/` - Add utility classes in same package (no separate utils package currently)
- Pattern: Use static methods for pure functions, inject dependencies via Micronaut when needed

**Build/Deployment:**
- Maven configuration: `pom.xml` - Add dependencies, plugins, or properties
- CI/CD: `.github/workflows/maven.yml` - Add build steps, test runs, or deployment stages

## Special Directories

**`target/`:**
- Purpose: Build output directory
- Generated: Yes (by Maven during build)
- Committed: No (excluded by `.gitignore`)
- Contains: Compiled classes, test classes, generated sources, JAR artifacts, test reports

**`.planning/codebase/`:**
- Purpose: GSD (Get Shit Done) framework codebase documentation
- Generated: Yes (by `/gsd:map-codebase` command)
- Committed: Yes (for use by planning/execution phases)
- Contains: ARCHITECTURE.md, STRUCTURE.md, CONVENTIONS.md, TESTING.md, STACK.md, INTEGRATIONS.md, CONCERNS.md

---

*Structure analysis: 2026-03-27*
