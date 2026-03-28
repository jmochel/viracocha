# Stack Research

**Domain:** Java CLI workspace management tool
**Existing skeleton:** Micronaut 4.10.10 + picocli on JDK 21

---

## Current State (from pom.xml)

| Component | Version | Source |
|-----------|---------|--------|
| JDK | 21 | pom.xml `<java.version>` |
| Micronaut BOM | 4.10.10 | pom.xml `micronaut.platform:micronaut-platform` |
| Maven | 3.9.13 | `.mvn/wrapper/maven-wrapper.properties` |
| picocli | BOM-managed | `micronaut-picocli` dependency |
| Logback | BOM-managed | `micronaut-logging` → `logback-classic` |
| Lombok | BOM-managed | `lombok` + annotation processor |
| SnakeYAML | transitive | via Micronaut core |

## Missing Dependencies (must add to pom.xml)

| Component | Artifact | Version | Why Needed |
|-----------|----------|---------|------------|
| Freemarker | `org.freemarker:freemarker` | `2.3.33` | Template expansion — NOT BOM-managed, must add explicitly |
| Jackson YAML | `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | BOM-managed | ObjectMapper-based config I/O; SnakeYAML alone is insufficient |
| Logstash Logback | `net.logstash.logback:logstash-logback-encoder` | `7.4` | JSONL structured log output |

## Recommended Stack

| Component | Library | Rationale |
|-----------|---------|-----------|
| DI framework | Micronaut 4.10.x | Already in skeleton; build-time DI, fast startup, picocli integration built-in |
| CLI parsing | picocli (via micronaut-picocli) | Declarative `@Command`/`@Option`, subcommand support, standard help/version |
| Template engine | Apache Freemarker 2.3.33 | Latest stable 2.3.x; supports filesystem template loading, AST introspection |
| Config serialization | jackson-dataformat-yaml | ObjectMapper API consistency; supports POJO mapping with `@JsonProperty` |
| Logging | Logback + logstash-logback-encoder | Logback for SLF4J impl; logstash encoder for JSONL appender |
| Structured logging | JSONL via logstash encoder | Machine-readable, separate appender from CLI stdout |
| XDG paths | No library — hand-rolled | `System.getenv("XDG_CONFIG_HOME")` with `~/.config` fallback; trivial to implement |
| Boilerplate reduction | Project Lombok | Already configured with correct annotation processor order |

## Integration Patterns

### picocli + Micronaut DI

```java
// Root command — PicocliRunner handles DI bootstrap
@Command(name = "vira", subcommands = {ConfigCommand.class, PublisherCommand.class})
@Singleton
public class ViracochaCommand implements Runnable {
    @Inject ConfigService configService;
}

// Entry point — already in skeleton
public static void main(String[] args) {
    PicocliRunner.run(ViracochaCommand.class, args);
}
```

Every command class that uses `@Inject` must be a `@Singleton`. Field injection works with Micronaut+picocli; constructor injection preferred for testability.

### Freemarker — Per-Pattern Template Loader

```java
// Create a new Configuration per pattern source path
Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
cfg.setDirectoryForTemplateLoading(new File(patternSourcePath));
cfg.setDefaultEncoding("UTF-8");
Template t = cfg.getTemplate("relative/path/to/template.ftl");
```

**CRITICAL**: Never share one `Configuration`/`FileTemplateLoader` across multiple pattern directories. Create per-pattern instances.

### Variable Extraction from Freemarker Templates

Phase 1: Regex scan of raw template source:
```java
Pattern VAR = Pattern.compile("\\$\\{([^}?!]+)[^}]*\\}");
```
Extracts `${varName}`, `${varName?default("")}`, etc. Sufficient for v1.

Phase 2 (if needed): AST walk via `template.getRootTreeNode()` for correctness.

### JSONL Logging (logback.xml)

```xml
<appender name="JSONL" class="ch.qos.logback.core.FileAppender">
    <file>${user.home}/.local/share/viracocha/vira.jsonl</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
<!-- Separate CLI stdout appender for user-facing output -->
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>%msg%n</pattern></encoder>
</appender>
```

CLI stdout is for user output only; JSONL log goes to a file.

### XDG Config Path

```java
public Path configPath() {
    String xdg = System.getenv("XDG_CONFIG_HOME");
    Path base = (xdg != null && !xdg.isBlank())
        ? Path.of(xdg)
        : Path.of(System.getProperty("user.home"), ".config");
    return base.resolve("viracocha").resolve("config.yaml");
}
```

### Jackson YAML ObjectMapper

```java
ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
mapper.findAndRegisterModules(); // picks up JavaTimeModule etc.
ViracochaConfig config = mapper.readValue(configFile, ViracochaConfig.class);
mapper.writeValue(configFile, config);
```

Use a `ViracochaConfig` POJO (Lombok `@Data`) as the root model.

## What to Avoid

| Anti-Pattern | Why |
|--------------|-----|
| Using Micronaut `@Value`/`@ConfigurationProperties` for central config.yaml | That file is user data, not app config — treat it as a domain model, not framework config |
| Sharing `FileTemplateLoader` across pattern directories | Each pattern has its own root; shared loader breaks path resolution |
| Regex-only variable extraction long-term | Misses `<#list>`, `<#if>` context variables; acceptable for v1, document the limitation |
| Writing logs to stdout | Stdout is reserved for user-facing command output; logs → file |
| Caching `ViracochaConfig` across command invocations | Load fresh at command start, mutate, save — prevents stale state bugs |

## Confidence Levels

| Recommendation | Confidence | Notes |
|----------------|------------|-------|
| Micronaut 4.10.x + picocli | HIGH | Direct from pom.xml; stable |
| Freemarker 2.3.33 | HIGH | Latest 2.3.x stable; long-running branch |
| jackson-dataformat-yaml | HIGH | Standard Jackson module; stable API |
| logstash-logback-encoder 7.4 | MEDIUM | Verify against logback version compatibility |
| Regex variable extraction (v1) | MEDIUM | Works for simple templates; has known gaps |
| AST variable extraction (v2) | MEDIUM | API exists but underdocumented |

---
*Research date: 2026-03-27*
