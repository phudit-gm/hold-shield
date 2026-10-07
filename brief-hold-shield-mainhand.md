# Hold-to-Block Main-Hand Shield Swap Mod (Minecraft Java)

## Objective
A client-side Minecraft Java mod where holding a configurable key moves a shield from anywhere in the player's inventory into the main hand and raises it to block; releasing the key stops blocking and returns every item to the exact slot it came from. The offhand is never touched.

## Context
- Relevant files/modules: none yet — this is a new mod project.
- Current state: no existing code. The user has seen an existing client-side "elytra/chestplate swap" mod that swaps items from any inventory slot with one key, and wants the same swapping behavior applied to a shield in the main hand.

## Scope
**In scope**
- New mod project for the loader and Minecraft version given in Open questions.
- One keybind, registered in Minecraft's Controls menu, with a sensible unbound or uncommon default.
- On key press:
  - Find a shield in the player's inventory (hotbar + main inventory). If several exist, pick the one with the most remaining durability.
  - If the shield is in the hotbar: switch the selected hotbar slot to it and remember the previous selected slot.
  - If the shield is in the main inventory: swap it with the item in the currently selected hotbar slot and remember the source slot.
  - Start using the item (same as holding right-click) so the shield is raised.
- While the key is held: keep the shield raised.
- On key release: stop using the item, then reverse the swap so the original main-hand item returns to the selected hotbar slot and the shield returns to its original slot.
- Edge cases that must restore a correct inventory state or cleanly abort:
  - No shield in inventory → do nothing.
  - Shield breaks while blocking → original main-hand item still ends up back in the selected hotbar slot.
  - Player dies, changes dimension, or disconnects while the key is held.
  - A GUI screen opens while the key is held.
  - The player scrolls/changes hotbar slot while the key is held.
  - Key pressed again before the previous restore completed.
- Must work on vanilla multiplayer servers with no server-side install, using only actions a vanilla client can normally send (slot selection, inventory slot-swap clicks, use item).

**Out of scope**
- Any server-side component.
- Removing or shortening the vanilla shield-raise delay.
- Any artificial delay added by the mod — swap and raise happen as fast as the client allows.
- Offhand handling of any kind.
- Config screen / config file (one keybind only).
- Auto-blocking, attack detection, or any automation triggered without the player pressing the key.

## Constraints
- Do not modify: the offhand slot, armor slots, or any slot other than the selected hotbar slot and the shield's source slot.
- Conventions: follow the standard project template of the chosen loader; one clearly named class for the swap/restore logic, separate from keybind registration.
- Dependencies: only the loader and its standard API (e.g. Fabric API for Fabric). No other mods required.
- Inventory changes must use the game's own client→server interaction methods so the server stays in sync; never edit the client inventory locally only.
- Write pseudocode for the swap/restore state machine as a comment block before implementing it.

## Acceptance criteria
- [ ] Project builds with the loader's standard build command and produces a mod jar.
- [ ] Keybind appears in Controls and can be rebound.
- [ ] Singleplayer: shield in hotbar → hold key → shield raised in main hand; release → original slot selected again.
- [ ] Singleplayer: shield in main inventory → hold key → shield raised in main hand; release → shield back in its original inventory slot and original item back in the selected hotbar slot.
- [ ] Offhand item is unchanged after every test.
- [ ] With multiple shields, the highest-durability one is used.
- [ ] No shield in inventory → key does nothing, no error in log.
- [ ] Each edge case listed in Scope leaves no duplicated, missing, or ghost items (verified by reopening the inventory, and by relogging on a server).
- [ ] Same tests pass on a vanilla dedicated server of the same version with no mod installed on the server.

## Open questions
- Mod loader: Fabric, NeoForge, or Forge?
- Exact Minecraft version?
- Resolved: default key is `V` (rebindable in Controls).
- Resolved: if the selected hotbar slot is empty, the shield still goes there and the slot is left empty again after release.
