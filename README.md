<p align="center">
  <h1 align="center">🔍 DataLens</h1>
  <p align="center">
    <strong>Real-time data inspector for Minecraft — inspect, edit, compare and export any block, entity or item.</strong>
  </p>
  <p align="center">
    <a href="#features">Features</a> •
    <a href="#commands">Commands</a> •
    <a href="#permissions">Permissions</a> •
    <a href="#configuration">Configuration</a> •
    <a href="#api">API</a> •
    <a href="#building">Building</a> •
    <a href="#project-structure">Project Structure</a>
  </p>
</p>

---

## Overview

**DataLens** is a Paper plugin that gives server operators and developers a powerful lens into the internal data structures of Minecraft objects. Point at any block, entity or player, and instantly explore its full data tree — PDC tags, attributes, potion effects, enchantments, block states and more — through both a **paginated chest GUI** and **chat commands**.

Unlike simple NBT viewers, DataLens provides a complete **inspect → edit → validate → diff → export** pipeline with automatic rollback on failure, a built-in changelog, and a public API for third-party schema registration.

**Supported versions:** Paper 1.20.x – 1.21.x · Java 21+

---

## Features

### 🔎 Deep Inspection
- **Blocks, Entities, Items & Players** — inspect any object via raytrace targeting or by player name.
- **Full data tree extraction** — PDC keys, block states, entity attributes, potion effects, equipment, enchantments, lore, and more are captured into a hierarchical `DataNode` tree.
- **Version-adaptive** — pluggable adapter layer auto-detects the server version and loads the appropriate reader/writer (`Paper120Adapter`, `Paper121Adapter`).

### ✏️ Safe In-Place Editing
- **Set / Remove** any primitive value by dot-notation path (e.g. `pdc.myplugin:level`).
- **Type validation & coercion** — raw string inputs are validated against the target node's `DataType` before committing.
- **Atomic rollback** — a deep-copy snapshot is taken before every write; if persistence fails the tree is restored automatically.

### 📊 Diff Engine
- Compare the **live** data tree against your **working copy** to see exactly what changed — additions, removals and modifications are displayed with color-coded chat output.

### 📤 Export
- Dump the complete data tree as **JSON** (via Jackson) or **YAML** (via SnakeYAML) directly to chat.

### 🖥️ Inspector GUI
- 54-slot paginated inventory with navigation breadcrumbs.
- Click into compound/list nodes to drill down; use **Back** to navigate up.
- Color-coded type indicators, pagination controls, and one-click export.

### 📝 Changelog
- Every `SET` and `REMOVE` operation is logged to `plugins/DataLens/changelog.log` with timestamps, actor, path, and old/new values.
- Configurable — can be disabled entirely.

### 🔐 Permission System
- Granular permission nodes (`datalens.inspect`, `datalens.edit`, `datalens.admin`) centralized through `PermissionGuard`.

### 🧩 Public API
- Third-party plugins can register **schemas** for their PDC namespaces to improve labelling and validation.
- Programmatic access to `InspectorService` and `SessionService` via `DataLensPlugin.getAPI()`.

### ⚡ Performance
- Per-player sessions backed by a **Caffeine** LRU cache with configurable TTL and capacity.
- Read operations are thread-safe; writes enforce main-thread execution.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/inspect` | Inspect the block/entity you are looking at (raytrace) | `datalens.inspect` |
| `/inspect <player>` | Inspect a named online player directly | `datalens.inspect` |
| `/data set <path> <value>` | Edit a primitive value at the given path | `datalens.edit` |
| `/data remove <path>` | Delete the node at the given path | `datalens.edit` |
| `/data export [json\|yaml]` | Export the inspected data tree to chat | `datalens.inspect` |
| `/data diff` | Show differences between live data and working copy | `datalens.inspect` |

Both commands include **context-aware tab completion** — paths are autocompleted from the live data tree, and values suggest type-appropriate options.

---

## Permissions

| Node | Description | Default |
|---|---|---|
| `datalens.inspect` | Inspect blocks, entities and items | `op` |
| `datalens.edit` | Edit inspected data values | `op` |
| `datalens.admin` | Full administrative access (inherits inspect + edit) | `op` |

---

## Configuration

Configuration is stored in `plugins/DataLens/config.yml`:

```yaml
debug: false

cache:
  session-ttl-seconds: 60     # Idle timeout for player sessions
  max-sessions: 100            # Maximum concurrent inspection sessions

inspect:
  max-ray-distance: 5.0        # Raytrace distance in blocks for /inspect

changelog:
  enabled: true                # Enable/disable edit logging
  max-entries: 10000           # Maximum changelog entries
```

---

## API

Other plugins can interact with DataLens programmatically:

```java
// Obtain the API instance
DataLensAPI api = DataLensPlugin.getAPI();

// Register a schema for your PDC namespace
Schema schema = new Schema("myplugin", List.of(
    new SchemaField("level", DataType.INT, "Player level"),
    new SchemaField("guild", DataType.STRING, "Guild name")
));
api.registerSchema("myplugin", schema);

// Programmatic inspection
InspectorService inspector = api.getInspectorService();
InspectableObject obj = inspector.inspect(someEntity);

