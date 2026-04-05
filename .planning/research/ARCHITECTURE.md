# Architecture Research

**Domain:** Java CLI workspace management tool (Viracocha / `vira`)
**Stack:** JDK 21, Micronaut 4.10.x, picocli, Freemarker, jackson-dataformat-yaml, Logback

---

## Component Overview

Five service layers beneath the command layer:

| Component | Package | Responsibility |
|-----------|---------|----------------|
| Command Layer | `org.saltations.*Command` | Thin CLI dispatchers — parse args, call services, print output |
| Config Service | `org.saltations.config` | Read/write central YAML config; XDG path resolution |
| Catalog Service | `org.saltations.catalog` | Register/list catalogs in config |
| Pattern Service | `org.saltations.pattern` | Register/list patterns; extract Freemarker variables |
| Project Service | `org.saltations.project` | Create/list projects; add/list mappings |
| Generator Service | `org.saltations.generate` | Expand patterns into workspace; skip-existing logic |
| Model | `org.saltations.model` | POJOs for config schema — no logic |
| Infrastructure | `org.saltations.infra` | XdgPaths, FreemarkerFactory — cross-cutting utilities |

## Recommended Package Structure

```
org.saltations/
  ViracochaCommand.java              ← root @Command, entry point

  config/
    ConfigCommand.java               ← group command: vira config
    InitCommand.java                 ← vira config init
    ConfigService.java               ← @Singleton: load/save ViracochaConfig

  catalog/
    CatalogCommand.java            ← group command: vira catalog
    RegisterCatalogCommand.java    ← vira catalog register
    ListCatalogsCommand.java       ← vira catalog list
    CatalogService.java            ← @Singleton: catalog CRUD (if extracted)

  pattern/
    PatternCommand.java              ← group command: vira pattern
    RegisterPatternCommand.java      ← vira pattern register
    ListPatternsCommand.java         ← vira pattern list
    ShowPatternCommand.java          ← vira pattern show
    PatternService.java              ← @Singleton: pattern CRUD + param extraction

  project/
    ProjectCommand.java              ← group command: vira project
    CreateProjectCommand.java        ← vira project create
    ListProjectsCommand.java         ← vira project list
    AddMappingCommand.java           ← vira project add-mapping
    ProjectService.java              ← @Singleton: project/mapping CRUD

  generate/
    GenerateCommand.java             ← vira generate
    GeneratorService.java            ← @Singleton: template expansion engine

  model/
    ViracochaConfig.java             ← root config POJO
    CatalogEntry.java
    PatternEntry.java                ← includes List<String> parameters
    ProjectEntry.java
    MappingEntry.java                ← includes Map<String,String> values
    WorkspaceEntry.java

  infra/
    XdgPaths.java                    ← @Singleton: config/data path resolution
    FreemarkerFactory.java           ← creates per-pattern Configuration instances
```

## Data Flow

### `vira catalog register -name aia -path /opt/aia`

```
RegisterCatalogCommand.run()
  → ConfigService.load()           // read ViracochaConfig from YAML
  → validate: path exists on disk
  → validate: name not already registered
  → CatalogEntry{name, path}
  → config.catalogs.add(entry)
  → ConfigService.save(config)     // write back to YAML
  → print: "Catalog 'aia' registered."
```

### `vira pattern register -name svc-bp -path /opt/patterns/svc`

```
RegisterPatternCommand.run()
  → ConfigService.load()
  → validate: path exists on disk
  → PatternService.extractParameters(path)   // scan .ftl files for ${varName}
  → PatternEntry{name, path, type, parameters}
  → config.patterns.add(entry)
  → ConfigService.save(config)
  → print: "Registered pattern 'svc-bp' (parameters: serviceName, packageName)"
```

### `vira generate -project-name uber-component`

```
GenerateCommand.run()
  → ConfigService.load()
  → ProjectService.findProject(name)         // error if not found
  → resolve workspace.path (create if absent)
  → for each mapping in project.mappings:
      → PatternService.findPattern(mapping.pattern)
      → resolve merged params (project.params + mapping.values)
      → GeneratorService.generate(pattern, mapping.workspacePath, params)
          → expand workspace path (Freemarker variable substitution in path string)
          → if workspace path is folder:
              → walk pattern source tree
              → for each file:
                  → expand file path segments with params
                  → compute absolute output path (project workspace + expanded relative path)
                  → if output path exists: log "Skipped", continue
                  → else: expand file content via Freemarker, write file
          → if workspace path is file:
              → expand content, write (skip if exists)
  → print summary: "N files created, M skipped"
```

