# Hold Shield

A client-side Minecraft (Java Edition) Fabric mod. Hold a key and a shield from **anywhere in your inventory** is swapped into your **offhand** and raised. Release the key and every item goes back to the exact slot it came from (an empty offhand is restored as empty).

> Status: used in game on 26.3 and 26.2 (singleplayer) with the Punchy! mod. The 1.21.11 build has not been re-tested since the offhand change.

## Features

- One rebindable keybind (default: `V`), listed in *Options â†’ Controls*.
- Works with the shield in the hotbar or in the main inventory.
- With several shields, the one with the most remaining durability is used.
- No server-side install needed: it only sends actions a vanilla client can send (hotbar selection, inventory slot clicks, use item), so it works on vanilla servers.
- Safe restore on edge cases: shield breaks, death, dimension change, disconnect, GUI opened, hotbar scrolled, key pressed again before restore finished.

## Supported versions

| Minecraft | Loader | Java | Directory |
|-----------|--------|------|-----------|
| 1.21.11   | Fabric | 21   | [`mc-1.21.11`](mc-1.21.11) |
| 26.3      | Fabric | 25   | [`mc-26.3`](mc-26.3) |
| 26.2      | Fabric | 25   | [`mc-26.2`](mc-26.2) |

Each version is its own Gradle project, since Minecraft internals differ between them.

## Building

The Loom Gradle plugin used by the templates needs a **JDK 25** to run Gradle, even for the 1.21.11 project (its bytecode still targets Java 21).

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
cd mc-1.21.11   # or mc-26.3 / mc-26.2
.\gradlew.bat build
```

The mod jar is written to `build/libs/holdshield-<version>.jar`.

## Settings

Settings are stored in `config/holdshield.json` and can be changed in game: bind the *Open Hold Shield settings* key in *Options → Controls* (unbound by default) and press it.

| Option | Values | Meaning |
|--------|--------|---------|
| `enabled` | on / off | Turn the mod off without removing it. |
| `priority` | `DURABILITY` (default), `HOTBAR_FIRST`, `INVENTORY_FIRST` | Which shield to use when you carry several. |
| `raiseDelayTicks` | integer, default `1` | Ticks to wait after the offhand swap before the shield is raised, so the Punchy! equip animation can finish and the shield is drawn. |
| `debugLogging` | on / off | Log every swap and restore (useful for bug reports). |

## Behaviour on hotbar change

If you scroll or change the selected hotbar slot while the key is held, everything is restored immediately and that round is cancelled (the key must be released and pressed again to start a new one).

## Project brief

See [`brief-hold-shield-mainhand.md`](brief-hold-shield-mainhand.md) for the full scope and acceptance criteria.

## License

MIT
