# Technology Stack

**Analysis Date:** 2026-03-27

## Languages

**Primary:**
- Java 21 - Core application language for CLI tool and business logic
- YAML - Configuration files for Micronaut application

## Runtime

**Environment:**
- JDK 21 (Temurin distribution)
- Java 21 is configured as the release version in `pom.xml`

**Package Manager:**
- Maven 3+ (via mvnw wrapper)
- Lockfile: Maven pom.xml (present at `/home/jmochel/pers/viracocha/pom.xml`)

## Frameworks

**Core:**
- Micronaut 4.10.10 - Java application framework using dependency injection and annotation processing
- Picocli - CLI command framework for building command-line applications
- Micronaut PicocliRunner - Integration between Micronaut and PicocliRunner for CLI applications

**Serialization:**
- Jackson - JSON serialization and deserialization via `micronaut-serde-jackson`
- SnakeYAML - YAML parsing for configuration files

**Testing:**
- JUnit 5 (Jupiter) - Testing framework
- Micronaut Test JUnit5 - Integration testing support for Micronaut applications

**Build/Dev:**
- Maven Enforcer Plugin - Enforces Maven and Java version constraints
- Lombok - Code generation for reducing boilerplate (getters, setters, constructors)
- Micronaut Maven Plugin - Plugin for building and managing Micronaut applications
- GraalVM support - Annotation processing for native image compilation

## Key Dependencies

**Critical:**
- Micronaut Core (io.micronaut:micronaut-inject-java) - Dependency injection and annotation processing
- Picocli (info.picocli:picocli) - CLI command parsing and execution
- Logback Classic (ch.qos.logback:logback-classic) - Logging framework at runtime

**Infrastructure:**
- Micronaut Serde Jackson - Serialization with Jackson integration
- Micronaut Graal - GraalVM native image compilation support
- SnakeYAML - YAML configuration parsing

## Configuration

**Environment:**
- Application configuration via `application.yml` located at `/home/jmochel/pers/viracocha/src/main/resources/application.yml`
- Micronaut application name: `viracocha`
- Micronaut processing group: `org.saltations`
- Micronaut processing module: `viracocha`

**Build:**
- Maven POM configuration at `/home/jmochel/pers/viracocha/pom.xml`
- Maven Compiler configuration with annotation processing paths
- Annotation processors: Lombok, Micronaut Inject, PicocliCodegen, Micronaut Graal, Micronaut Serde Processor

## Platform Requirements

**Development:**
- JDK 21
- Maven 3.6.0 or higher
- Linux, macOS, or Windows with Java support

**Production:**
- JDK 21 runtime
- Deployed as JAR executable (`viracocha-0.1.jar`)
- Entry point class: `org.saltations.ViracochaCommand`

## Build Artifacts

**Packaging:**
- Default packaging: JAR
- GraalVM native image compilation support enabled
- Maven Shade plugin may be used for fat JAR creation (referenced in micronaut-cli.yml features)

---

*Stack analysis: 2026-03-27*
