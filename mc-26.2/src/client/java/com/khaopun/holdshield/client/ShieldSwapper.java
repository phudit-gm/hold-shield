package com.khaopun.holdshield.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Swap / restore state machine (called once per client tick with "should the shield be up?"; in toggle mode
 * HoldShieldClient turns key presses into that state).
 *
 * The round uses one hand, chosen at the press:
 *   OFFHAND   the shield is moved into the real off hand with one SWAP click (button 40, the same as pressing F over
 *             a slot). The main hand and the hotbar selection are never touched. The click is symmetric: clicking
 *             the same slot again puts the shield back and returns whatever was in the off hand (an empty left hand
 *             stays empty).
 *   MAIN_HAND shield in the hotbar -> select that slot, select the old slot again on release.
 *             shield in the main inventory -> SWAP click with the selected hotbar slot, SWAP again on release.
 *             If the selected slot was EMPTY, the shield is pulled into it and STAYS (by design: putting it back
 *             into the emptied inventory slot was rejected by the server).
 *   Totem protection: off hand holds a Totem of Undying and the hand is OFFHAND -> raise with the main hand
 *             instead, or do not raise at all, or ignore it (config).
 *
 * Shields with less durability left than minDurabilityPercent are never picked (a worn shield already in the hand
 * is swapped for a usable one). A usable shield already in the hand used this round wins over the priority.
 *
 * phases: IDLE -> ACTIVE -> IDLE        (normal: press, hold, release)
 *         ACTIVE -> WAIT_RELEASE        (cancelled: must release the key before a new round can start)
 *         IDLE -> WAIT_RELEASE          (no usable shield / totem blocks: do nothing until the key is released)
 *
 *   ACTIVE: death -> abandon (server owns the items); dimension change / screen open / the hand no longer holds
 *           the shield (it broke, or the player scrolled in main-hand mode) -> restore, WAIT_RELEASE;
 *           key released -> restore, IDLE; else wait raiseDelayTicks, then keep the shield raised
 *   restore(): stop using the item, give the "use" key back, then undo the swap / selection change, but only if
 *              the slot still holds the shield (or is empty because it broke), so an unexpected inventory change is
 *              never scrambled
 */
public final class ShieldSwapper {
	private static final Logger LOGGER = LoggerFactory.getLogger("holdshield");

	private enum Phase { IDLE, ACTIVE, WAIT_RELEASE }

	/** Button value of a SWAP click that targets the offhand. */
	private static final int OFFHAND_BUTTON = 40;

	private Phase phase = Phase.IDLE;
	/** Hand used by the current round. */
	private HoldShieldConfig.Hand hand;
	/**
	 * OFFHAND: inventory index (0-35) the shield came from, or -1 if it was already in the off hand.
	 * MAIN_HAND: main inventory slot (9-35) swapped with the selected hotbar slot, or -1 if nothing to swap back.
	 */
	private int sourceInvSlot = -1;
	/** MAIN_HAND: hotbar slot that holds the shield while it is raised. */
	private int shieldHotbar;
	/** MAIN_HAND: hotbar slot that was selected when the key was pressed. */
	private int originalSel;
	private LocalPlayer player;
	private ClientLevel level;
	/** Ticks left to wait after the swap before raising the shield, so equip animations of other mods can finish. */
	private int useDelay;
	private boolean lowDurabilityWarned;

