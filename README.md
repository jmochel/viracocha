# Viracocha (`vira`)

CLI workspace manager for AI-assisted development: register **sources** (local directories, optionally Freemarker template trees), register **destinations** (workspace roots), attach **mappings** from sources into a destination, then **generate** files or **sync** non-template files. **Generate** never overwrites existing files. **Sync** is local filesystem only (no network, no Git).

Requires **JDK 21**. Build with `./mvnw package`, then `java -jar target/viracocha-0.1.jar`, or run `./scripts/vira` from the repository root (the script builds the fat JAR if missing).

---

## Conceptual model

| Concept | Role |
| --- | --- |
| **Central config** | Single YAML file (`config.yaml`) listing `sources` and `destinations`. Created by `vira config init`. Must declare **`version: 3`**. |
| **Source** | Named path to a directory. If marked as templates, path segments and file contents are expanded with Freemarker using the destination’s **parameters** map. |
| **Destination** | Named workspace root path, optional default **parameters** (`key: value`), and a list of **mappings**. |
| **Mapping** | Binds a **source** into that destination: optional glob filter, recurse flag, and **`sync: true`** to include the mapping in `vira sync`. |
| **Generate** | For each mapping, walks the source tree, applies template or binary copy rules, writes under the destination root. Existing destination files are skipped. |
| **Sync** | For each mapping with `sync: true`, copies changed **non-template** source files into the destination using timestamp and content checks; reports conflicts when the destination file is newer than the source and content differs. |

Structured logging (JSONL) is written to `~/.local/share/viracocha/vira.jsonl`. Normal command output uses stdout/stderr.

---

## Configuration

| Item | Location |
| --- | --- |
| Config file | `$XDG_CONFIG_HOME/viracocha/config.yaml` when `XDG_CONFIG_HOME` is set; otherwise `~/.config/viracocha/config.yaml`. |
| JSONL log | `~/.local/share/viracocha/vira.jsonl` |

If the config file is missing, commands that need it print: `Config not initialized. Run 'vira config init' first.` If the file exists but **`version` is less than 3**, load fails with an error that tells you to remove the old file and run `vira config init` again.

---

## Typical workflow

### 1. Initialize and inspect config

```bash
vira config init
vira config show
```

### 2. Register sources

Paths may be absolute or relative; the string must not contain `..`. The path must exist and be a directory.

```bash
vira source add -n my-templates -p /absolute/or/relative/path/to/tree --templates
vira source add -n my-static -p /path/to/static-files
vira source list
vira source show my-templates
```

Short aliases: `-n` for `--name`, `-p` for `--path`. `--templates` marks the source as Freemarker-driven and can record extracted variable names in config. Use `vira source remove NAME` to unregister.

### 3. Register destinations and mappings

Destination paths may be absent on disk at registration time (but must not contain `..` in the stored string). Tilde (`~`) in stored paths is expanded when generating or syncing.

```bash
vira destination add -n my-app -p /path/to/workspace
vira destination add-mapping my-app --source my-templates --recurse
vira destination add-mapping my-app --source my-static --glob "**/*.md" --sync
vira destination show my-app
vira destination list
```

`add-mapping` takes the **destination name** as the first parameter, then `--source` (required). Options: `--glob`, `--recurse`, `--sync` (persisted on the mapping; only mappings with `sync: true` participate in `vira sync`).

List mappings: `vira destination list-mappings DEST`. Remove one by **0-based index**: `vira destination remove-mapping DEST 0`.

To set Freemarker **default parameter values** for a destination, edit `config.yaml` under that destination’s `parameters:` key. There is no CLI flag for parameters today.

### 4. Generate

```bash
vira generate --destination-name my-app
vira generate --destination-name my-app --dry-run
vira generate --destination-name my-app --verbose
```

If the destination directory does not exist, you are prompted to create it (`y`/`N`), except in `--dry-run`, which only reports `Would create: <path>`. Summary line: `Generated: …, Skipped: …, Failed: …`.

### 5. Sync

```bash
vira sync --destination-name my-app
vira sync --destination-name my-app --dry-run
vira sync --destination-name my-app --verbose
vira sync --destination-name my-app --json
```

Only mappings with **`sync: true`** are processed. **Template sources are skipped** during sync. Human-readable summary: `Copied: …, Skipped: …, Failed: …, Conflicts: …`. With **`--json`**, a single **SyncResult** JSON object is printed on stdout; exit code **1** if there are conflicts or failures. Conflicts occur when a destination file is **newer** than the corresponding source file and **content differs**.

---

## Command reference

Group aliases: `vira config` → `vira cfg`, `vira source` → `vira src`, `vira destination` → `vira dest`, `vira generate` → `vira gen`.

| Command | Purpose |
| --- | --- |
| `vira config init` | Create config directory and `config.yaml` if missing. |
| `vira config show` | Print config file path and raw YAML. |
| `vira source add` | `-n`/`--name`, `-p`/`--path` (required); optional `--templates`. |
| `vira source list` | Optional `--json` (JSONL, one object per line). |
| `vira source show NAME` | Optional `--json` (single object). |
| `vira source remove NAME` | Remove a source. |
| `vira destination add` | `-n`/`--name`, `-p`/`--path` (required). |
| `vira destination list` | Optional `--json` (JSONL). |
| `vira destination show NAME` | Optional `--json` (single object). |
| `vira destination remove NAME` | Remove a destination and its mappings. |
| `vira destination add-mapping DEST` | `--source` (required); `--glob`, `--recurse`, `--sync`. |
| `vira destination list-mappings DEST` | Optional `--json` (JSONL). |
| `vira destination remove-mapping DEST INDEX` | `INDEX` is 0-based. |
| `vira generate` | Required `--destination-name`; `--dry-run`, `--verbose`. |
| `vira sync` | Required `--destination-name`; `--dry-run`, `--verbose`, `--json`. |

Use `vira <group> <subcommand> --help` for full option lists.

---

## GraalVM native image

Use a JDK 21 distribution that includes **`native-image`** (install tooling for your vendor if needed).

```bash
./mvnw -DskipTests -Pgraalvm-native package
./target/vira --help
```

---

## Development

Stack: Java 21, Micronaut 4, Picocli, Freemarker, Jackson YAML, Logback.

References: [Micronaut 4 guide](https://docs.micronaut.io/4.10.10/guide/index.html), [Micronaut Maven plugin](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/).
