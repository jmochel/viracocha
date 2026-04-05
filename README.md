# Viracocha (`vira`)

CLI workspace manager for AI-assisted development: register **publishers** (folder sources) and **patterns** (Freemarker templates), tie them together in **projects**, then **generate** files into a project workspace. Regeneration skips files that already exist.

Requires **JDK 21**. Build with `./mvnw package` and run `java -jar target/viracocha-0.1.jar`, or use `scripts/vira` from the repo root.

---

## Conceptual model


| Concept            | Role                                                                                                                                                                                                                                       |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Central Config** | Single YAML file listing publishers, patterns, and projects. Created by `vira config init`.                                                                                                                                                |
| **Publisher**      | Named reference to a folder containing AI configuration artifacts that can be synced to a **workspace folder**. Paths to the **Publisher** must exist on disk when registered.                                                                                                      |
| **Pattern**        | Named reference to a folder or file of **Freemarker** templates. The names of Pattern folders and files can contain `${variable}` and the file contents  can use `${variable}` placeholders.                                                                                         |
| **Project**        | A configuration stored in the **Central Config** that has the path to the **workspace folder** and a list of **mappings** that map folders/files from **Publishers** and **Patterns**.                                                                                                                 |
| **Mapping**        | “Install this **registered pattern** under this **workspace** path (relative to the project workspace root), with optional **parameters** (`key=value`) for Freemarker.” Project-level default parameters can exist; mapping params override. |
| `**generate`**     | Walks each mapping, merges parameters, expands templates, writes into the project workspace. **Existing files are not overwritten** (skip-existing).                                                                                       |

Structured logging (JSONL) goes to `~/.local/share/viracocha/vira.jsonl`; normal command output stays on stdout/stderr.

---

## Subscriptions and sync

1. **Register a publisher** (`vira publisher register`) and **create a project** (`vira project create`).
2. **Add a subscription** linking the publisher tree to a folder under the workspace, with a **direction**:
   - `publish-to-workspace` — copy publisher → workspace
   - `workspace-to-publish` — copy workspace → publisher
   - `bidirectional` — reconcile both sides (analyze then apply); conflicts abort the apply phase

   Example: `vira subscription add --project <name> --publisher <pub> --source <rel> --workspace <rel> --direction publish-to-workspace`

3. **Run sync:** `vira sync --project-name <name>` with optional:
   - `--subscription <uuid>` — limit to one subscription
   - `--dry-run` — report what would happen without copying or creating directories
   - `--verbose` — one line per file action on stdout before the summary
   - `--json` — machine-readable `SyncEngineResult` on stdout (no human summary line)

**Conflict behavior:** sync is **one-shot** (no file watcher). On **bidirectional** sync, if both sides differ for the same path, the engine reports **structured conflicts** and **does not auto-merge** — exit code **1** when any subscription has conflicts or file failures.

**Scope:** **local filesystem only** — same as the rest of vira (no network, no Git operations).

---

## Configuration file


| Item        | Location                                                                                                                 |
| ----------- | ------------------------------------------------------------------------------------------------------------------------ |
| Config file | `$XDG_CONFIG_HOME/viracocha/config.yaml` — if `XDG_CONFIG_HOME` is unset, defaults to `~/.config/viracocha/config.yaml`. |
| JSONL log   | `~/.local/share/viracocha/vira.jsonl`                                                                                    |


Almost every command requires an initialized config (`vira config init` first). Otherwise you’ll see: `Config not initialized. Run 'vira config init' first.`

---

## Usage overview

### 1. Initialize config

```bash
vira config init
vira config show
```

### 2. Register publishers and patterns

Publishers and patterns are **absolute paths** on your machine.

```bash
vira publisher register --name my-publisher --path /absolute/path/to/publisher/folder
vira publisher list
vira publisher show --name my-publisher

vira pattern register --name my-pattern --path /absolute/path/to/template-tree
vira pattern list
vira pattern show --name my-pattern
```

Use `--json` on `list` / `show` where supported for machine-readable output.

### 3. Define a project and mappings

```bash
vira project create --name my-app --path /absolute/path/to/workspace
vira project add-mapping \
  --project my-app \
  --pattern my-pattern \
  --workspace relative/target/dir \
  --param foo=bar \
  --param other=value
vira project show --name my-app
vira project list
```

### 4. Generate into the workspace

```bash
vira generate --project-name my-app
vira generate --project-name my-app --dry-run    # plan only, no writes
vira generate --project-name my-app --verbose    # one line per file action
```

Summary line always reports counts: `Generated: …, Skipped: …, Failed: …`.

---

## Command tree (quick reference)


| Command                                                                | Purpose                                                                                                              |
| ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `vira config init`                                                     | Create config dir and `config.yaml` if missing.                                                                      |
| `vira config show`                                                     | Print config path and file contents.                                                                                 |
| `vira publisher` … `register`, `list`, `show`, `unregister`            | CRUD for named publisher paths (`--name`, `--path` where needed).                                                    |
| `vira pattern` … `register`, `list`, `show`, `unregister`              | CRUD for named pattern trees; `show` lists extracted Freemarker parameters.                                          |
| `vira project` … `create`, `list`, `show`, `add-mapping`, `unregister` | Projects (`--name`, `--path`), mappings (`--project`, `--pattern`, `--workspace`, repeatable `--param key=value`). |
| `vira generate`                                                        | `--project-name` (required), optional `--dry-run`, `--verbose`.                                                      |
| `vira subscription` … `add`, `list`, `show`, `remove`                  | Link a publisher subtree to a workspace subtree with a sync direction.                                                |
| `vira sync`                                                            | `--project-name` (required); optional `--subscription`, `--dry-run`, `--verbose`, `--json`.                         |


For every subcommand, `vira <group> <command> --help` lists exact options.

---

## GraalVM native image

Prerequisites: [GraalVM for JDK 21](https://www.graalvm.org/) (or another distribution) with `native-image` available (e.g. `gu install native-image` on older bundles).

```bash
./mvnw -DskipTests -Pgraalvm-native package
./target/vira --help
```

The usual JVM workflow remains: `./mvnw -DskipTests package` and `java -jar target/viracocha-0.1.jar`, or `scripts/vira`.

---

## Development

- **Stack:** Java 21, Micronaut 4, Picocli, Freemarker, Jackson YAML, Logback.
- **Docs:** [Micronaut 4 guide](https://docs.micronaut.io/4.10.10/guide/index.html), [Maven plugin](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/).