	/** True while a shield is held up (or about to be, during the raise delay). */
	public boolean isActive() {
		return phase == Phase.ACTIVE;
	}

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
		if (client.gui.screen() != null || !shieldStack(p).is(Items.SHIELD)) {
			restore(client, p);
			phase = keyDown ? Phase.WAIT_RELEASE : Phase.IDLE;
			return;
		}
		if (!keyDown) {
			restore(client, p);
			phase = Phase.IDLE;
			return;
		}
		warnLowDurability(p);
		if (useDelay > 0) {
			useDelay--;
			return;
		}
		// Keep the shield raised: vanilla releases the item whenever the use key is not down.
		client.options.keyUse.setDown(true);
		if (!p.isUsingItem()) {
			client.gameMode.useItem(p, interactionHand());
		}
	}

	private void begin(Minecraft client, LocalPlayer p) {
		HoldShieldConfig config = HoldShieldConfig.get();
		HoldShieldConfig.Hand h = config.hand;
		if (h == HoldShieldConfig.Hand.OFFHAND && p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			switch (config.totemProtection) {
				case USE_MAIN_HAND -> h = HoldShieldConfig.Hand.MAIN_HAND;
				case DONT_RAISE -> {
					debug("press: totem in off hand, not raising");
					message(p, "holdshield.message.totem");
					phase = Phase.WAIT_RELEASE;
					return;
				}
				case OFF -> { }
			}
		}

		boolean started = h == HoldShieldConfig.Hand.OFFHAND
				? beginOffhand(client, p, config) : beginMainHand(client, p, config);
		if (!started) {
			debug("press: no usable shield in inventory");
			message(p, "holdshield.message.no_shield");
			phase = Phase.WAIT_RELEASE;
			return;
		}

		hand = h;
		player = p;
		level = client.level;
		lowDurabilityWarned = false;
		phase = Phase.ACTIVE;
		if (useDelay == 0) {
			client.options.keyUse.setDown(true);
			client.gameMode.useItem(p, interactionHand());
		}
	}

	private boolean beginOffhand(Minecraft client, LocalPlayer p, HoldShieldConfig config) {
		int slot = -1;
		if (!isUsable(p.getOffhandItem(), config.minDurabilityPercent)) {
			slot = findBestShield(p.getInventory(), config.priority, config.minDurabilityPercent);
			if (slot < 0) {
				return false;
			}
		}
		debug("begin offhand: shieldSlot={} offhand={}", slot, p.getOffhandItem());
		sourceInvSlot = slot;
		useDelay = slot >= 0 ? config.raiseDelayTicks : 0;
		if (slot >= 0) {
			swapWithOffhand(client, p, slot);
		}
		return true;
	}

	private boolean beginMainHand(Minecraft client, LocalPlayer p, HoldShieldConfig config) {
		Inventory inv = p.getInventory();
		int sel = inv.getSelectedSlot();
		int best = isUsable(inv.getItem(sel), config.minDurabilityPercent)
				? sel : findBestShield(inv, config.priority, config.minDurabilityPercent);
		if (best < 0) {
			return false;
		}
		debug("begin main hand: selected={} shieldSlot={} (hand={}, shieldSlotItem={})",
				sel, best, inv.getItem(sel), inv.getItem(best));
		originalSel = sel;
		if (best < Inventory.getSelectionSize()) {
			shieldHotbar = best;
			sourceInvSlot = -1;
			useDelay = best != sel ? config.raiseDelayTicks : 0;
			if (best != sel) {
				inv.setSelectedSlot(best);
			}
		} else {
			boolean emptyHand = inv.getItem(sel).isEmpty();
			shieldHotbar = sel;
			useDelay = config.raiseDelayTicks;
			swapWithHotbar(client, p, best, sel);
			// Empty hand: the shield is pulled into the empty hotbar slot and stays there. Putting it back into
			// the (now empty) inventory slot was rejected by the server, so nothing is restored in this case.
			sourceInvSlot = emptyHand ? -1 : best;
		}
		return true;
	}

	/**
	 * Slot (0-35) of the usable shield the configured priority prefers, or -1. Slots are scanned hotbar (0-8) first,
	 * then the main inventory, and only a strictly better shield replaces the current best, so ties go to the lowest
	 * index.
	 */
	private static int findBestShield(Inventory inv, HoldShieldConfig.Priority priority, int minDurabilityPercent) {
		int best = -1;
		int bestDurability = 0;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = inv.getItem(slot);
			if (!isUsable(stack, minDurabilityPercent)) {
				continue;
			}
			int durability = remaining(stack);
			if (best == -1) {
				best = slot;
				bestDurability = durability;
				if (priority == HoldShieldConfig.Priority.FIRST_FOUND) {
					break;
				}
				continue;
			}
			boolean better = switch (priority) {
				case HIGHEST_DURABILITY -> durability > bestDurability;
				case LOWEST_DURABILITY -> durability < bestDurability;
				case HOTBAR_FIRST -> (slot < 9) != (best < 9) ? slot < 9 : durability > bestDurability;
				case INVENTORY_FIRST -> (slot < 9) != (best < 9) ? slot >= 9 : durability > bestDurability;
				case FIRST_FOUND -> false;
			};
			if (better) {
				best = slot;
				bestDurability = durability;
			}
		}
		return best;
	}

	/** A shield with at least minDurabilityPercent of its durability left (0 = any shield). */
	private static boolean isUsable(ItemStack stack, int minDurabilityPercent) {
		if (!stack.is(Items.SHIELD)) {
			return false;
		}
		return minDurabilityPercent <= 0 || remaining(stack) * 100 >= minDurabilityPercent * stack.getMaxDamage();
	}

	private static int remaining(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamageValue();
	}

	/** The stack in the hand this round raises the shield with (OFFHAND: off hand; MAIN_HAND: the shield's slot). */
	private ItemStack shieldStack(LocalPlayer p) {
		if (hand == HoldShieldConfig.Hand.OFFHAND) {
			return p.getOffhandItem();
		}
		Inventory inv = p.getInventory();
		return inv.getSelectedSlot() == shieldHotbar ? inv.getItem(shieldHotbar) : ItemStack.EMPTY;
	}

	private InteractionHand interactionHand() {
		return hand == HoldShieldConfig.Hand.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	/** Once per round: action bar warning when the raised shield is at or below the configured percentage. */
	private void warnLowDurability(LocalPlayer p) {
		int percent = HoldShieldConfig.get().lowDurabilityWarningPercent;
		if (lowDurabilityWarned || percent <= 0) {
			return;
		}
		ItemStack stack = shieldStack(p);
		if (stack.getMaxDamage() > 0 && remaining(stack) * 100 <= percent * stack.getMaxDamage()) {
			lowDurabilityWarned = true;
			actionBar(p, Component.translatable("holdshield.message.low_durability",
					remaining(stack), stack.getMaxDamage()).withStyle(ChatFormatting.YELLOW));
		}
	}

	private void restore(Minecraft client, LocalPlayer p) {
		if (p.isUsingItem()) {
			client.gameMode.releaseUsingItem(p);
		}
		client.options.keyUse.setDown(false);
		KeyMapping.setAll();

		Inventory inv = p.getInventory();
		if (hand == HoldShieldConfig.Hand.OFFHAND) {
			debug("restore offhand: source={} offhand={} sourceItem={}", sourceInvSlot, p.getOffhandItem(),
					sourceInvSlot >= 0 ? inv.getItem(sourceInvSlot) : "-");
			if (sourceInvSlot >= 0) {
				ItemStack off = p.getOffhandItem();
				if (off.is(Items.SHIELD) || off.isEmpty()) {
					swapWithOffhand(client, p, sourceInvSlot);
				} else {
					LOGGER.warn("Not restoring: offhand no longer holds the shield");
				}
			}
		} else {
			debug("restore main hand: source={} hotbar={} originalSel={} selected={} hotbarItem={} sourceItem={}",
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
				// If the player scrolled away, their new selection is kept.
				inv.setSelectedSlot(originalSel);
			}
		}
		forget();
	}

	private static void debug(String message, Object... args) {
		if (HoldShieldConfig.get().debugLogging) {
			LOGGER.info("[debug] " + message, args);
		}
	}

	private static void message(LocalPlayer p, String key) {
		if (HoldShieldConfig.get().actionBarMessages) {
			actionBar(p, Component.translatable(key));
		}
	}

	private static void actionBar(LocalPlayer p, Component text) {
		p.sendOverlayMessage(text);
	}

	/** One SWAP click (button 40) between an inventory slot (0-35) and the offhand, through the player's own menu. */
	private static void swapWithOffhand(Minecraft client, LocalPlayer p, int inventorySlot) {
		// Inventory index -> player menu slot: hotbar 0-8 is menu 36-44, main inventory 9-35 is the same number.
		int menuSlot = inventorySlot < Inventory.getSelectionSize() ? 36 + inventorySlot : inventorySlot;
		client.gameMode.handleContainerInput(
				p.inventoryMenu.containerId, menuSlot, OFFHAND_BUTTON, ContainerInput.SWAP, p);
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
