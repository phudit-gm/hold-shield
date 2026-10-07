package com.khaopun.holdshield.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Swap / restore state machine (called once per client tick with the current key state).
 *
 * State kept while ACTIVE:
 *   shieldHotbar    hotbar slot (0-8) that holds the shield while it is raised
 *   sourceInvSlot   main-inventory slot (9-35) the shield came from, or -1 if it was already in the hotbar
 *   originalSel     hotbar slot that was selected when the key was pressed
 *   player, level   references used to detect death / respawn / dimension change
 *
 * phases: IDLE -> ACTIVE -> IDLE        (normal: press, hold, release)
 *         ACTIVE -> WAIT_RELEASE        (cancelled: must release the key before a new round can start)
 *         IDLE -> WAIT_RELEASE          (no shield found: do nothing until the key is released)
 *
 * tick(key down?):
 *   no player / no game mode (disconnected)  -> forget everything, no packets, phase = IDLE
 *
 *   IDLE:
 *     if key down and no screen open and player alive:
 *       best = shield with most remaining durability in inventory slots 0-35
 *              (ties: selected slot, then hotbar, then lowest index)
 *       none -> phase = WAIT_RELEASE (log nothing, send nothing)
 *       best in hotbar     -> remember originalSel; select that hotbar slot
 *       best in main inv   -> remember originalSel + sourceInvSlot;
 *                             SWAP click: source slot <-> selected hotbar slot (one packet, offhand untouched)
 *       hold the vanilla "use" key down so vanilla does not release the item, then use the main hand item
 *       phase = ACTIVE
 *
 *   ACTIVE:
 *     player died / is a different entity (respawn) -> abandon: items are handled by the server, no restore
 *     level changed (dimension)  -> restore
 *     screen open                -> restore, WAIT_RELEASE
 *     selected hotbar slot != shieldHotbar (player scrolled) -> restore, WAIT_RELEASE
 *     main hand no longer holds a shield (it broke) -> restore, WAIT_RELEASE
 *     key released               -> restore, IDLE
 *     otherwise                  -> keep the shield raised
 *
 *   WAIT_RELEASE: if key up -> IDLE
 *
 * restore():
 *   stop using the item; give the "use" key back to the physical key state
 *   if sourceInvSlot >= 0: SWAP click again (same two slots), but only if the hotbar slot still holds a
 *                          shield or is empty, so an unexpected inventory change is never scrambled
 *   else: if the shield's hotbar slot is still selected, select originalSel again
 *         (if the player scrolled, their new selection is kept)
 *   phase bookkeeping is done by the caller
 *
 * "Key pressed again before the previous restore completed" cannot happen: restore is synchronous inside
 * one tick, and a new round only starts from IDLE after the key was seen up.
 */
public final class ShieldSwapper {
	private static final Logger LOGGER = LoggerFactory.getLogger("holdshield");

	private enum Phase { IDLE, ACTIVE, WAIT_RELEASE }

	private Phase phase = Phase.IDLE;
	private int shieldHotbar;
	private int sourceInvSlot;
	private int originalSel;
	private LocalPlayer player;
	private ClientLevel level;

	public void tick(Minecraft client, boolean keyDown) {
		LocalPlayer p = client.player;
		if (p == null || client.gameMode == null || client.level == null) {
			forget();
			return;
		}

		switch (phase) {
			case IDLE -> {
				if (keyDown && HoldShieldConfig.get().enabled && client.gui.screen() == null && !p.isDeadOrDying()) {
					begin(client, p);
				}
			}
			case ACTIVE -> tickActive(client, p, keyDown);
			case WAIT_RELEASE -> {
				if (!keyDown) {
					phase = Phase.IDLE;
				}
			}
		}
	}

	private void tickActive(Minecraft client, LocalPlayer p, boolean keyDown) {
		if (player.isDeadOrDying() || p.isDeadOrDying()) {
			// Death: the server owns the inventory now, nothing to restore.
			client.options.keyUse.setDown(false);
			KeyMapping.setAll();
			forget();
			phase = keyDown ? Phase.WAIT_RELEASE : Phase.IDLE;
			return;
		}
		if (p != player || client.level != level) {
			// Dimension change: vanilla creates a new player and level, but the inventory carries over.
			restore(client, p);
			phase = keyDown ? Phase.WAIT_RELEASE : Phase.IDLE;
			return;
		}
		Inventory inv = p.getInventory();
		if (client.gui.screen() != null
				|| inv.getSelectedSlot() != shieldHotbar
				|| !inv.getItem(shieldHotbar).is(Items.SHIELD)) {
			restore(client, p);
			phase = keyDown ? Phase.WAIT_RELEASE : Phase.IDLE;
			return;
		}
		if (!keyDown) {
			restore(client, p);
			phase = Phase.IDLE;
			return;
		}
		// Keep the shield raised: vanilla releases the item whenever the use key is not down.
		client.options.keyUse.setDown(true);
		if (!p.isUsingItem()) {
			client.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		}
	}