## Micronaut + PicoCLI Integration Pattern

```java
// Root command — declares all group commands as subcommands
@Command(
    name = "vira",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.class,
        CatalogCommand.class,
        PatternCommand.class,
        ProjectCommand.class,
        GenerateCommand.class
    }
)
@Singleton
public class ViracochaCommand implements Runnable { ... }

// Group command (no logic, just subcommand grouping)
@Command(
    name = "catalog",
    subcommands = {RegisterCatalogCommand.class, ListCatalogsCommand.class}
)
@Singleton
public class CatalogCommand implements Runnable { ... }

// Leaf command (has logic, injects services)
@Command(name = "register")
@Singleton
public class RegisterCatalogCommand implements Runnable {
    @Inject ConfigService configService;
    @Option(names = {"-n", "--name"}, required = true) String name;
    @Option(names = {"-p", "--path"}, required = true) String path;

    @Override
    public void run() { ... }
}
```

**Rules:**
- Every `@Command` class must be `@Singleton` for Micronaut DI to inject
- Use static `subcommands={...}` declaration — no dynamic `addSubcommand()`
- `PicocliRunner.run()` in `main()` handles both Micronaut context creation and picocli parsing

## Config Service Design

```java
@Singleton
public class ConfigService {
    @Inject XdgPaths xdgPaths;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public ViracochaConfig load() {
        Path path = xdgPaths.configFile();
        if (!Files.exists(path)) throw new ConfigNotInitializedException();
        return yaml.readValue(path.toFile(), ViracochaConfig.class);
    }

    public void save(ViracochaConfig config) {
        yaml.writeValue(xdgPaths.configFile().toFile(), config);
    }

    public void init() {
        Files.createDirectories(xdgPaths.configDir());
        if (!Files.exists(xdgPaths.configFile())) {
            save(new ViracochaConfig()); // empty config, version=1
        }
    }
}
```

**Rule:** Load fresh at command start, mutate in memory, save once. No caching.

## Freemarker Integration

### Variable Extraction (Pattern Registration)

```java
// Phase 1: regex scan (v1 implementation)
Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}?!:]+)[^}]*\\}");

public List<String> extractParameters(Path sourceDir) {
    Set<String> vars = new LinkedHashSet<>();
    // Walk all files in source dir
    Files.walk(sourceDir).filter(Files::isRegularFile).forEach(file -> {
        String content = Files.readString(file);
        // Also scan the relative path string for filename variables
        String relativePath = sourceDir.relativize(file).toString();
        extractVarsFromString(content, vars);
        extractVarsFromString(relativePath, vars);
    });
    return new ArrayList<>(vars);
}
```

### Template Expansion (Generate)

```java
// Per-pattern Freemarker Configuration
Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
cfg.setDirectoryForTemplateLoading(patternEntry.sourceDir().toFile());
cfg.setDefaultEncoding("UTF-8");

// Expand file content
Template template = cfg.getTemplate(relativeFilePath);
StringWriter writer = new StringWriter();
template.process(params, writer);
String expandedContent = writer.toString();

// Expand path segments
String expandedPath = expandPathSegments(relativeFilePath, params);
```

**Path expansion:** Treat the path string itself as a mini-template — use `String.replace("${varName}", value)` for path segments (Freemarker engine is overkill for paths).

## Build Order

Dependencies determine the correct build sequence:

```
Phase 1: Foundation
  model POJOs (ViracochaConfig, CatalogEntry, PatternEntry, ProjectEntry, MappingEntry)
  infra/XdgPaths
  config/ConfigService + InitCommand
  Add missing pom.xml deps (freemarker, jackson-dataformat-yaml, logstash-logback-encoder)

Phase 2: Catalogs + Patterns
  catalog/* (commands)
  infra/FreemarkerFactory
  pattern/* (service + commands, including param extraction)

Phase 3: Projects + Mappings
  project/* (service + commands)

Phase 4: Generation
  generate/* (GeneratorService + GenerateCommand)
  JSONL logging configuration
```

ConfigService is the **critical path blocker** — nothing else can be built until it exists and is tested.

## Error Handling Strategy

- `ConfigNotInitializedException` → "Config not initialized. Run 'vira config init' first." (exit 1)
- `CatalogNotFoundException` → "Catalog 'name' not found. Run 'vira catalog list'." (exit 1)
- `PatternNotFoundException` → same pattern
- `MissingParameterException` → "Pattern 'name' requires parameter 'varName' — not provided in mapping values or project params." (exit 2)
- All exceptions caught at command `run()` level; stack trace to log only, friendly message to stderr

---
*Research date: 2026-03-27*
