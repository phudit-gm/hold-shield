# Hold Shield

A client-side Minecraft (Java Edition) Fabric mod. Hold a key and a shield from **anywhere in your inventory** is swapped into your **offhand** and raised. Release the key and every item goes back to the exact slot it came from (an empty offhand is restored as empty).

> Status: 1.1.0. Tested in game on 26.2 with the Punchy! mod; the 26.3 and 1.21.11 builds compile but have not been re-tested since the settings overhaul.

## Features

- One rebindable keybind (default: `V`), listed in *Options → Controls*.
- **Hold** the key to keep the shield up, or switch to **Toggle** mode (press to raise, press again to lower).
- Raise the shield with the **left hand (offhand)** or the **right hand (main hand)**.
- **Totem protection:** if your offhand holds a Totem of Undying, raise with the main hand instead, or don't raise at all.
- Works with the shield in the hotbar or in the main inventory; a shield already in the chosen hand is used first.
- Five shield-choice rules (highest/lowest durability, hotbar first, inventory first, first found), an optional minimum durability and a low-durability warning.
- Config screen with tooltips and *Reset to Defaults*; also reachable from [Mod Menu](https://modrinth.com/mod/modmenu) (optional).
- No server-side install needed: it only sends actions a vanilla client can send (hotbar selection, inventory slot clicks, use item), so it works on vanilla servers.
- Safe restore on edge cases: shield breaks, death, dimension change, disconnect, GUI opened, hotbar scrolled (main-hand mode).

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

The mod jar is written to `build/libs/holdshield-<version>.jar` (for example `holdshield-1.1.0+mc26.2.jar`). Requires Fabric Loader and Fabric API; Mod Menu is optional.

## Settings

Open them from Mod Menu (*Configure*), or bind the *Open Hold Shield settings* key in *Options → Controls* (unbound by default). Stored in `config/holdshield.json`.

| Option | Values | Default | Meaning |
|--------|--------|---------|---------|
| `enabled` | on / off | on | Turn the mod off without removing it. |
| `activation` | `HOLD`, `TOGGLE` | `HOLD` | Hold the key, or press once to raise and again to lower. |
| `priority` | `HIGHEST_DURABILITY`, `LOWEST_DURABILITY`, `HOTBAR_FIRST`, `INVENTORY_FIRST`, `FIRST_FOUND` | `HIGHEST_DURABILITY` | Which shield to use when you carry several. (Old configs with `DURABILITY` still load.) |
| `minDurabilityPercent` | whole number 0–100 | `0` | Never use a shield with less durability left than this (0 = any). |
| `lowDurabilityWarningPercent` | whole number 0–100 | `0` | Warn above the hotbar when the raised shield is at or below this (0 = off). |
| `hand` | `OFFHAND`, `MAIN_HAND` | `OFFHAND` | Which hand raises the shield. |
| `totemProtection` | `OFF`, `USE_MAIN_HAND`, `DONT_RAISE` | `OFF` | What to do when the offhand holds a Totem of Undying (offhand mode only). |
| `raiseDelayTicks` | whole number 0–20 | `1` | Ticks to wait after the swap before the shield is raised. Use `1` with Punchy! so its equip animation can finish; `0` is instant. |
| `actionBarMessages` | on / off | on | Show "No usable shield" / totem messages above the hotbar. |
| `debugLogging` | on / off | off | Log every swap and restore (useful for bug reports). |

Main-hand mode caveat: if the selected hotbar slot is empty and the shield comes from the inventory, the shield stays in that slot after release.

## Behaviour on hotbar change

In main-hand mode, scrolling or changing the selected hotbar slot while the shield is up restores everything immediately and cancels that round (release and press again to start a new one).

## Project brief

[`brief-hold-shield-mainhand.md`](brief-hold-shield-mainhand.md) is the original (historical) brief for the first main-hand version.

## License

MIT