	private void begin(Minecraft client, LocalPlayer p) {
		Inventory inv = p.getInventory();
		int sel = inv.getSelectedSlot();
		int best = findBestShield(inv, sel, HoldShieldConfig.get().priority);
		if (best < 0) {
			debug("press: no shield in inventory");
			phase = Phase.WAIT_RELEASE;
			return;
		}

		debug("begin: selected={} shieldSlot={} (hand={}, shieldSlotItem={})",
				sel, best, inv.getItem(sel), inv.getItem(best));
		originalSel = sel;
		player = p;
		level = client.level;
		if (best < Inventory.getSelectionSize()) {
			shieldHotbar = best;
			sourceInvSlot = -1;
			if (best != sel) {
				inv.setSelectedSlot(best);
			}
		} else {
			boolean emptyHand = inv.getItem(sel).isEmpty();
			shieldHotbar = sel;
			swapWithHotbar(client, p, best, sel);
			// Empty hand: the shield is pulled into the empty hotbar slot and stays there. Putting it back into
			// the (now empty) inventory slot was rejected by the server, so nothing is restored in this case.
			sourceInvSlot = emptyHand ? -1 : best;
		}
		phase = Phase.ACTIVE;
		client.options.keyUse.setDown(true);
		client.gameMode.useItem(p, InteractionHand.MAIN_HAND);
	}

	/**
	 * Picks the shield according to the configured priority. Within the same group the most remaining durability
	 * wins; ties go to the selected slot, then hotbar, then lowest index.
	 */
	private static int findBestShield(Inventory inv, int sel, HoldShieldConfig.Priority priority) {
		int best = -1;
		int bestTier = -1;
		int bestDurability = -1;
		for (int n = -1; n < 36; n++) {
			int slot = n == -1 ? sel : n;
			if (n != -1 && slot == sel) {
				continue;
			}
			ItemStack stack = inv.getItem(slot);
			if (!stack.is(Items.SHIELD)) {
				continue;
			}
			boolean inHotbar = slot < Inventory.getSelectionSize();
			int tier = switch (priority) {
				case DURABILITY -> 0;
				case HOTBAR_FIRST -> inHotbar ? 1 : 0;
				case INVENTORY_FIRST -> inHotbar ? 0 : 1;
			};
			int durability = stack.getMaxDamage() - stack.getDamageValue();
			if (tier > bestTier || (tier == bestTier && durability > bestDurability)) {
				best = slot;
				bestTier = tier;
				bestDurability = durability;
			}
		}
		return best;
	}

	private void restore(Minecraft client, LocalPlayer p) {
		if (p.isUsingItem()) {
			client.gameMode.releaseUsingItem(p);
		}
		client.options.keyUse.setDown(false);
		KeyMapping.setAll();

		Inventory inv = p.getInventory();
		debug("restore: source={} hotbar={} originalSel={} selected={} hotbarItem={} sourceItem={}",
				sourceInvSlot, shieldHotbar, originalSel, inv.getSelectedSlot(),
				inv.getItem(shieldHotbar), sourceInvSlot >= 0 ? inv.getItem(sourceInvSlot) : "-");
		if (sourceInvSlot >= 0) {
			ItemStack inHotbar = inv.getItem(shieldHotbar);
			if (inHotbar.is(Items.SHIELD) || inHotbar.isEmpty()) {
				swapWithHotbar(client, p, sourceInvSlot, shieldHotbar);
			} else {
				LOGGER.warn("Not restoring: hotbar slot {} no longer holds the shield", shieldHotbar);
			}
		} else if (inv.getSelectedSlot() == shieldHotbar) {
			inv.setSelectedSlot(originalSel);
		}
		forget();
	}

	private static void debug(String message, Object... args) {
		if (HoldShieldConfig.get().debugLogging) {
			LOGGER.info("[debug] " + message, args);
		}
	}

	/** One SWAP click between a main-inventory slot (9-35) and a hotbar slot, sent through the player's own menu. */
	private static void swapWithHotbar(Minecraft client, LocalPlayer p, int inventorySlot, int hotbarSlot) {
		client.gameMode.handleContainerInput(
				p.inventoryMenu.containerId, inventorySlot, hotbarSlot, ContainerInput.SWAP, p);
	}

	private void forget() {
		player = null;
		level = null;
		sourceInvSlot = -1;
		if (phase == Phase.ACTIVE) {
			phase = Phase.IDLE;
		}
	}
}
