# Hold Shield – listing text and release checklist

Copy the text below into the Modrinth / CurseForge project page. Everything under "Checklist" is for you, not the page.

## Summary (one line)
Hold a key to pull a shield from anywhere in your inventory into your hand and raise it; release to put everything back.

## Description
**Hold Shield** is a client-side Fabric mod. Press and hold one key (default `V`): the best shield in your inventory jumps into your **offhand** (or **main hand**, your choice) and is raised. Let go and every item returns to the slot it came from.

- Hold or **toggle** mode
- Left hand or right hand, with **Totem of Undying protection**
- Shield choice: highest / lowest durability, hotbar first, inventory first, first found
- Minimum durability and low-durability warning
- Configurable raise delay (works with Punchy!)
- Config screen with tooltips, reset button and Mod Menu support
- **Client-side only** – nothing to install on the server; works on vanilla servers

Requires Fabric Loader and Fabric API. Mod Menu is optional.

## Changelog – 1.1.0
- New: priority options Lowest Durability and First Found (the old priority setting was not actually applied; it is now)
- New: Hand option (offhand / main hand), Totem protection, Toggle mode
- New: minimum durability %, low-durability warning, action bar messages
- New: Raise Delay is a number field (0–20 ticks)
- New: grouped config screen with tooltips and Reset to Defaults; Mod Menu entry
- Existing configs keep working (`DURABILITY` is read as `HIGHEST_DURABILITY`)

## Files to upload (build with `gradlew build` in each folder)
| Minecraft | File |
|-----------|------|
| 1.21.11 | `mc-1.21.11/build/libs/holdshield-1.1.0+mc1.21.11.jar` |
| 26.2 | `mc-26.2/build/libs/holdshield-1.1.0+mc26.2.jar` |
| 26.3 | `mc-26.3/build/libs/holdshield-1.1.0+mc26.3.jar` |

Do not upload the `-sources.jar`. Loader: Fabric. Environment: client. Dependencies: Fabric API (required), Mod Menu (optional).

## Checklist before publishing
- [ ] Make the GitHub repo public (the mod metadata links to `https://github.com/phudit-gm/hold-shield`), or edit `contact` in each `fabric.mod.json`.
- [ ] Test 26.3 and 1.21.11 in game (only 26.2 was run with the new settings).
- [ ] Test main-hand mode and Toggle with Punchy! on and off.
- [ ] Add screenshots of the config screen and the raised shield.
- [ ] Create a GitHub release/tag `v1.1.0` and attach the three jars.
