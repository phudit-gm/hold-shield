# Hold Shield - handoff (2026-10-11)

Client-side Fabric mod: hold `V` to swap a shield into the main hand and raise it, release to restore.
Repo: https://github.com/phudit-gm/hold-shield (private). Two Gradle projects: `mc-1.21.11` (Java 21 target) and
`mc-26.3` (Java 25). Gradle must run on **JDK 25** for both (Loom needs it):
`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`.

## 2026-10-11 update (release prep, version 1.1.0)
Latest state of `main`; everything below this section is older history.
- Three Gradle projects, each its own mod jar: `mc-1.21.11` (Java 21 target), `mc-26.2`, `mc-26.3` (Java 25). Gradle runs on
  JDK 25 for all (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`). Jar name: `holdshield-1.1.0+mc<ver>.jar`.
- Config screen is a plain vanilla `Screen` (no Cloth Config). Reachable from Mod Menu (`HoldShieldModMenu`, compile-only
  dependency, `suggests` in fabric.mod.json) or the unbound "Open settings" key. Mod Menu versions: 17.0.1 (1.21.11),
  20.0.3 (26.2), 21.0.0 (26.3).
- Options (all in `HoldShieldConfig`, saved to `config/holdshield.json`): `enabled`, `activation` HOLD|TOGGLE, `priority`
  HIGHEST_DURABILITY|LOWEST_DURABILITY|HOTBAR_FIRST|INVENTORY_FIRST|FIRST_FOUND (old `DURABILITY` is read via
  `@SerializedName alternate`), `minDurabilityPercent`, `lowDurabilityWarningPercent`, `hand` OFFHAND|MAIN_HAND,
  `totemProtection` OFF|USE_MAIN_HAND|DONT_RAISE, `raiseDelayTicks` (number field, 0-20), `actionBarMessages`, `debugLogging`.
  Values are clamped in `sanitize()` when loading; the screen's number fields only accept whole numbers in range.
- `ShieldSwapper` now handles both hands: OFFHAND = one SWAP click with button 40 (as before); MAIN_HAND = the old
  hotbar-select / SWAP-with-selected-slot logic from before commit `5467d11`, with its known quirk (empty selected slot +
  shield from inventory: the shield stays in that slot). `findBestShield` finally honours `priority` (before, it was
  ignored). Toggle mode lives in `HoldShieldClient` (`toggledOn`; reset when `SWAPPER.isActive()` goes false).
- Version differences between the three source trees are only API names: 1.21.11 uses `GuiGraphics`/`render`/`setScreen`/
  `client.screen`/`ClickType`/`handleInventoryMouseClick`/`displayClientMessage(text, true)`; 26.x uses
  `GuiGraphicsExtractor`/`extractRenderState`/`setScreenAndShow`/`client.gui.screen()`/`ContainerInput`/
  `handleContainerInput`/`sendOverlayMessage`. 26.2 differs from 26.3 only by `InputConstants.Type.KEYSYM`.
- Release prep done: MIT `LICENSE` (root + each project; the template CC0 files were replaced), fabric.mod.json contact
  links to the GitHub repo, one root CI workflow (`.github/workflows/build.yml`, matrix over the three projects; the old
  per-project workflows could never run and were removed), listing text + checklist in `release/MODRINTH.md`.
- Test status: built OK on all three. Played in game ONLY on 26.2 (Survival 26.2, with Punchy 2.8e) up to the Mod Menu
  entry; the settings overhaul (main hand, toggle, totem, number fields) has NOT been confirmed in game anywhere, and
  26.3 / 1.21.11 have not been run since. Installed in Prism: `Survival 26.2` has the 1.1.0-era jar named
  `holdshield-1.0.0-MC26.2.jar`; old jar backed up in `Survival 26.2\minecraft\mods-backup`. Other instances still hold older jars.
- Before publishing: make the GitHub repo public (mod metadata links to it), test 26.3 and 1.21.11, add screenshots,
  tag `v1.1.0` and attach the jars (see `release/MODRINTH.md`). `feature/offhand-visual` is obsolete and can be deleted.

## 2026-10-09 update
- `feature/real-offhand` is merged into `main`: the shield now goes into the REAL offhand (symmetric SWAP click, an empty
  left hand is restored) and `raiseDelayTicks` (default 1) delays the raise so Punchy's equip animation does not hide it.
  This replaces the experimental `feature/offhand-visual` fake-hands approach (that branch is obsolete, can be deleted).
- New project `mc-26.2` (copy of `mc-26.3`; only differences: `InputConstants.Type.KEYSYM` instead of `KEYBOARD`,
  `minecraft ~26.2`, Fabric API `0.161.0+26.2`). Installed in the Prism instance `Survival 26.2` as
  `holdshield-1.0.0-MC26.2.jar`, used with Punchy 2.8e.
- Sections below that describe the one-hand SWAP/empty-hand behaviour and the fake-hands branch are historical.

## State of `main` before the offhand change (historical)
- Keybind `V` (rebindable), unbound "Open settings" key, `config/holdshield.json`
  (`enabled`, `priority` = DURABILITY | HOTBAR_FIRST | INVENTORY_FIRST, `debugLogging`).
- Shield in hotbar -> select that slot, restore the old selection on release.
- Shield in main inventory + non-empty selected slot -> SWAP click, SWAP click again on release.
- Shield in main inventory + EMPTY selected slot (empty hand) -> shield is pulled into that slot and STAYS
  (by design). Reason: putting it back into the emptied inventory slot was rejected (restore retries made it flicker),
  root cause never found. A retry/verify loop and a pickup-click swap were tried and removed.
- Hotbar scroll / GUI open / dimension change / shield break -> restore immediately and cancel the round.
  Death -> nothing to restore.
- Installed jars: `PvP 1.21.11` (1.21.11 build) and `Survival 26.3` (26.3 build), jar names
  `holdshield-1.0.0-MC<ver>.jar` in each instance's `mods`. Prism instances live in `C:\PrismLauncher\instances`
  (the AppData path is a junction).

## Branch `feature/offhand-visual` (experimental, 26.3 only, NOT merged)
Goal: first-person view shows the item you held before the swap in the RIGHT hand and the shield in the LEFT hand
(view only; real state unchanged; real offhand item e.g. totem stays). Must work with the Punchy! mod (2.8a, installed
in `Survival 26.3`; its jar is All Rights Reserved: do not copy its code).
- Approach: `ShieldVisual` "fake hands window" + mixins (`mc-26.3/src/client/java/.../mixin/`):
  `LivingEntityVisualMixin` (fakes getMainHandItem/getOffhandItem/getItemInHand/getUsedItemHand and, latest commit,
  isUsingItem=false/isBlocking=false/getUseItem=EMPTY/getUseItemRemainingTicks=0 for the local player only while the
  window is open), `FirstPersonHandsVisualMixin` (window around FirstPersonHandsAndItems.tick/extractRenderState),
  `GameRendererVisualMixin` (WrapOperation around submitHandsWithItems), `PunchyTickVisualMixin`
  (window around Punchy's end-of-tick lambda `lambda$init$0`, Punchy 2.8a only).
- Debug: with `debugLogging` on, each round logs `fake-hands window opens this round: {...}`.
  Confirmed in the log: all four hooks fire (incl. punchy.tick), so the hooks are not the problem.
- Findings from F2 screenshots (`Survival 26.3 TEST\minecraft\screenshots`):
  - reference (shield in the REAL offhand, sword main, idle): sword raised top-right, shield low at bottom
    (`2026-10-07_21.58.45.png`).
  - with use state faked as "using shield in off hand" (`21.58.33.png`): Punchy draws the block pose (shield big
    top-left, sword pushed down to bottom centre).
  - after hiding the use state (latest commit, `22.02.05.png`): sword now in the right hand on the right side, a dark
    shape at the bottom right. The user said it did not look like the reference ("nothing happened like in the picture").
    UNCONFIRMED whether the shield is drawn at all. Next step: ask the user what is wrong, compare with the reference
    shot, check whether Punchy hides the offhand (see `PunchyClient.shouldSuppressOffhandRender`,
    `HandEquipStateMachine.isOffhandRenderSuppressed/shouldLowerOffhand`).
- Test instance: `C:\PrismLauncher\instances\Survival 26.3 TEST` (clone of Survival 26.3 without saves; its config has
  `debugLogging: true`). The real `Survival 26.3` and `PvP 1.21.11` never received the experimental jar.
- If it cannot be made to look right, keep it as an experiment on the branch; do not merge into `main`.

## Decided / out of scope
- Attack-charge reset when swapping: the reset happens in `Player.tick` on the server too; a client-only mod cannot fix
  it. User chose to skip it (a fix would be cheating on servers).
- Mod Menu entry, toggle mode, protected hotbar slots: only suggested, not requested.
- 1.21.11 version of the visual feature: not started (no Punchy there; uses vanilla `ItemInHandRenderer`, needs its own
  mixins).
- License: README/mod.json say MIT but each project's `LICENSE` is still the template's CC0. Unresolved.

## Housekeeping
- Commit messages end with the Co-Authored-By line; user wants commits at each milestone and English docs in the repo.
- Repo identity is set locally: user.name `phudit-gm`, email `phudit.g26@gmail.com`. `gh` is logged in as the old
  username `vanitas-lina` (same account).
