# Hold Shield - handoff (2026-10-07)

Client-side Fabric mod: hold `V` to swap a shield into the main hand and raise it, release to restore.
Repo: https://github.com/phudit-gm/hold-shield (private). Two Gradle projects: `mc-1.21.11` (Java 21 target) and
`mc-26.3` (Java 25). Gradle must run on **JDK 25** for both (Loom needs it):
`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`.

## State of `main` (stable, tested by the user in game)
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
