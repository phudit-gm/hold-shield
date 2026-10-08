package com.khaopun.holdshield.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Offhand swap / restore state machine (called once per client tick with the current key state).
 *
 * The shield is moved into the real OFF HAND (left hand) with one SWAP click (button 40, the same as pressing F
 * over a slot). The main hand and the hotbar selection are never touched, so the first-person view is the vanilla
 * "shield in the left hand" pose and works with mods that change hand rendering (e.g. Punchy!).
 * The click is symmetric: clicking the same slot again puts the shield back and returns whatever was in the
 * offhand (including nothing: an empty left hand stays empty).
 *
 * State kept while ACTIVE:
 *   sourceInvSlot  inventory index (0-35) the shield came from, or -1 if it was already in the offhand
 *   player, level  references used to detect death / respawn / dimension change
 *
 * phases: IDLE -> ACTIVE -> IDLE        (normal: press, hold, release)
 *         ACTIVE -> WAIT_RELEASE        (cancelled: must release the key before a new round can start)
 *         IDLE -> WAIT_RELEASE          (no shield found: do nothing until the key is released)
 *
 *   IDLE:   key down, no screen, alive -> offhand already a shield: use it
 *                                         else best shield in slots 0-35 (most durability left; ties: hotbar,
 *                                         then lowest index): SWAP it with the offhand; none -> WAIT_RELEASE
 *                                         hold the vanilla "use" key, use the OFF HAND, phase = ACTIVE
 *   ACTIVE: death -> abandon (server owns the items); dimension change / screen open / offhand no longer a
 *           shield (it broke) -> restore, WAIT_RELEASE; key released -> restore, IDLE; else keep it raised
 *   restore(): stop using the item, give the "use" key back, SWAP again if we swapped and the offhand still holds
 *              the shield (or is empty because it broke, so the old offhand item comes back)
 */
public final class ShieldSwapper {
	private static final Logger LOGGER = LoggerFactory.getLogger("holdshield");

	private enum Phase { IDLE, ACTIVE, WAIT_RELEASE }

	/** Button value of a SWAP click that targets the offhand. */
	private static final int OFFHAND_BUTTON = 40;

	private Phase phase = Phase.IDLE;
	private int sourceInvSlot = -1;
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
				if (keyDown && HoldShieldConfig.get().enabled && client.screen == null && !p.isDeadOrDying()) {
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
		if (client.screen != null || !p.getOffhandItem().is(Items.SHIELD)) {
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
			client.gameMode.useItem(p, InteractionHand.OFF_HAND);
		}
	}

	private void begin(Minecraft client, LocalPlayer p) {
		Inventory inv = p.getInventory();
		int slot = p.getOffhandItem().is(Items.SHIELD) ? -1 : findBestShield(inv);
		if (slot == -1 && !p.getOffhandItem().is(Items.SHIELD)) {
			debug("press: no shield in inventory");
			phase = Phase.WAIT_RELEASE;
			return;
		}

		debug("begin: shieldSlot={} offhand={}", slot, p.getOffhandItem());
		player = p;
		level = client.level;
		sourceInvSlot = slot;
		if (slot >= 0) {
			swapWithOffhand(client, p, slot);
		}
		phase = Phase.ACTIVE;
		client.options.keyUse.setDown(true);
		client.gameMode.useItem(p, InteractionHand.OFF_HAND);
	}

	/** Slot (0-35) of the shield with the most durability left; ties go to the hotbar, then the lowest index. */
	private static int findBestShield(Inventory inv) {
		int best = -1;
		int bestDurability = -1;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = inv.getItem(slot);
			if (!stack.is(Items.SHIELD)) {
				continue;
			}
			int durability = stack.getMaxDamage() - stack.getDamageValue();
			if (durability > bestDurability) {
				best = slot;
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

		debug("restore: source={} offhand={} sourceItem={}", sourceInvSlot, p.getOffhandItem(),
				sourceInvSlot >= 0 ? p.getInventory().getItem(sourceInvSlot) : "-");
		if (sourceInvSlot >= 0) {
			ItemStack off = p.getOffhandItem();
			if (off.is(Items.SHIELD) || off.isEmpty()) {
				swapWithOffhand(client, p, sourceInvSlot);
			} else {
				LOGGER.warn("Not restoring: offhand no longer holds the shield");
			}
		}
		forget();
	}

	private static void debug(String message, Object... args) {
		if (HoldShieldConfig.get().debugLogging) {
			LOGGER.info("[debug] " + message, args);
		}
	}

	/** One SWAP click (button 40) between an inventory slot (0-35) and the offhand, through the player's own menu. */
	private static void swapWithOffhand(Minecraft client, LocalPlayer p, int inventorySlot) {
		// Inventory index -> player menu slot: hotbar 0-8 is menu 36-44, main inventory 9-35 is the same number.
		int menuSlot = inventorySlot < Inventory.getSelectionSize() ? 36 + inventorySlot : inventorySlot;
		client.gameMode.handleInventoryMouseClick(
				p.inventoryMenu.containerId, menuSlot, OFFHAND_BUTTON, ClickType.SWAP, p);
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
