package com.khaopun.holdshield.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Render-only "fake hands" window. While open, the local player reports the item it held before the swap as its
 * main-hand item, the raised shield as its off-hand item and the off hand as the hand that is using an item.
 * The window is only ever opened around first-person rendering code (hand animation tick, render state extraction
 * and the submit call), so gameplay code never sees the fake values.
 */
public final class ShieldVisual {
	private static boolean open;
	private static ItemStack mainHand = ItemStack.EMPTY;
	private static ItemStack offHand = ItemStack.EMPTY;

	private ShieldVisual() {
	}

	/** Opens the window if a round is active; returns whether it was opened. */
	public static boolean open() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			open = false;
			return false;
		}
		ItemStack[] hands = HoldShieldClient.swapper().visualHands(client.player);
		if (hands == null) {
			open = false;
			return false;
		}
		mainHand = hands[0];
		offHand = hands[1];
		open = true;
		return true;
	}

	public static void close() {
		open = false;
	}

	/** True when {@code entity} is the local player and the fake-hands window is open. */
	public static boolean applies(LivingEntity entity) {
		return open && entity == Minecraft.getInstance().player;
	}

	public static ItemStack mainHand() {
		return mainHand;
	}

	public static ItemStack offHand() {
		return offHand;
	}

	public static ItemStack inHand(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? mainHand : offHand;
	}
}
