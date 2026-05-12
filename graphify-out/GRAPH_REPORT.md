# Graph Report - viracocha  (2026-05-12)

## Corpus Check
- 70 files · ~22,271 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 443 nodes · 696 edges · 32 communities (13 shown, 19 thin omitted)
- Extraction: 60% EXTRACTED · 40% INFERRED · 0% AMBIGUOUS · INFERRED: 279 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `968efd9c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 29|Community 29]]

## God Nodes (most connected - your core abstractions)
1. `DestinationServiceTest` - 18 edges
2. `SourceServiceTest` - 14 edges
3. `ConfigServiceTest` - 11 edges
4. `SourceAddCommandTest` - 11 edges
5. `Viracocha (`vira`)` - 11 edges
6. `DestinationAddCommandTest` - 10 edges
7. `DestinationShowCommandTest` - 10 edges
8. `GeneratorServiceTest` - 10 edges
9. `SourceShowCommandTest` - 10 edges
10. `DestinationService` - 9 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities (32 total, 19 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (6): DestinationListMappingsCommandTest, DestinationShowCommandTest, GenerateCommandTest, GeneratorServiceTest, DefaultSyncServiceTest, SyncCommandTest

### Community 1 - "Community 1"
Cohesion: 0.07
Nodes (7): ConfigService, ConfigServiceTest, InitCommand, ShowConfigCommand, ShowConfigCommandTest, XdgPaths, XdgPathsTest

### Community 2 - "Community 2"
Cohesion: 0.04
Nodes (11): ConfigVersionException, DestinationAddCommand, DestinationAddCommandTest, DestinationAddMappingCommand, DestinationListCommand, GenerateCommand, GeneratorService, IOException (+3 more)

### Community 3 - "Community 3"
Cohesion: 0.06
Nodes (6): SourceListCommand, SourceRemoveCommand, SourceRemoveCommandTest, SourceService, SourceServiceTest, SourceShowCommand

### Community 4 - "Community 4"
Cohesion: 0.06
Nodes (6): DestinationRemoveCommand, DestinationRemoveCommandTest, DestinationRemoveMappingCommand, DestinationService, DestinationServiceTest, DestinationShowCommand

### Community 5 - "Community 5"
Cohesion: 0.09
Nodes (26): 1. Initialize and inspect config, 1. Initialize config, 2. Register catalogs and archetypes, 2. Register sources, 3. Define a project and mappings, 3. Register destinations and mappings, 4. Generate, 4. Generate into the workspace (+18 more)

### Community 6 - "Community 6"
Cohesion: 0.1
Nodes (6): FreemarkerVariableExtractor, GlobMatcher, GlobMatcherTest, HiddenPathFilter, DefaultSyncService, SyncService

### Community 7 - "Community 7"
Cohesion: 0.11
Nodes (3): DestinationAddMappingCommandTest, DestinationListMappingsCommand, DestinationRemoveMappingCommandTest

### Community 8 - "Community 8"
Cohesion: 0.13
Nodes (14): CLI Output UX — Rules, CLI Requirements, CLI UX, code:java, code:block2 (┌───────────────────────────────────────────────────────────), code:block3 (Archetype                                       Workspace), code:block4 (Catalog (LHS)                     Project (Subscriptions)   ), Command API (PicoCLI) (+6 more)

### Community 13 - "Community 13"
Cohesion: 0.22
Nodes (8): ai-idioms repo, Central Configuration, code:block1 (/home/jmochel/ai-idioms), code:block2 (/home/jmochel/sample-archetype), code:yaml (sources:), General Rules, Sample Sources, Viracocha

## Knowledge Gaps
- **27 isolated node(s):** `ViracochaConfig`, `DestinationEntry`, `SourceEntry`, `MappingEntry`, `SyncConflictRecord` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **19 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DestinationService` connect `Community 4` to `Community 0`, `Community 7`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **What connects `ViracochaConfig`, `DestinationEntry`, `SourceEntry` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.04 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `Community 4` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._