// Session management
SessionService sessions = api.getSessionService();
PlayerSession session = sessions.open(player.getUniqueId(), obj);
```

---

## Building

**Requirements:** Java 21+, Gradle 8+

```bash
# Clone the repository
git clone https://github.com/Parallax-Development/DataLens.git
cd DataLens

# Build the fat JAR (output: build/libs/DataLens-<version>.jar)
./gradlew clean build

# Run a local Paper test server
./gradlew runServer
```

The build produces a **shadow JAR** that bundles Jackson and Caffeine. Paper-provided dependencies (Paper API, SnakeYAML) are excluded.

---

## Project Structure

```
DataLens/
├── build.gradle.kts                  # Build config (Shadow, run-paper)
├── settings.gradle.kts               # Project settings
├── gradle.properties                 # Version & Gradle flags
│
└── src/
    ├── main/
    │   ├── java/dev/darkblade/datalens/
    │   │   ├── DataLensPlugin.java           # Plugin entry point & lifecycle
    │   │   │
    │   │   ├── adapter/
    │   │   │   ├── common/
    │   │   │   │   └── Adapter.java          # Version-agnostic data I/O interface
    │   │   │   └── versioned/
    │   │   │       ├── Paper120Adapter.java   # Paper 1.20.x implementation
    │   │   │       └── Paper121Adapter.java   # Paper 1.21.x implementation
    │   │   │
    │   │   ├── api/
    │   │   │   └── DataLensAPI.java          # Public API surface for third-party plugins
    │   │   │
    │   │   ├── command/
    │   │   │   ├── InspectCommand.java       # /inspect — raytrace & named targeting
    │   │   │   └── DataCommand.java          # /data — set, remove, export, diff
    │   │   │
    │   │   ├── core/
    │   │   │   ├── diff/
    │   │   │   │   └── DiffService.java      # Recursive tree comparison engine
    │   │   │   ├── edit/
    │   │   │   │   ├── EditService.java      # Safe edit pipeline with rollback
    │   │   │   │   ├── PathResolver.java     # Dot-notation path resolution
    │   │   │   │   └── PathSegment.java      # Path segment model (key / index)
    │   │   │   ├── inspect/
    │   │   │   │   └── InspectorService.java # Converts live objects to DataNode trees
    │   │   │   ├── serialize/
    │   │   │   │   └── SerializationService.java  # JSON & YAML import/export
    │   │   │   ├── session/
    │   │   │   │   ├── PlayerSession.java    # Per-player inspection state & navigation
    │   │   │   │   └── SessionService.java   # Caffeine-backed session cache
    │   │   │   └── validate/
    │   │   │       └── ValidationService.java # Type validation & coercion
    │   │   │
    │   │   ├── model/
    │   │   │   ├── DataNode.java             # Core tree node (mutable, deep-copyable)
    │   │   │   ├── DataType.java             # NBT-compatible type enum
    │   │   │   ├── InspectableObject.java    # Inspected object wrapper
    │   │   │   ├── InspectableType.java      # BLOCK / ENTITY / ITEM enum
    │   │   │   ├── diff/
    │   │   │   │   ├── DataDiff.java         # Single diff entry model
    │   │   │   │   └── DiffType.java         # ADDED / REMOVED / CHANGED
    │   │   │   ├── schema/
    │   │   │   │   ├── Schema.java           # Namespace schema definition
    │   │   │   │   └── SchemaField.java      # Individual field descriptor
    │   │   │   └── validation/
    │   │   │       └── ValidationResult.java # Validation outcome (ok / error)
    │   │   │
    │   │   ├── repository/
    │   │   │   └── ChangeLogRepository.java  # File-based edit audit log
    │   │   │
    │   │   ├── security/
    │   │   │   └── PermissionGuard.java      # Centralized permission checks
    │   │   │
    │   │   ├── service/
    │   │   │   └── DataLensServiceLocator.java # Central service registry
    │   │   │
    │   │   ├── ui/
    │   │   │   ├── chat/
    │   │   │   │   └── ChatRenderer.java     # Tree & diff rendering to chat
    │   │   │   └── gui/
    │   │   │       ├── GuiListener.java      # Inventory click event handler
    │   │   │       ├── InspectorGui.java     # 54-slot paginated chest GUI
    │   │   │       └── NodeRenderer.java     # ItemStack rendering for data nodes
    │   │   │
    │   │   └── util/
    │   │       ├── AdapterLoader.java        # Runtime version detection & adapter loading
    │   │       ├── PathCompleter.java        # Tab-completion for data paths & values
    │   │       ├── PdcUtil.java              # PDC reading utilities
    │   │       └── VersionUtil.java          # Server version parsing
    │   │
    │   └── resources/
    │       ├── plugin.yml                    # Bukkit plugin descriptor
    │       └── config.yml                    # Default configuration
    │
    └── test/
        └── java/dev/darkblade/datalens/
            ├── core/
            │   ├── diff/
            │   │   └── DiffServiceTest.java
            │   └── validate/
            │       └── ValidationServiceTest.java
            └── model/
                └── DataNodeTest.java
```

---

## License

Copyright © 2026 [Parallax Development](https://github.com/Parallax-Development). All rights reserved.
